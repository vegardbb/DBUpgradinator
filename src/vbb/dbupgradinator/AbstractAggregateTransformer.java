package vbb.dbupgradinator;

// The abstract superclass also used known in DBUpgradinator. All AggregateTransformers implemented must extend this abstract class
@SuppressWarnings("unused")
public abstract class AbstractAggregateTransformer {
    // State variables - initialised in constructor - used to identify update sequence
    private String currentAppVersion;
    private String nextAppVersion;

    public AbstractAggregateTransformer(String currentAppVersion, String nextAppVersion) {
        this.currentAppVersion = currentAppVersion;
        this.nextAppVersion = nextAppVersion;
    }

    public String getNextSchemaVersion() {
        return nextAppVersion;
    }

    public String getAppVersion() {
        return currentAppVersion;
    }

    // Use this function when implementing TransformAggregate in subclass
    public boolean checkAggregateKeyVersion(String key) {
        // Split string into a collection of substrings and check if the collection of strings is exactly of length 2
        String[] col = key.split(":");
        if (col.length != 2) { return false; }
        // Check if second string in the list is equal to this.currentAppVersion
        return (col[1].equals(this.currentAppVersion));
    }

    /**
     * Both parameters are strings either returned from or going into a DB query
     * @return String - the new aggregate based on the input
     * @param val The value, which is a generic object
    */
    public abstract String transformAggregate(String val);
}
