package dbaclient;

import java.io.*;

// The abstract superclassclass also used known in DBUpgradinator. All AggregateTransformers implemented must extend this abstract class
abstract class AbstractAggregateTransformer implements Serializable {
    // State variables - initialised in constructor - used to identify update sequence
    private String previousAppVersion;
    private String currentAppVersion;
    private String nextAppVersion;

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

    public String getcurrentAppVersion() {
        return currentAppVersion;
    }

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

    /*
     * This is the default implementation of writeObject.
     * Customise if necessary.
    public void writeObject(
            ObjectOutputStream aOutputStream
    ) throws IOException {
        //perform the default serialization for all non-transient, non-static fields
        aOutputStream.defaultWriteObject();
    }
    private void readObject(
            ObjectInputStream aInputStream
    ) throws ClassNotFoundException, IOException {
        // always perform the default de-serialization first
        aInputStream.defaultReadObject();
        // make defensive copy of the mutable Date field
        // fDateOpened = new Date(fDateOpened.getTime());
        // ensure that object state has not been corrupted or tampered with maliciously
        // this.validateState();
    }

    private void readObjectNoData() throws InvalidObjectException {
        throw new InvalidObjectException("Stream data required");
    }
     */
}
