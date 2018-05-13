package vbb.dbupgradinator;

public interface StringQueryInterface {
    public String query(String key);
    public void persist(String key, String aggregate);
}
