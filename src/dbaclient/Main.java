package dbaclient;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        String[] array = new String[] { args[1], args[2], args[3] };
        String className = args[4]; // not relevant?

        try {
            Path path = Paths.get(fileName);
            byte[] data = Files.readAllBytes(path);

            // Next, we serialize the object and send it over the network to each deatheater in the cluster
            ArrayList<AppNode> listOfServers = getConfig(args[5]);
            for (int r = 0; r < listOfServers.toArray().length; r++) {
                AppNode node = listOfServers.get(r);
                try {
                    ObjectOutputStream out = new ObjectOutputStream(new Socket(node.getAddress(), node.getPort()).getOutputStream());
                    // On the server side, remember to read each object in the same order as they were written
                    out.writeObject(data); // byte[]
                    out.flush();
                    out.writeObject(array); // String[]
                    out.writeObject(className); // String
                    out.close();
                } catch (IOException io) {
                    System.err.println(io.getMessage());
                }
            }
        } catch (MalformedURLException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.println(e.toString());
        } catch (Exception ex) {
            // Catch-all block
            ex.printStackTrace();
        }
    }
}
