package migrator;

import java.io.Serializable;

// The abstract superclass also used known in DBUpgradinator. All AggregateTransformers implemented must extend this abstract class
@SuppressWarnings("unused") // Just for this one statement
public abstract class AbstractAggregateTransformer implements Serializable {
    // State variables - initialised in constructor - used to identify update sequence
    private String previousAppVersion;
    private String currentAppVersion;
    private String nextAppVersion;

    public AbstractAggregateTransformer(String[] strings) {
        this.previousAppVersion = strings[0];
        this.currentAppVersion = strings[1];
        this.nextAppVersion = strings[2];
    }

    public AbstractAggregateTransformer(String previousAppVersion, String currentAppVersion, String nextAppVersion) {
        this.previousAppVersion = previousAppVersion;
        this.currentAppVersion = currentAppVersion;
        this.nextAppVersion = nextAppVersion;
    }

    public String getnextAppVersion() {
        return nextAppVersion;
    }

    public String getpreviousAppVersion() {
        return previousAppVersion;
    }

    public String getAppVersion() {
        return currentAppVersion;
    }

    // Use this function when implementing TransformAggregate in subclass
    public Boolean checkAggregateKeyVersion(String key) {
        // Split string into a collection of substrings and check if the collection of strings is exactly of length 2
        String[] col = key.split(":");
        if (col.length != 2) { return false; }
        // Check if second string in the list is equal to this.currentAppVersion
        return (col[1].equals(this.currentAppVersion));
    }

    /**
     * Both parameters are strings either returned from or going into a DB query
     * @param key The key used to get the aggregate
     * @param val The value, which is a generic object
    */
    public abstract void TransformAggregate(String key, Object val);
}
