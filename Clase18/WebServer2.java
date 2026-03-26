import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.Executors;

public class WebServer2 {
    private static final String TASK_ENDPOINT = "/task";
    private static final String STATUS_ENDPOINT = "/status";
    private static final String IPN_ENDPOINT = "/ipn";  // Nuevo endpoint

    private final int port;
    private HttpServer server;

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
        HttpContext ipnContext = server.createContext(IPN_ENDPOINT); // Nuevo contexto

        statusContext.setHandler(this::handleStatusCheckRequest);
        taskContext.setHandler(this::handleTaskRequest);
        ipnContext.setHandler(this::handleIpnRequest); // Manejador para /ipn

        server.setExecutor(Executors.newFixedThreadPool(1));
        server.start();
    }

    private void handleTaskRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (!method.equalsIgnoreCase("post")) {
            exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed, sin cuerpo
            exchange.close();
            return;
        }

        // A partir de aquí, solo POST
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

        // Simulación de procesamiento pesado (sleep de 5 segundos)
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        byte[] responseBytes = calculateResponse(requestBytes);

        long finishTime = System.nanoTime();

        if (isDebugMode) {
            String debugMessage = String.format("La operación tomó %d nanosegundos", finishTime - startTime);
            exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        sendResponse(responseBytes, exchange);
    }

    private byte[] calculateResponse(byte[] requestBytes) {
        String bodyString = new String(requestBytes);
        String[] stringNumbers = bodyString.split(",");

        BigInteger result = BigInteger.ONE;

        for (String number : stringNumbers) {
            BigInteger bigInteger = new BigInteger(number.trim()); // .trim() por si hay espacios
            result = result.multiply(bigInteger);
        }

        return String.format("El resultado de la multiplicación es %s\n", result).getBytes();
    }

    private void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.sendResponseHeaders(405, -1);
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
        // Nuevo manejador para /ipn
    private void handleIpnRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        // Obtener User-Agent
        Headers headers = exchange.getRequestHeaders();
        String userAgent = headers.getFirst("User-Agent");

        // Determinar tipo de cliente
        boolean isBrowser = userAgent != null && (userAgent.contains("Mozilla") || userAgent.contains("Chrome") || userAgent.contains("Safari"));
        boolean isJavaClient = userAgent != null && (userAgent.contains("Java") || userAgent.contains("Java 11 HttpClient Bot"));

        Path filePath;
        String contentType;
        if (isBrowser) {
            // Enviar imagen PNG
            filePath = Paths.get("resources", "ipn.png");
            contentType = "image/png";
        } else if (isJavaClient) {
            // Enviar archivo de texto
            filePath = Paths.get("resources", "ascii-ipn.txt");
            contentType = "text/plain; charset=utf-8";
        } else {
            // Si no se reconoce, por defecto enviar texto (o podríamos enviar error)
            filePath = Paths.get("resources", "ascii-ipn.txt");
            contentType = "text/plain; charset=utf-8";
        }

        // Verificar que el archivo exista
        if (!Files.exists(filePath)) {
            String response = "Archivo no encontrado: " + filePath;
            exchange.sendResponseHeaders(404, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
            exchange.close();
            return;
        }

        // Leer archivo y enviar
        if (contentType.startsWith("image/")) {
            // Para imagen, leer como bytes
            byte[] fileBytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, fileBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(fileBytes);
            os.close();
        } else {
            // Para texto, leer como String (respetando codificación)
            String content = Files.readString(filePath);
            byte[] responseBytes = content.getBytes("utf-8");
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
        exchange.close();
    }
}