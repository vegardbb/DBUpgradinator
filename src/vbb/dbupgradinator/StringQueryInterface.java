package vbb.dbupgradinator;

public interface StringQueryInterface {
    public String query(String key);
    public String persist(String key, String aggregate);
}
