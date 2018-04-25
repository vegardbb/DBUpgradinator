package dbaclient;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.util.ArrayList;

// Class reserved for this file
class AppNode {
    private final int port;
    private final String hostAddress;

    AppNode(final int first, final String second) {
        this.port = first;
        this.hostAddress = second;
    }
    public int getPort() {
        return port;
    }

    public String getAddress() {
        return hostAddress;
    }
}

public class Main {
    private static ArrayList<AppNode> getConfig(String filepath) {
        // Define lists of strings to keep 1) host names and 2) ports
        ArrayList<AppNode> hosts = new ArrayList<>();
        try {
            File fXmlFile = new File(filepath);
            DocumentBuilderFactory bFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = bFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            // Optional, but recommended
            // See http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            // Get servers
            NodeList nList = doc.getElementsByTagName("server");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    hosts.add(new AppNode(Integer.parseInt(eElement.getElementsByTagName("socket-port").item(0).getTextContent()), eElement.getElementsByTagName("host").item(0).getTextContent()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hosts;
    }

    // Server side: Upgrade starts upon receiving new transformer class, does not end
    // Arguments args:
    // 0: Absolute path to AggregateTransformerClass
    // 1: First argument to constructor, previousVersion
    // 2: Second argument til constructor, currentVersion
    // 3: Third argument til constructor, nextVersion
    // 4: Full name for the AggregateTransformerClass extending AbstractAggregateTransformer, used in loadClass
    // 5: Absolute path to conf.xml
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
            // Next, we serialize the object and send it over the network to each deatheater in the cluster
            ArrayList<AppNode> listOfServers = getConfig(args[5]);
            for (int r = 0; r < listOfServers.toArray().length; r++) {
                AppNode node = listOfServers.get(r);
                try {
                    ObjectOutputStream out = new ObjectOutputStream(new Socket(node.getAddress(), node.getPort()).getOutputStream());
                    out.writeObject(tran);
                    out.flush();
                } catch (IOException io) {
                    System.err.println(io.getMessage());
                }
            }
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
*
*/
