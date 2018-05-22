package app;

import voldemort.client.ClientConfig;
import voldemort.client.SocketStoreClientFactory;
import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.versioning.ObsoleteVersionException;
import voldemort.versioning.Versioned;
import vbb.dbupgradinator.StringQueryInterface;

public class StringQueryExecutor implements StringQueryInterface {
    private StoreClient<String, String> client;

    StringQueryExecutor(ClientConfig con) {
        StoreClientFactory factory = new SocketStoreClientFactory(con);

        // The name of the cluster is indeed "test"
        this.client = factory.getStoreClient("test");
    }

    @Override
    public String query(String key) {
        Versioned<String> versioned = client.get(key);
        return String.valueOf(versioned.getValue());
    }

    @Override
    public Exception persist(String key, String s) {
        try {
            client.put(key, s);
            return null;
        } catch (ObsoleteVersionException ove) {
            return ove;
        }
    }
}
