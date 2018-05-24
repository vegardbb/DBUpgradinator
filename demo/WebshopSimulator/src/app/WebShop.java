package app;

/*
  Main file in which server application logic is written. Services three types of HTTP requests, POST, PUT and GET.
  Uses spark-java to declare a set of routes in the main method, which the Spark module
 */

import static spark.Spark.*;
import vbb.dbupgradinator.Migrator;
import voldemort.client.ClientConfig;
import java.util.UUID;

public class WebShop {
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

    // 0: socket-port for store client (the stores themselves are launched separately)
    // 1: HTTP port used to contact Spark server
    // 2: Class name for Aggregate Transformer
    public static void main(String[] args) {
        if (args.length != 2) {
            System.exit(1);
        }
        if (stringNotInteger(args[0]) || stringNotInteger(args[1])) {
            System.exit(1);
        }
        // setEnableInconsistencyResolvingLayer
        ClientConfig clico = new ClientConfig();
        clico.setBootstrapUrls("tcp://localhost:" + args[0]);
        clico.setEnableInconsistencyResolvingLayer(true);
        StringQueryExecutor dbi = new StringQueryExecutor(clico);
        Migrator m = new Migrator();

        port((Integer.parseInt(args[1]))); // Starts the server
        get("/hello", (request, response) -> "{\"message\": \"I am alive!\"}");

        // Creates a new aggregate
        // The schema version of the frontend instance is sent in the post body as x-www-urlencoded values e.g. ?schema=foo
        post("/api", "application/json", (request, response) -> {
            String schemaVersion = request.queryParamOrDefault("schema","v0");
            String uid = UUID.randomUUID().toString();
            String jsonObj = request.body();
            boolean flag = m.migrateAndPostAggregate(dbi, uid, schemaVersion, jsonObj);
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
            String schemaVersion = request.queryParamOrDefault("schema","v0");
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
