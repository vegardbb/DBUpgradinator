package app;

/*
  Main file in which server application logic is written. Services three types of HTTP requests, POST, PUT and GET.
  Uses spark-java to declare a set of routes in the main method, which the Spark module
 */

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import static spark.Spark.*;
import vbb.dbupgradinator.Migrator;
import voldemort.client.ClientConfig;
import java.util.UUID;

public class WebShop {
    private static List<String> getConfig(String filepath) {
        // Define lists of strings to keep urls the client shall connect to
        List<String> hosts = new ArrayList<>();
        try {
            File fXmlFile = new File(filepath);
            DocumentBuilderFactory bFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = bFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            // Optional, but recommended
            // See http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();
            System.out.println("Root element :" + doc.getDocumentElement().getNodeName()); // prints out ":cluster"

            // Get servers
            NodeList nList = doc.getElementsByTagName("server");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    hosts.add("tcp://" + eElement.getElementsByTagName("host").item(0).getTextContent() + ":" + eElement.getElementsByTagName("socket-port").item(0).getTextContent());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hosts;
    }
    private static boolean stringNotInteger(String str) {
        if (str == null) {
            return true;
        }
        int length = str.length();
        if (length < 4 || length > 5) {
            return true;
        }
        int i;
        for (i = 0; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return true;
            }
        }
        return false;
    }

    // 0: HTTP port used to contact Spark server
    // 1: file path to Voldemort config
    public static void main(String[] args) {
        if (args.length != 2) {
            System.exit(1);
        }
        if (stringNotInteger(args[0])) {
            System.exit(1);
        }
        // setEnableInconsistencyResolvingLayer
        ClientConfig cli = new ClientConfig();
        List<String> urls = getConfig(args[1]);
        if (urls.isEmpty()) { System.exit(1); }
        cli.setBootstrapUrls(urls);
        cli.setEnableInconsistencyResolvingLayer(true);
        StringQueryExecutor dbi = new StringQueryExecutor(cli);
        Migrator m = new Migrator();

        port((Integer.parseInt(args[0]))); // Starts the server
        get("/hello", (request, response) -> "{\"message\": \"I am alive!\"}");

        // Creates a new aggregate
        // The schema version of the frontend instance is sent in the post body as x-www-urlencoded values e.g. ?schema=foo
        post("/api", "application/json", (request, response) -> {
            String schemaVersion = request.queryParamOrDefault("schema","x");
            String uid = UUID.randomUUID().toString();
            String jsonObj = request.body();
            boolean flag = m.migrateAndPutAggregate(dbi, uid, schemaVersion, jsonObj);
            if (flag) {
                response.status(201); // 201 Created
                return "{ \"ok\": true, \"message\": \"Successfully persisted aggregate having id "+ uid + ", belonging to schema version " + schemaVersion + "\"" + " }";
            }
            response.status(500); // Caught exception server side
            return "{ \"ok\": false, \"message\": \"Creation of object failed.\" }";
        });

        get("/api/:id", (request, response) -> {
            String schemaVersion = request.queryParams("schema");
            if (schemaVersion == null) {
                response.status(404); // 404 Not found
                return "{ \"ok\": false, \"aggregate\": null, \"message\": \"Schema version is undefined!\"}";
            }
            return "{ \"ok\": true, \"aggregate\": " + m.getAndMigrateAggregate(dbi, request.params(":id"), schemaVersion) + ", \"message\": \"Query successful!\"}";
        });

        put("/api/:id", "application/json", (request, response) -> {
            String schemaVersion = request.queryParamOrDefault("schema","x");
            String uid = request.params(":id");
            String jsonObj = request.body();
            boolean flag = m.migrateAndPutAggregate(dbi, uid, schemaVersion, jsonObj);
            if (flag) {
                response.status(201); // 201 Created
                return "{ \"ok\": true, \"message\": \"Successfully persisted aggregate having id "+ uid + ", belonging to schema version " + schemaVersion + "\"" + " }";
            }
            response.status(500); // Caught exception server side
            return "{ \"ok\": false, \"message\": \"Creation of object failed.\"}";
        });

        after((req, res) -> res.type("application/json"));

    }
}
