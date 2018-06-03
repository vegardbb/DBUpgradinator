package vbb.dbupgradinator;

public abstract class AbstractAggregateTransformer {
    private final String currentAppVersion;
    private final String nextAppVersion;

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

    /**
     * Both parameters are strings either returned from or going into a DB query
     * @return String - the new aggregate based on the input
     * @param val The value, which is a generic object
    */
    public abstract String transformAggregate(String val);
}
