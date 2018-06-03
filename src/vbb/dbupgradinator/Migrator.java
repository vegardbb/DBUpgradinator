package vbb.dbupgradinator;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Migrator {
    // Define ClassLoader instance
    private final AggregateTransformerLoader loader = new AggregateTransformerLoader();
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final String className;
    private final HashMap<String, AbstractAggregateTransformer> transformers = new HashMap<>(4, (float) 0.95);

    public Migrator() {
        this.className = "app.UserAggregateTransformer";
        this.aggregateTransformerReceiver();
    }

    private class AggregateTransformerLoader extends ClassLoader {
        AggregateTransformerLoader() { super(); }
        final Class<?> createClass(String name, byte[] b) throws ClassFormatError {
            return super.defineClass(name, b, 0, b.length);
        }
    }

    // Addition of transformer class to the HashMap
    private void addTransformer(AbstractAggregateTransformer t) { this.transformers.put(t.getAppVersion(), t); }

    private static void log(String s) {
        try {
            Files.write(Paths.get("dbupgradinator.log").toAbsolutePath(), s.getBytes(), StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.out.println();
            e.printStackTrace();
            System.out.println();
        }
    }

    // Separate process that actively listens for new classes that extend AAT
    private void aggregateTransformerReceiver() {
        if ( this.transformers.size() < 1 ) {
            try {
                Path path = Paths.get( "classes/transformer/" + this.className.replace(".","/") + ".class").toAbsolutePath();
                if (Files.exists(path)) {
                    byte[] classData = Files.readAllBytes(path);
                    Class<?> c = loader.createClass(this.className, classData);
                    Constructor cons = c.getConstructor(String.class, String.class);
                    AbstractAggregateTransformer tran = (AbstractAggregateTransformer) cons.newInstance( "x", "y" );
                    // What to do with the transformer object: Add it to the AAT list
                    this.addTransformer(tran);
                    log("INFO @ " + this.dateFormat.format(new Date()) + " - Received aggregate transformer for schema " + tran.getAppVersion() + "\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
                log("ERROR @ " + this.dateFormat.format(new Date()) + " " + e.toString() + "\n");
            }
        }
    }

    // Used by the application instance to get the persisted key in DB - always run before a query
    private String getPersistedKey(String aggregateKey, String schema) {
        return aggregateKey + ":" + schema;
    }

    private boolean logUpdateResult(String key, Exception ex) {
        if (ex == null) {
            log("INFO @ " + this.dateFormat.format(new Date()) + " - Persisted key " + key + "\n");
            return true;
        } else {
            log("ERROR @ " + this.dateFormat.format(new Date()) + " Error during persisting " + key + ex.toString() + "\n");
            return false;
        }
    }

    // This function both POSTs the new aggregate as well as migrates it to the next schema
    public boolean migrateAndPostAggregate(StringQueryInterface db, String aggregateKey, String schema, String ag) {
        this.aggregateTransformerReceiver();
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
                    log("INFO @ " + this.dateFormat.format(new Date()) + " - Migrated aggregate with key " + key + " to " + nextKey + "\n");
                } else {
                    log("ERROR @ " + this.dateFormat.format(new Date()) + " - Error during persisting " + nextKey + "\n" + fail.toString() + "\n");
                }
            });
        }
        Exception ex = db.persist(key, ag);
        return logUpdateResult(key, ex);
    }

    public boolean migrateAndPutAggregate(StringQueryInterface db, String aggregateKey, String schema, String ag) {
        this.aggregateTransformerReceiver();
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
                    log("INFO @ " + this.dateFormat.format(new Date()) + " - Migrated aggregate with key " + key + " to " + nextKey + "\n");
                } else {
                    log("ERROR @ " + this.dateFormat.format(new Date()) + " - Error during migration from " + key + " to " + nextKey + ":\n"+ fail.toString() + "\n");
                }
            });
        }
        Exception ex = db.persist(key, ag);
        return logUpdateResult(key, ex);
    }

    // GET aggregate in old schema
    public String getAndMigrateAggregate(StringQueryInterface db, String aggregateKey, String schema) {
        this.aggregateTransformerReceiver();
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
                    log("ERROR @ " + this.dateFormat.format(new Date()) + " - Error during migration from " + key + " to " + nextKey + ":\n"+ fail.toString() + "\n");
                }
                return false;
            }).thenAccept((b) -> {
                if (b) {
                    log("INFO @ " + this.dateFormat.format(new Date()) + " - Migrated aggregate with key " + key + " to " + nextKey + "\n");
                }
            });
        }
        return aggregate;
    }
}
