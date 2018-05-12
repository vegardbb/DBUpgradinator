package dba;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
    private static final Logger logger = LogManager.getLogger("DBUpgradinator");
    private static AppNode[] getConfig(String filepath) {
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
            logger.error("Config Failure!", e);
        }
        return hosts.toArray(new AppNode[hosts.size()]);
    }

    // Server side: Upgrade starts upon receiving new transformer class, does not end
    // Arguments args:
    // 0: Absolute path to AggregateTransformerClass
    // 1: First argument to constructor, currentVersion
    // 2: Second argument to constructor, nextVersion
    // 3: Full name for the AggregateTransformerClass extending AbstractAggregateTransformer, used in loadClass
    // 4: Absolute path to conf.xml
    public static void main(String[] args) {
        if (args.length != 5) {
            logger.error("You did not specify enough arguments!");
            System.exit(1);
        }
        String fileName = args[0];
        String[] array = new String[] { args[1], args[2] };
        String className = args[3];

        try {
            Path path = Paths.get(fileName);
            byte[] data = Files.readAllBytes(path);

            AppNode[] listOfServers = getConfig(args[4]);
            for (AppNode node : listOfServers) {
                try {
                    ObjectOutputStream out = new ObjectOutputStream(new Socket(node.getAddress(), node.getPort()).getOutputStream());
                    // On the server side, remember to read each object in the same order as they were written
                    out.writeObject(data);
                    out.flush();
                    out.writeObject(array);
                    out.writeObject(className);
                    out.close();
                } catch (IOException io) {
                    logger.error("I/O Failure!", io);
                }
            }
        } catch (Exception ex) {
            logger.error("Client Failure!", ex);
        }
    }
}
