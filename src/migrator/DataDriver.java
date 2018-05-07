package migrator;

// DataDriver class from which separate threads are spawned for the sake of querying aggregates on the newest schema just to find out 
// This class will also migrate the aggregate if it is not represented in the newest schema, indicated by the Migrator class
// Instead of retrieving a db config we pass a query interface, in which the programmer themselves can implement a querying function in which the return type, as well as the parameter, is a String.

import java.util.concurrent.Callable;

public class DataDriver implements Callable<String> {
    private final String key;
    private final StringQueryInterface db;

    public DataDriver(String key, StringQueryInterface db) {
        this.key = key;
        this.db = db;
    }

    @Override
    public String call() {
        return db.query(key);
    }
}
