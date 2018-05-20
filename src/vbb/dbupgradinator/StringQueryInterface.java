package vbb.dbupgradinator;

public interface StringQueryInterface {
    public String query(String key);
    public Exception persist(String key, String aggregate);
}
