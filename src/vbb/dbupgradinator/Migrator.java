package vbb.dbupgradinator;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Migrator {
    private static final Logger logger = LogManager.getLogger("DBUpgradinator");
    private final String className;
    private final HashMap<String, AbstractAggregateTransformer> transformers = new HashMap<>(4, (float) 0.95);

    public Migrator(final String className) {
        this.className = className;
        // Sets up separate process which listens actively on the server socket
        new Thread( this::aggregateTransformerReceiver ).start();
    }

    public Migrator() {
        this.className = "UserAggregateTransformer";
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
        while ( this.transformers.size() < 1 ) {
            try {
                Path path = Paths.get( new URI("./classes/" + this.className + ".class"));
                // Use Files.exists - condition
                if (Files.exists(path)) {
                    byte[] classData = Files.readAllBytes(path);
                    Class<?> c = loader.createClass(this.className, classData);
                    Constructor cons = c.getConstructor(String.class, String.class);
                    AbstractAggregateTransformer tran = (AbstractAggregateTransformer) cons.newInstance( "x", "y" );
                    // What to do with the transformer object: Add it to the AAT list
                    this.addTransformer(tran);
                }
            } catch (Exception e) {
                logger.error("An error occurred in AggregateTransformerReceiver ", e);
            }
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
