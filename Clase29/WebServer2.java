import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.Executors;

public class WebServer2 {
    private static final String TASK_ENDPOINT = "/task";
    private static final String STATUS_ENDPOINT = "/status";

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

        boolean isDebugMode = false;
        if (headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true")) {
            isDebugMode = true;
        }

        long startTime = System.nanoTime();

        // Leer el cuerpo de la petición
        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        String body = new String(requestBytes).trim();

        String[] parts = body.split(",");
        if (parts.length != 2) {
            // Formato incorrecto
            sendResponse("Formato incorrecto. Use: hash,primeraLetra".getBytes(), exchange);
            exchange.close();
            return;
        }

        String hashObjetivo = parts[0].trim();
        String primeraLetraStr = parts[1].trim();

        if (primeraLetraStr.length() != 1 || primeraLetraStr.charAt(0) < 'a' || primeraLetraStr.charAt(0) > 'z') {
            sendResponse("La primera letra debe ser una letra minúscula de 'a' a 'z'".getBytes(), exchange);
            exchange.close();
            return;
        }

        char primeraLetra = primeraLetraStr.charAt(0);

        // Buscar la contraseña
        String password = buscarPassword(hashObjetivo, primeraLetra);

        long finishTime = System.nanoTime();
        long durationNanos = finishTime - startTime;
        long durationMillis = durationNanos / 1_000_000;
        long durationSeconds = durationMillis / 1000;
        long remainingMillis = durationMillis % 1000;

        if (isDebugMode) {
            String debugMessage = String.format("Operación tomó %d segundos %d milisegundos", durationSeconds, remainingMillis);
            exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        // Enviar respuesta: la contraseña encontrada o "NULL"
        String responseBody = (password != null) ? password : "NULL";
        
        sendResponse(responseBody.getBytes(), exchange);
    }

    /**
     * Busca una contraseña de 5 letras minúsculas que comience con la letra dada
     * y cuyo hash SHA-256 coincida con el objetivo.
     *
     * @param hashObjetivo hash SHA-256 a buscar
     * @param primeraLetra primera letra fija
     * @return la contraseña encontrada o null si no se encuentra
     */
    private String buscarPassword(String hashObjetivo, char primeraLetra) {
        // Generar todas las combinaciones de las 2 letras restantes (a-z)
        for (char b = 'a'; b <= 'z'; b++) {
            for (char c = 'a'; c <= 'z'; c++) {
                String intento = "" + primeraLetra + b + c;
                try {
                    String hash = sha256(intento);
                    if (hash.equals(hashObjetivo)) {
                        return intento;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return null;
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

    /**
     * Calcula el hash SHA-256 de un texto.
     *
     * @param texto texto a hashear
     * @return representación hexadecimal del hash
     * @throws Exception si el algoritmo no está disponible
     */
    public static String sha256(String texto) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(texto.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}