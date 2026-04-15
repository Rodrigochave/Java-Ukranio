import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class DispatcherServer {
    private static final String DISPATCHER_ENDPOINT = "/dispatcher";
    private static final String WORKER_ADDRESS_1 = "http://localhost:8081/task";
    private static final String WORKER_ADDRESS_2 = "http://localhost:8082/task";

    private final int port;
    private HttpServer server;

    public static void main(String[] args) throws IOException {
        int serverPort = 9090; // puerto del despachador
        if (args.length == 1) {
            serverPort = Integer.parseInt(args[0]);
        }
        DispatcherServer dispatcher = new DispatcherServer(serverPort);
        dispatcher.startServer();
        System.out.println("Dispatcher escuchando en el puerto " + serverPort);
    }

    public DispatcherServer(int port) {
        this.port = port;
    }

    public void startServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        HttpContext dispatcherContext = server.createContext(DISPATCHER_ENDPOINT);
        dispatcherContext.setHandler(this::handleDispatchRequest);
        server.setExecutor(Executors.newFixedThreadPool(1));
        server.start();
    }

private void handleDispatchRequest(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
        exchange.sendResponseHeaders(405, -1);
        exchange.close();
        return;
    }

    // Leer el hash del cuerpo de la petición
    byte[] requestBody = exchange.getRequestBody().readAllBytes();
    String hashObjetivo = new String(requestBody).trim();

    if (hashObjetivo.isEmpty()) {
        sendResponse("Falta el hash objetivo".getBytes(), exchange, 400);
        return;
    }

    System.out.println("Hash recibido: " + hashObjetivo);

    // --- INICIO DE MEDICIÓN DE TIEMPO ---
    long startTime = System.currentTimeMillis();

    // Generar las 26 tareas
    List<String> tareas = new ArrayList<>();
    for (char letra = 'a'; letra <= 'z'; letra++) {
        tareas.add(hashObjetivo + "," + letra);
    }

    System.out.println("Generadas " + tareas.size() + " tareas. Enviando a workers...");

    // Crear aggregator y enviar tareas
    Aggregator aggregator = new Aggregator();
    List<String> workers = Arrays.asList(WORKER_ADDRESS_1, WORKER_ADDRESS_2);
    List<String> resultados = aggregator.sendTasksToWorkersDynamic(workers, tareas);

    // Buscar la primera respuesta que no sea "NULL"
    String passwordEncontrada = null;
    for (String res : resultados) {
        if (res != null && !res.equals("NULL")) {
            passwordEncontrada = res;
            break;
        }
    }

    // --- FIN DE MEDICIÓN DE TIEMPO ---
    long endTime = System.currentTimeMillis();
    long elapsedMs = endTime - startTime;

    System.out.println("Tiempo total de despacho: " + elapsedMs + " ms");

    // Construir respuesta incluyendo el tiempo
    String respuesta;
    if (passwordEncontrada != null) {
        respuesta = "Contraseña: " + passwordEncontrada + " | Tiempo: " + elapsedMs + " ms";
    } else {
        respuesta = "NULL | Tiempo: " + elapsedMs + " ms";
    }

    System.out.println("Respuesta: " + respuesta);
    sendResponse(respuesta.getBytes(), exchange, 200);
}

    private void sendResponse(byte[] responseBytes, HttpExchange exchange, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.flush();
        os.close();
        exchange.close();
    }
}