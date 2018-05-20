package vbb.dbupgradinator;

import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Migrator {
    private static final Logger logger = LogManager.getLogger("DBUpgradinator");
    private final int port;
    private final HashMap<String, AbstractAggregateTransformer> transformers = new HashMap<>(4, (float) 0.95);

    public Migrator(final int port) {
        this.port = port;
        // Sets up separate process which listens actively on the server socket
        new Thread( this::aggregateTransformerReceiver ).start();
    }

    private class AggregateTransformerLoader extends ClassLoader {
        AggregateTransformerLoader() { super(); }
        final Class<?> createClass(String name, byte[] b) throws ClassFormatError {
            return super.defineClass(name, b, 0, b.length);
        }
    }

    // Addition of transformer class to the HashMap
    private void addTransformer(AbstractAggregateTransformer t) { this.transformers.put(t.getAppVersion(), t); }

    // Separate process that actively listens for new classes that extend AAT
    private void aggregateTransformerReceiver() {
        // Define ClassLoader instance
        AggregateTransformerLoader loader = new AggregateTransformerLoader();
        // This try block will run forever!
        try {
            // Define ServerSocket instance
            boolean looper = true;
            ServerSocket server = new ServerSocket(this.port);
            while ( looper ) {
                Socket s = server.accept(); // When a connection lands, we move on to the next line in this program
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                // Read objects
                byte[] classData = (byte[]) in.readObject();
                String[] consArgs = (String[]) in.readObject();
                String className = (String) in.readObject();
                looper = (boolean) in.readObject();
                // Probably smart to do this?
                in.close();
                Class c = loader.createClass(className, classData);
                @SuppressWarnings("unchecked") // Just for this one statement
                Constructor cons = c.getConstructor(String.class, String.class);
                AbstractAggregateTransformer tran = (AbstractAggregateTransformer) cons.newInstance( consArgs[0], consArgs[1] );
                // What to do with the transformer object: Add it to the AAT list
                this.addTransformer(tran);
                // this.setLastSchemaVersion(tran.getAppVersion());
            }
        } catch (Exception e) {
            logger.error("This is error", e);
            Thread.currentThread().interrupt();
        }
    }

    // Used by the application instance to get the persisted key in DB - always run before a query
    private String getPersistedKey(String aggregateKey, String schema) {
        return aggregateKey + ":" + schema;
    }

    private static boolean logUpdateResult(String key, Exception ex) {
        if (ex == null) {
            logger.info("Persisted key " + key);
            return true;
        } else {
            logger.error("Error during persisting" + key + ": "+ ex.toString());
            return false;
        }
    }

    // This function both POSTs the new aggregate as well as migrates it to the next schema
    public boolean migrateAndPostAggregate(StringQueryInterface db, String aggregateKey, String schema, String ag) {
        AbstractAggregateTransformer spec = this.transformers.get(schema);
        String key = this.getPersistedKey(aggregateKey, schema); // This is the key used by the application
        if (null != spec) {
            // Do extra query here if there is a migration spec available
            String nextSchema = spec.getNextSchemaVersion();
            // Get the persisted object keys for 1) the desired schema and 2) the newest schema
            String nextKey = this.getPersistedKey(aggregateKey, nextSchema);
            // Next, run a GET query on nextKey, which returns an empty string if the key is not found in which case we use the transformer whose app-version is equal to the schema-variable
            // Callback with CompletableFuture using a Lambda Expression - need to use a thenAccept method
            String migratedAggregate = spec.transformAggregate(ag);
            CompletableFuture.supplyAsync(() -> db.persist(nextKey, migratedAggregate)).thenAcceptAsync((fail) -> {
                if (fail == null) {
                    logger.info("Migrated aggregate with key " + key + " to " + nextKey);
                } else {
                    logger.error("Error during migration from " + key + " to " + nextKey + ":\n"+ fail.toString());
                }
            });
        }
        Exception ex = db.persist(key, ag);
        return logUpdateResult(key, ex);
    }

    public boolean migrateAndPutAggregate(StringQueryInterface db, String aggregateKey, String schema, String ag) {
        String key = this.getPersistedKey(aggregateKey, schema); // This is the key used by the application
        AbstractAggregateTransformer spec = this.transformers.get(schema);
        if (null != spec) {
            String nextSchema = spec.getNextSchemaVersion();
            // Get the persisted object keys for 1) the desired schema and 2) the newest schema
            String nextKey = this.getPersistedKey(aggregateKey, nextSchema);
            // Next, run a GET query on nextKey, which returns an empty string if the key is not found in which case we use the transformer whose app-version is equal to the schema-variable
            // Callback with CompletableFuture using a Lambda Expression - need to use a thenAccept method
            CompletableFuture.supplyAsync(() -> db.persist(nextKey, spec.transformAggregate(ag))).thenAccept((fail) -> {
                if (fail == null) {
                    logger.info("Migrated aggregate with key " + key + " to " + nextKey);
                } else {
                    logger.error("Error during migration from " + key + " to " + nextKey + ":\n"+ fail.toString());
                }
            });
        }
        Exception ex = db.persist(key, ag);
        return logUpdateResult(key, ex);
    }

    // GET aggregate in old schema
    public String getAndMigrateAggregate(StringQueryInterface db, String aggregateKey, String schema) {
        String key = this.getPersistedKey(aggregateKey, schema);
        String aggregate = db.query(key); // Blocking DB op
        AbstractAggregateTransformer spec = this.transformers.get(schema);
        if (null != spec) {
            String nextSchema = spec.getNextSchemaVersion();
            // Get the persisted object keys for 1) the desired schema and 2) the newest schema
            String nextKey = this.getPersistedKey(aggregateKey, nextSchema);
            // Next, run a GET query on nextKey, which returns an empty string if the key is not found in which case we use the transformer whose app-version is equal to the schema-variable
            // Callback with CompletableFuture using a Lambda Expression - need to use a thenAccept method
            CompletableFuture.supplyAsync(() -> {
                if (!aggregate.equals("")) {
                    // Migrate the aggregate having the key _key to another with the key _nextKey using spec
                    String migratedAggregate = spec.transformAggregate(aggregate);
                    Exception fail = db.persist(nextKey, migratedAggregate);
                    if (fail == null) { return true; }
                    logger.error("Error during migration from " + key + " to " + nextKey + ":\n"+ fail.toString());
                }
                return false;
            }).thenAccept((b) -> {
                if (b) {
                    logger.info("Migrated aggregate with key " + key + " to " + nextKey);
                }
            });
        }
        return aggregate;
    }
}
