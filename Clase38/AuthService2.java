// AuthService2.java
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class AuthService2 {
    private static final String SECURE_BALANCER_URL = "http://10.142.0.7/api/secure";
    private final int port;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String VALID_USER = "admin";
    private static final String VALID_PASS = "admin123";

    public AuthService2(int port) { this.port = port; }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        new AuthService2(port).start();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/secure", new SecureHandler());
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("AuthService2 listening on port " + port);
    }

    class SecureHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String username = extractValue(body, "username");
            String password = extractValue(body, "password");

            if (username == null || password == null) {
                sendResponse(exchange, 400, "{\"error\":\"username and password required\"}");
                return;
            }

            if (!VALID_USER.equals(username) || !VALID_PASS.equals(password)) {
                sendResponse(exchange, 401, "{\"error\":\"invalid credentials\"}");
                return;
            }

            String securePayload = "{\"username\":\"" + username + "\"}";

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SECURE_BALANCER_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(securePayload))
                        .timeout(java.time.Duration.ofSeconds(5))
                        .build();
                HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                sendResponse(exchange, resp.statusCode(), resp.body());
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 503, "{\"error\":\"secure service unavailable\"}");
            }
        }

        private String extractValue(String json, String key) {
            int idx = json.indexOf("\"" + key + "\"");
            if (idx == -1) return null;
            int start = json.indexOf(":", idx) + 1;
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            if (end == -1) return null;
            String raw = json.substring(start, end).trim();
            if (raw.startsWith("\"")) raw = raw.substring(1);
            if (raw.endsWith("\"")) raw = raw.substring(0, raw.length() - 1);
            return raw;
        }

        private void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
            exchange.close();
        }
    }
}