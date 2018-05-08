package migrator;

import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Quote Jenkov.com: "Inner classes are associated with an instance of the enclosing class.
// Thus, you must first create an instance of the enclosing class to create an instance of an inner class."

@SuppressWarnings("unused")
public class Migrator {
    private static final Logger logger = LogManager.getLogger("DBUpgradinator");
    private final int port;
    private final HashMap<String, AbstractAggregateTransformer> transformers = new HashMap<>(8, (float) 0.95);

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
        // Define ClassLoader instance from anonymous class
        AggregateTransformerLoader loader = new AggregateTransformerLoader();
        // I will run forever!
        try {
            // Define ServerSocket instance
            ServerSocket server = new ServerSocket(this.port);
            //noinspection InfiniteLoopStatement
            while ( true ) {
                Socket s = server.accept(); // When a connection lands, we move on to the next line in this program
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                // Read objects
                byte[] classData = (byte[]) in.readObject();
                String[] consArgs = (String[]) in.readObject();
                String className = (String) in.readObject();
                // Probably smart to do this?
                in.close();
                Class c = loader.createClass(className, classData);
                @SuppressWarnings("unchecked") // Just for this one statement
                Constructor cons = c.getConstructor(String.class, String.class, String.class);
                AbstractAggregateTransformer tran = (AbstractAggregateTransformer) cons.newInstance( consArgs[0], consArgs[1], consArgs[2]);
                // What to do with the transformer object: Add it to the AAT list
                this.addTransformer(tran);
                // this.setLastSchemaVersion(tran.getAppVersion());
            }
        } catch (Exception e) {
            logger.error("This is error", e);
            Thread.currentThread().interrupt();
        }
    }

    public int getPort() {
        return port;
    }

    // Used by the application instance to get the persisted key in DB - always run before a query
    public String getPersistedKey(String aggregateKey, String schema) {
        return aggregateKey + ":" + schema;
    }

    // Function to run after a GET query or before a PUT query gets executed
    public void checkIfAggregateIsMigrated(StringQueryInterface db, String aggregateKey, String schema, String ag) {
        AbstractAggregateTransformer spec = this.transformers.get(schema);
        if (spec == null) { return; }
        String nextSchema = spec.getNextSchemaVersion();
        // Get the persisted object keys for 1) the desired schema and 2) the newest schema
        String key = this.getPersistedKey(aggregateKey, schema); // This is the key used by the application
        String nextKey = this.getPersistedKey(aggregateKey, nextSchema);
        // Next, run a GET query on nextKey, which returns an empty string if the key is not found in which case we use the transformer whose app-version is equal to the schema-variable
        // Callback with CompletableFuture using a Lambda Expression - need to use a thenAccept method
        CompletableFuture.supplyAsync(() -> db.query(nextKey)).thenApplyAsync((aggregate) -> {
            if (aggregate.equals("")) {
                // Migrate the aggregate having the key _key to another with the key _nextKey using spec
                String migratedAggregate = spec.transformAggregate(ag);
                db.persist(nextKey, migratedAggregate);
                return migratedAggregate; // String evaluates to true
            }
            return aggregate; // String evaluates to false
        }).thenAccept((str) -> {
            if (Boolean.parseBoolean(str)) {
                logger.info("Migrated aggregated with key " + key + " to " + nextKey);
            } else {
                logger.info("No migration was applied");
            }
        });
    }
}
