package migrator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

// TODO: En indre klasse som implementerer Runnable-grensesnittet og som kjører som en egen tråd.
// Sitat Jenkov.com: "Inner classes are associated with an instance of the enclosing class. Thus, you must first create an instance of the enclosing class to create an instance of an inner class."
public class Migrator {
    private final int port;
    // private final String hostAddress;
    private final HashMap<String, AbstractAggregateTransformer> transformers = new HashMap<>(8, (float) 0.95);

    public Migrator(final int port) {
        this.port = port;
        // this.hostAddress = host;
        // Sets up separate process which listens actively on the server socket
        new Thread( this::aggregateTransformerReceiver ).start(); // make this a state variable?
    }

    private class AggregateTransformerLoader extends ClassLoader {
        AggregateTransformerLoader() { super(); }
        final Class<?> createClass(String name, byte[] b) throws ClassFormatError {
            return super.defineClass(name, b, 0, b.length);
        }
    }

    // Addition of transformer class to the HashMap
    private void addTransformer(AbstractAggregateTransformer t) { this.transformers.put(t.getAppVersion(), t); }

    // Separate process that actively listens for new classes that extend AAT
    private void aggregateTransformerReceiver() {
        // Define ClassLoader instance from anonymous class
        AggregateTransformerLoader loader = new AggregateTransformerLoader();
        // Define ServerSocket instance
        // I will run forever!
            try {
                ServerSocket server = new ServerSocket(this.port);
                //noinspection InfiniteLoopStatement
                while ( true ) {
                    Socket s = server.accept(); // When a connection lands, we move on to the next line in this program
                    ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                    // Read objects
                    byte[] classData = (byte[]) in.readObject();
                    String[] consArgs = (String[]) in.readObject();
                    String className = (String) in.readObject();
                    // Probably smart to do this?
                    in.close();
                    Class c = loader.createClass(className, classData);
                    @SuppressWarnings("unchecked") // Just for this one statement
                    Constructor cons = c.getConstructor(String.class, String.class, String.class);
                    AbstractAggregateTransformer tran = (AbstractAggregateTransformer) cons.newInstance( consArgs[0], consArgs[1], consArgs[2]);
                    // What to do with the transformer object: Add it to the AAT list
                    this.addTransformer(tran);
                }
            } catch (IOException io) {
                System.err.println(io.getMessage());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                // Catch-all case
                System.err.print(e.getMessage());
            }
    }

    public int getPort() {
        return port;
    }

    /*
    public String getHostAddress() {
        return hostAddress;
    } */

}
