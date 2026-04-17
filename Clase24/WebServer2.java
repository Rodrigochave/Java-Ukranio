import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class WebServer2 {
    private static final String STATUS_ENDPOINT = "/status";
    private static final String ALUMNOS_ENDPOINT = "/alumnos";

    private final int port;
    private HttpServer server;
    private final Map<Integer, Alumno> alumnos = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final Gson gson = new Gson();

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
        // Endpoints específicos
        HttpContext statusContext = server.createContext(STATUS_ENDPOINT);
        HttpContext alumnosContext = server.createContext(ALUMNOS_ENDPOINT);
        statusContext.setHandler(this::handleStatusCheckRequest);
        alumnosContext.setHandler(this::handleAlumnosRequest);

        // Endpoint comodín para rutas inexistentes (inciso 1)
        server.createContext("/", this::handleNotFound);

        server.setExecutor(Executors.newFixedThreadPool(1));
        server.start();
    }

    // Inciso 1: Endpoint inexistente -> 404 con JSON
    private void handleNotFound(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        // Evitar interferir con los endpoints conocidos
        if (path.equals(STATUS_ENDPOINT) || path.startsWith(ALUMNOS_ENDPOINT)) {
            exchange.close();
            return;
        }
        sendError(404, "Recurso no encontrado", exchange);
    }

    private void handleAlumnosRequest(HttpExchange exchange) throws IOException {
        String ruta = exchange.getRequestURI().getPath();
        String metodo = exchange.getRequestMethod();
        String[] partes = ruta.split("/");

        try {
            if (metodo.equalsIgnoreCase("get")) {
                // GET /alumnos/{id}
                if (partes.length == 3) {
                    int id;
                    try {
                        id = Integer.parseInt(partes[2]);
                    } catch (NumberFormatException e) {
                        sendError(400, "ID invalido", exchange); // Inciso 2
                        return;
                    }
                    Alumno a = alumnos.get(id);
                    if (a != null) {
                        sendResponse(200, gson.toJson(a), exchange); // Inciso 4
                    } else {
                        sendError(404, "Alumno no encontrado", exchange); // Inciso 4
                    }
                } else {
                    sendError(400, "Formato incorrecto. Use /alumnos/{id}", exchange);
                }
            } else if (metodo.equalsIgnoreCase("post")) {
                // POST /alumnos (inciso 3)
                if (partes.length == 2) {
                    String body = new String(exchange.getRequestBody().readAllBytes());
                    try {
                        Alumno nuevo = gson.fromJson(body, Alumno.class);
                        if (nuevo.getNombre() == null || nuevo.getApellido1() == null || nuevo.getApellido2() == null) {
                            sendError(400, "Faltan campos requeridos", exchange);
                            return;
                        }
                        int id = nextId.getAndIncrement();
                        nuevo.setId(id);
                        alumnos.put(id, nuevo);
                        sendResponse(201, gson.toJson(nuevo), exchange); // 201 + JSON con id
                    } catch (JsonSyntaxException e) {
                        sendError(400, "JSON mal formado", exchange);
                    }
                } else {
                    sendError(400, "Para POST use /alumnos", exchange);
                }
            } else if (metodo.equalsIgnoreCase("put")) {
                // PUT /alumnos/{id} (inciso 5)
                if (partes.length == 3) {
                    int id;
                    try {
                        id = Integer.parseInt(partes[2]);
                    } catch (NumberFormatException e) {
                        sendError(400, "ID invalido", exchange);
                        return;
                    }
                    if (!alumnos.containsKey(id)) {
                        sendError(404, "Alumno no encontrado", exchange);
                        return;
                    }
                    String body = new String(exchange.getRequestBody().readAllBytes());
                    try {
                        Alumno actualizado = gson.fromJson(body, Alumno.class);
                        if (actualizado.getNombre() == null || actualizado.getApellido1() == null) {
                            sendError(400, "Faltan campos requeridos", exchange);
                            return;
                        }
                        actualizado.setId(id);
                        alumnos.put(id, actualizado);
                        Map<String, String> respuesta = new HashMap<>();
                        respuesta.put("mensaje", "Alumno actualizado");
                        sendResponse(200, gson.toJson(respuesta), exchange);
                    } catch (JsonSyntaxException e) {
                        sendError(400, "JSON mal formado", exchange);
                    }
                } else {
                    sendError(400, "Use /alumnos/{id} para PUT", exchange);
                }
            } else if (metodo.equalsIgnoreCase("delete")) {
                // DELETE /alumnos/{id} (inciso 6)
                if (partes.length == 3) {
                    int id;
                    try {
                        id = Integer.parseInt(partes[2]);
                    } catch (NumberFormatException e) {
                        sendError(400, "ID invalido", exchange);
                        return;
                    }
                    if (alumnos.remove(id) != null) {
                        sendResponse(204, null, exchange); // 204 sin cuerpo
                    } else {
                        sendError(404, "Alumno no encontrado", exchange);
                    }
                } else {
                    sendError(400, "Use /alumnos/{id} para DELETE", exchange);
                }
            } else {
                sendError(405, "Método no permitido", exchange);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(500, "Error interno del servidor", exchange);
        }
    }

    private void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            sendError(405, "Método no permitido", exchange);
            return;
        }
        sendResponse(200, "El servidor está vivo\n", exchange);
    }

    // Envía respuesta con código HTTP y cuerpo (puede ser null)
    private void sendResponse(int statusCode, String responseBody, HttpExchange exchange) throws IOException {
        byte[] bytes = responseBody != null ? responseBody.getBytes() : new byte[0];
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            if (bytes.length > 0) os.write(bytes);
        }
        exchange.close();
    }

    // Envía error en formato JSON {"error": "mensaje"}
    private void sendError(int statusCode, String mensaje, HttpExchange exchange) throws IOException {
        Map<String, String> error = new HashMap<>();
        error.put("error", mensaje);
        String json = gson.toJson(error);
        sendResponse(statusCode, json, exchange);
    }
}