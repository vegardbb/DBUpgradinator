// FIXME: Edit package descriptor
package dbaclient;

import java.io.*;

// The abstract superclassclass also used known in DBUpgradinator. All AggregateTransformers implemented must extend this abstract class
public abstract class AbstractAggregateTransformer implements Serializable {
    // State variables - initialised in constructor - used to identify 
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

    private Boolean checkAggregateKeyVersion(String key) {
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


    /**
     * This is the default implementation of writeObject.
     * Customise if necessary.
     */
    private void writeObject(
            ObjectOutputStream aOutputStream
    ) throws IOException {
        //perform the default serialization for all non-transient, non-static fields
        aOutputStream.defaultWriteObject();
    }
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {}

    private void readObjectNoData() throws ObjectStreamException {}
 
    /*
    public void setnextAppVersion(String nextAppVersion) {
        this.nextAppVersion = nextAppVersion;
    }

    public void setpreviousAppVersion(String previousAppVersion) {
        this.previousAppVersion = previousAppVersion;
    }

    public void setcurrentAppVersion(String currentAppVersion) {
        this.currentAppVersion = currentAppVersion;
    }
    */
}
