package co.edu.escuelaing.reflexionlab;

import co.edu.escuelaing.reflexionlab.annotation.GetMapping;
import co.edu.escuelaing.reflexionlab.annotation.RequestParam;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP Web Server that handles GET requests.
 * - Routes registered via @GetMapping are dispatched to Java methods (REST).
 * - Unregistered routes serve static files from the resources/static directory.
 * Handles one connection at a time (non-concurrent, as required).
 */
public class HttpServer {

    private static final int PORT = 8080;
    private static final String STATIC_DIR = "src/main/resources/static";

    // Map from URI path -> (controller instance, method)
    private final Map<String, Object[]> routeMap = new HashMap<>();

    /**
     * Registers a controller class: instantiates it and registers all @GetMapping
     * methods.
     */
    public void registerController(Class<?> controllerClass) {
        try {
            Object instance = controllerClass.getDeclaredConstructor().newInstance();
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GetMapping.class)) {
                    GetMapping mapping = method.getAnnotation(GetMapping.class);
                    String path = mapping.value();
                    routeMap.put(path, new Object[] { instance, method });
                    System.out.println("[HttpServer] Registered route: GET " + path
                            + " -> " + controllerClass.getSimpleName() + "." + method.getName() + "()");
                }
            }
        } catch (Exception e) {
            System.err.println("[HttpServer] Failed to register controller: " + controllerClass.getName());
            e.printStackTrace();
        }
    }

    /**
     * Starts the server and listens for incoming connections on PORT 8080.
     */
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("=================================================");
            System.out.println("  MicroSpring Boot Server started on port " + PORT);
            System.out.println("  Open: http://localhost:" + PORT + "/");
            System.out.println("=================================================");

            // Main loop: sequential (non-concurrent) request handling
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleRequest(clientSocket);
                } catch (Exception e) {
                    System.err.println("[HttpServer] Error handling request: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Reads an HTTP GET request and routes it appropriately.
     */
    private void handleRequest(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        OutputStream out = clientSocket.getOutputStream();

        // Read the first line: "GET /path?query HTTP/1.1"
        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isEmpty())
            return;

        System.out.println("[HttpServer] Request: " + requestLine);

        // Parse method and URI
        String[] parts = requestLine.split(" ");
        if (parts.length < 2 || !parts[0].equals("GET")) {
            sendResponse(out, 405, "text/plain", "Method Not Allowed");
            return;
        }

        String fullPath = parts[1]; // e.g. /greeting?name=Diego
        String path = fullPath.contains("?") ? fullPath.substring(0, fullPath.indexOf('?')) : fullPath;
        String queryString = fullPath.contains("?") ? fullPath.substring(fullPath.indexOf('?') + 1) : "";

        // Parse query parameters
        Map<String, String> queryParams = parseQueryParams(queryString);

        // Check if it's a registered REST route
        if (routeMap.containsKey(path)) {
            handleRestRoute(path, queryParams, out);
        } else {
            // Try to serve a static file
            serveStaticFile(path, out);
        }
    }

    /**
     * Invokes the registered Java method via reflection and returns the result as
     * HTTP response.
     */
    private void handleRestRoute(String path, Map<String, String> queryParams, OutputStream out) throws IOException {
        Object[] entry = routeMap.get(path);
        Object instance = entry[0];
        Method method = (Method) entry[1];

        try {
            // Build argument array based on @RequestParam annotations
            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].isAnnotationPresent(RequestParam.class)) {
                    RequestParam rp = parameters[i].getAnnotation(RequestParam.class);
                    String paramName = rp.value();
                    String defaultVal = rp.defaultValue();
                    args[i] = queryParams.getOrDefault(paramName, defaultVal);
                } else {
                    args[i] = null;
                }
            }

            // Invoke the method reflectively
            String result = (String) method.invoke(instance, args);
            sendResponse(out, 200, "text/plain; charset=UTF-8", result);

        } catch (Exception e) {
            System.err.println("[HttpServer] Error invoking method for path " + path + ": " + e.getMessage());
            sendResponse(out, 500, "text/plain", "Internal Server Error: " + e.getMessage());
        }
    }

    /**
     * Serves a static file from the resources/static directory.
     * Supports HTML, PNG, CSS, JS, ICO.
     */
    private void serveStaticFile(String path, OutputStream out) throws IOException {
        // Default to index.html for root path
        if (path.equals("/"))
            path = "/index.html";

        // Look for file in resources/static
        File file = new File(STATIC_DIR + path);

        // Also try from classpath (for packaged jar support)
        if (!file.exists()) {
            InputStream is = HttpServer.class.getResourceAsStream("/static" + path);
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                String contentType = getContentType(path);
                sendBinaryResponse(out, 200, contentType, bytes);
                return;
            }
            sendResponse(out, 404, "text/html",
                    "<html><body><h1>404 - Not Found</h1><p>Resource: " + path + "</p></body></html>");
            return;
        }

        String contentType = getContentType(path);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        sendBinaryResponse(out, 200, contentType, fileBytes);
    }

    /**
     * Sends a text HTTP response.
     */
    private void sendResponse(OutputStream out, int statusCode, String contentType, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        sendBinaryResponse(out, statusCode, contentType, bodyBytes);
    }

    /**
     * Sends a binary HTTP response (used for both text and images).
     */
    private void sendBinaryResponse(OutputStream out, int statusCode, String contentType, byte[] body)
            throws IOException {
        String statusText = statusCode == 200 ? "OK" : statusCode == 404 ? "Not Found" : "Error";
        String header = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    /**
     * Parses query parameters from a URL query string.
     * Example: "name=Diego&lang=Java" -> {name: Diego, lang: Java}
     */
    private Map<String, String> parseQueryParams(String queryString) {
        Map<String, String> params = new HashMap<>();
        if (queryString == null || queryString.isEmpty())
            return params;

        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            try {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                params.put(key, value);
            } catch (Exception e) {
                // Skip malformed pairs
            }
        }
        return params;
    }

    /**
     * Determines the Content-Type based on the file extension.
     */
    private String getContentType(String path) {
        if (path.endsWith(".html") || path.endsWith(".htm"))
            return "text/html; charset=UTF-8";
        if (path.endsWith(".css"))
            return "text/css";
        if (path.endsWith(".js"))
            return "application/javascript";
        if (path.endsWith(".png"))
            return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
            return "image/jpeg";
        if (path.endsWith(".gif"))
            return "image/gif";
        if (path.endsWith(".ico"))
            return "image/x-icon";
        if (path.endsWith(".json"))
            return "application/json";
        return "text/plain";
    }
}
