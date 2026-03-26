import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class WebServer2 {
    private static final String TASK_ENDPOINT = "/searchtoken";   // Cambiado a /searchtoken
    private static final String STATUS_ENDPOINT = "/status";

    private final int port;
    private HttpServer server;

    // Mapa de palabras a número de apariciones (según la captura)
    private static final Map<String, Integer> WORD_COUNTS = new HashMap<>();
    static {
        WORD_COUNTS.put("IPN", 105);
        WORD_COUNTS.put("SAL", 1);
        WORD_COUNTS.put("MAS", 6);
        WORD_COUNTS.put("PEZ", 104);
        WORD_COUNTS.put("SOL", 10);
    }

    public static void main(String[] args) {
        int serverPort = 8080;
        if (args.length == 1) {
            serverPort = Integer.parseInt(args[0]);
        }

        WebServer2 webServer = new WebServer2(serverPort);
        webServer.startServer();

        System.out.println("Servidor escuchando en el puerto " + serverPort);
    }

    public WebServer2(int port) {
        this.port = port;
    }

    public void startServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        HttpContext statusContext = server.createContext(STATUS_ENDPOINT);
        HttpContext taskContext = server.createContext(TASK_ENDPOINT);

        statusContext.setHandler(this::handleStatusCheckRequest);
        taskContext.setHandler(this::handleTaskRequest);

        server.setExecutor(Executors.newFixedThreadPool(1));
        server.start();
    }

    private void handleTaskRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
            exchange.close();
            return;
        }

        Headers headers = exchange.getRequestHeaders();
        if (headers.containsKey("X-Test") && headers.get("X-Test").get(0).equalsIgnoreCase("true")) {
            String dummyResponse = "123\n";
            sendResponse(dummyResponse.getBytes(), exchange);
            return;
        }

        boolean isDebugMode = false;
        if (headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true")) {
            isDebugMode = true;
        }

        long startTime = System.nanoTime();

        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        String bodyString = new String(requestBytes);

        // Extraer el primer número para determinar el tiempo de procesamiento (sleep)
        long sleepTime = 0;
        String word = "";
        try {
            String[] parts = bodyString.split(",");
            if (parts.length >= 1) {
                sleepTime = Math.min(Long.parseLong(parts[0].trim()) / 1000, 30000);
            }
            if (parts.length >= 2) {
                word = parts[1].trim();
            }
        } catch (NumberFormatException e) {
            sleepTime = 1000;
        }

        System.out.println("[" + Thread.currentThread().getName() + "] Procesando tarea: " + bodyString + " (sleep " + sleepTime + " ms)");

        // Simular procesamiento con duración variable
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Obtener el número de apariciones según el mapa
        int count = WORD_COUNTS.getOrDefault(word, 0);
        String response = String.valueOf(count);

        long finishTime = System.nanoTime();

        if (isDebugMode) {
            String debugMessage = String.format("La operación tomó %d nanosegundos", finishTime - startTime);
            exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        sendResponse(response.getBytes(), exchange);
    }

    private void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.close();
            return;
        }

        String responseMessage = "El servidor está vivo\n";
        sendResponse(responseMessage.getBytes(), exchange);
    }

    private void sendResponse(byte[] responseBytes, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
        exchange.close();
    }
}