package vbb.master;

// Package descriptor will vary
// A prototype class known in Voldemort. All AggregateTransformers implemented must extend this abstract class
public abstract class AbstractAggregateTransformer {
    // State variables - initialised in constructor
    private String previousVersion;
    private String currentVersion;
    private String nextVersion;

    public AbstractAggregateTransformer(String previousVersion, String currentVersion, String nextVersion) {
        this.previousVersion = previousVersion;
        this.currentVersion = currentVersion;
        this.nextVersion = nextVersion;
    }

    public String getNextVersion() {
        return nextVersion;
    }

    public String getPreviousVersion() {
        return previousVersion;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    /*
    public void setNextVersion(String nextVersion) {
        this.nextVersion = nextVersion;
    }

    public void setPreviousVersion(String previousVersion) {
        this.previousVersion = previousVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }
    */

    // Arguments args:

}
