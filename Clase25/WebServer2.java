import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class WebServer2 {
    private static final String STATUS_ENDPOINT = "/status";
    private static final String ALUMNOS_ENDPOINT = "/alumnos";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

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
        HttpContext statusContext = server.createContext(STATUS_ENDPOINT);
        HttpContext alumnosContext = server.createContext(ALUMNOS_ENDPOINT);
        statusContext.setHandler(this::handleStatusCheckRequest);
        alumnosContext.setHandler(this::handleAlumnosRequest);

        server.createContext("/", this::handleNotFound);

        server.setExecutor(Executors.newFixedThreadPool(1));
        server.start();
    }

    private void handleNotFound(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
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
                if (partes.length == 2) {
                    // GET /alumnos con query parameters
                    String query = exchange.getRequestURI().getQuery();
                    // Primero aplicar filtros (minPromedio, nombre)
                    List<Alumno> filtrados = filtrarAlumnos(alumnos.values(), query);
                    // Luego aplicar paginación (page, size)
                    Map<String, Integer> paginacion = extraerPaginacion(query);
                    if (paginacion == null) {
                        sendError(400, "Parámetros de página inválidos. Use page y size como enteros positivos", exchange);
                        return;
                    }
                    int page = paginacion.get("page");
                    int size = paginacion.get("size");
                    List<Alumno> pagina = paginar(filtrados, page, size);
                    sendResponse(200, gson.toJson(pagina), exchange);
                } else if (partes.length == 3) {
                    // GET /alumnos/{id}
                    int id;
                    try {
                        id = Integer.parseInt(partes[2]);
                    } catch (NumberFormatException e) {
                        sendError(400, "ID invalido", exchange);
                        return;
                    }
                    Alumno a = alumnos.get(id);
                    if (a != null) {
                        sendResponse(200, gson.toJson(a), exchange);
                    } else {
                        sendError(404, "Alumno no encontrado", exchange);
                    }
                } else {
                    sendError(400, "Formato incorrecto. Use /alumnos o /alumnos/{id}", exchange);
                }
            } else if (metodo.equalsIgnoreCase("post")) {
                // ... (código POST sin cambios)
                if (partes.length == 2) {
                    String body = new String(exchange.getRequestBody().readAllBytes());
                    try {
                        Alumno nuevo = gson.fromJson(body, Alumno.class);
                        if (nuevo.getNombre() == null || nuevo.getApellido1() == null || nuevo.getApellido2() == null) {
                            sendError(400, "Faltan campos requeridos (nombre, apellido1, apellido2)", exchange);
                            return;
                        }
                        int id = nextId.getAndIncrement();
                        nuevo.setId(id);
                        alumnos.put(id, nuevo);
                        sendResponse(201, gson.toJson(nuevo), exchange);
                    } catch (JsonSyntaxException e) {
                        sendError(400, "JSON mal formado", exchange);
                    }
                } else {
                    sendError(400, "Para POST use /alumnos", exchange);
                }
            } else if (metodo.equalsIgnoreCase("put")) {
                // ... (código PUT sin cambios)
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
                            sendError(400, "Faltan campos requeridos (nombre, apellido1)", exchange);
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
                // ... (código DELETE sin cambios)
                if (partes.length == 3) {
                    int id;
                    try {
                        id = Integer.parseInt(partes[2]);
                    } catch (NumberFormatException e) {
                        sendError(400, "ID invalido", exchange);
                        return;
                    }
                    if (alumnos.remove(id) != null) {
                        sendResponse(204, null, exchange);
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

    // Filtrado por minPromedio y nombre (como antes)
    private List<Alumno> filtrarAlumnos(Collection<Alumno> alumnos, String query) {
        if (query == null || query.isEmpty()) {
            return new ArrayList<>(alumnos);
        }
        Double minPromedio = null;
        String nombreInicio = null;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                if ("minPromedio".equalsIgnoreCase(key)) {
                    try {
                        minPromedio = Double.parseDouble(value);
                    } catch (NumberFormatException e) { /* ignorar */ }
                } else if ("nombre".equalsIgnoreCase(key)) {
                    nombreInicio = value;
                }
            }
        }
        final Double minP = minPromedio;
        final String nombrePref = nombreInicio;
        return alumnos.stream()
                .filter(a -> {
                    if (minP != null && a.getPromedio() < minP) return false;
                    if (nombrePref != null && !a.getNombre().startsWith(nombrePref)) return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    // Extrae page y size del query string. Devuelve null si hay error de formato.
    private Map<String, Integer> extraerPaginacion(String query) {
        int page = DEFAULT_PAGE;
        int size = DEFAULT_SIZE;
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    if ("page".equalsIgnoreCase(key)) {
                        try {
                            page = Integer.parseInt(value);
                            if (page < 1) return null;
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    } else if ("size".equalsIgnoreCase(key)) {
                        try {
                            size = Integer.parseInt(value);
                            if (size < 1) return null;
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }
                }
            }
        }
        Map<String, Integer> result = new HashMap<>();
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    // Aplica paginación a una lista
    private List<Alumno> paginar(List<Alumno> lista, int page, int size) {
        int fromIndex = (page - 1) * size;
        if (fromIndex >= lista.size()) {
            return new ArrayList<>(); // página más allá del final
        }
        int toIndex = Math.min(fromIndex + size, lista.size());
        return lista.subList(fromIndex, toIndex);
    }

    private void sendResponse(int statusCode, String responseBody, HttpExchange exchange) throws IOException {
        byte[] bytes = responseBody != null ? responseBody.getBytes() : new byte[0];
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            if (bytes.length > 0) os.write(bytes);
        }
        exchange.close();
    }

    private void sendError(int statusCode, String mensaje, HttpExchange exchange) throws IOException {
        Map<String, String> error = new HashMap<>();
        error.put("error", mensaje);
        String json = gson.toJson(error);
        sendResponse(statusCode, json, exchange);
    }
}