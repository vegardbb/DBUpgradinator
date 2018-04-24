package dbaclient;

import java.io.File;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import dbaclient.AbstractAggregateTransformer;

public class Main {

    // Server side: Upgrade starts upon receiving new transformer class, does not end
    // Arguments args:
    // 0: Absolute path to AggregateTransformerClass
    // 1: First argument to constructor, previousVersion
    // 2: Second argument til constructor, currentVersion
    // 3: Third argument til constructor, nextVersion
    // 4: Full name for the AggregateTransformerClass extending AbstractAggregateTransformer, used in loadClass
    public static void main(String[] args) {
        String fileName = args[0];
        // String[] constructorParams = new String[] { args[1], args[2], args[3] };
        String className = args[4];
        File file = new File(fileName);

        try {
            // Convert File to a URL
            URL url = file.toURI().toURL();
            URL[] addresses = new URL[] {url};
            // Create a new class loader with the directory
            ClassLoader cl = new URLClassLoader(addresses);
            // Load in the class; MyClass.class should be located in
            // the directory file:/c:/myclasses/com/mycompany
            Class c = cl.loadClass(className);
            @SuppressWarnings("unchecked") // Just for this one statement
            Constructor cons = c.getConstructor(String.class, String.class, String.class);
            // Instantiate transformer class but cast it to its parent AbstractAggregateTransformer
            AbstractAggregateTransformer tran = (AbstractAggregateTransformer) cons.newInstance(args[1], args[2], args[3]);
            // Next, we serialize the object and send it over the network to each DBUpgradinator client in the cluster
            // TODO: Make class serializable.
            // TODO: Load Voldemort Stores config dynamically to get sockets, IPs and URL destinations

            // TODO: Make class serializable + load config files.
            Socket s = new Socket("yourhostname.co.uk", 12345);
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            out.writeObject(tran);
            out.flush();
        } catch (MalformedURLException e) {
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
            System.err.println(e.toString());
        } catch (Exception ex) {
            // Catch-all block
            ex.printStackTrace();
        }
    }
}

/*
* Problems
* 1) Load a class from compiled class file and instantiate it
*
* In UpgraderClient: Abstract prototype of class
* */