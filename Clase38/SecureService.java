import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.Executors;

public class SecureService {
    private static final String LOG_FILE = "/tmp/secure.log";
    private final int port;

    public SecureService(int port) { this.port = port; }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9090;
        new SecureService(port).start();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/secure", new SecureHandler());
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("SecureService listening on port " + port);
    }

    class SecureHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            // Leer body
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String username = extractUsername(body);

            if (username == null) {
                sendResponse(exchange, 400, "{\"error\":\"username required\"}");
                return;
            }

            // Escribir en archivo
            String timestamp = Instant.now().toString();
            try (FileWriter fw = new FileWriter(LOG_FILE, true);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(username + "," + timestamp + "\n");
            } catch (IOException e) {
                sendResponse(exchange, 500, "{\"error\":\"cannot write log\"}");
                return;
            }

            String response = String.format("{\"status\":\"ok\",\"username\":\"%s\",\"timestamp\":\"%s\"}", username, timestamp);
            sendResponse(exchange, 200, response);
        }

        private String extractUsername(String json) {
            // parsing simple: busca "username":"valor"
            int idx = json.indexOf("\"username\"");
            if (idx == -1) return null;
            int start = json.indexOf(":", idx) + 1;
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            if (end == -1) return null;
            String raw = json.substring(start, end).trim();
            if (raw.startsWith("\"")) raw = raw.substring(1);
            if (raw.endsWith("\"")) raw = raw.substring(0, raw.length()-1);
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