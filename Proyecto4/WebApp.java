import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Aplicación Web (coordinador) que distribuye la búsqueda entre 3 Workers.
 */
public class WebApp {
    // Endpoints
    private static final String SEARCH_ENDPOINT = "/api/search";
    private static final String QUERY_STATUS = "/api/query/";   // seguido de queryId/status
    private static final String QUERY_PAGE = "/api/query/";     // seguido de queryId?page=X

    private final int port;
    private final String textServerUrl;                // ej: http://localhost:8081
    private final List<String> workerUrls;             // ej: ["http://IP1:8080", ...]
    private HttpServer server;

    // Ejecutor para tareas de búsqueda en segundo plano
    private final ExecutorService searchExecutor = Executors.newFixedThreadPool(4);
    // Cliente HTTP síncrono para el TextServer y asíncrono para Workers
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    // Caché de resultados: queryId -> QueryResult
    private final ConcurrentHashMap<String, QueryResult> resultCache = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = 8082;
        String textServer = "http://localhost:8081";
        String worker1 = "http://localhost:8080";
        String worker2 = "http://localhost:8080";
        String worker3 = "http://localhost:8080";

        // Parseo sencillo de argumentos: --port 8082 --text-server URL --workers url1,url2,url3
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":
                    port = Integer.parseInt(args[++i]);
                    break;
                case "--text-server":
                    textServer = args[++i];
                    break;
                case "--workers":
                    String[] workersArr = args[++i].split(",");
                    if (workersArr.length >= 1) worker1 = workersArr[0];
                    if (workersArr.length >= 2) worker2 = workersArr[1];
                    if (workersArr.length >= 3) worker3 = workersArr[2];
                    break;
                default:
                    System.err.println("Uso: WebApp --port <puerto> --text-server <URL> --workers <url1,url2,url3>");
                    return;
            }
        }

        List<String> workerList = List.of(worker1, worker2, worker3);
        WebApp app = new WebApp(port, textServer, workerList);
        app.start();
        System.out.println("Aplicación Web escuchando en puerto " + port);
    }

    public WebApp(int port, String textServerUrl, List<String> workerUrls) {
        this.port = port;
        this.textServerUrl = textServerUrl.endsWith("/") ? 
                textServerUrl.substring(0, textServerUrl.length()-1) : textServerUrl;
        this.workerUrls = workerUrls;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        server.createContext(SEARCH_ENDPOINT, this::handleSearch);
        server.createContext(QUERY_STATUS, this::handleQueryRequest); // captura /api/query/...
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
    }

    // ---- Manejo de endpoints ----

    private void handleSearch(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        // Extraer el valor n de los parámetros de la query
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQueryParams(query);
        String nStr = params.get("n");
        if (nStr == null) {
            sendError(exchange, 400, "Parámetro 'n' requerido");
            return;
        }
        int n;
        try {
            n = Integer.parseInt(nStr);
            if (n <= 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "'n' debe ser un entero > 1");
            return;
        }

        String queryId = UUID.randomUUID().toString();
        // Inicializar entrada en cache como "procesando"
        QueryResult qr = new QueryResult("processing", 0, null);
        resultCache.put(queryId, qr);

        // Lanzar búsqueda en segundo plano
        searchExecutor.submit(() -> executeSearch(queryId, n));

        // Responder inmediatamente
        JSONObject response = new JSONObject();
        response.put("queryId", queryId);
        response.put("status", "processing");
        sendJson(exchange, 200, response.toString());
    }

    private void handleQueryRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath(); // /api/query/{queryId} o /api/query/{queryId}/status
        String[] segments = path.split("/");
        if (segments.length < 4) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }
        String queryId = segments[3];
        String action = segments.length >= 5 ? segments[4] : ""; // "status" o vacío

        if (action.equals("status")) {
            // GET /api/query/{queryId}/status
            handleStatusRequest(exchange, queryId);
        } else if (action.equals("") && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            // GET /api/query/{queryId}?page=X&size=Y
            handlePageRequest(exchange, queryId);
        } else {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        }
    }

    private void handleStatusRequest(HttpExchange exchange, String queryId) throws IOException {
        QueryResult qr = resultCache.get(queryId);
        if (qr == null) {
            sendError(exchange, 404, "Consulta no encontrada");
            return;
        }
        JSONObject response = new JSONObject();
        response.put("status", qr.status);
        response.put("total", qr.total);
        sendJson(exchange, 200, response.toString());
    }

    private void handlePageRequest(HttpExchange exchange, String queryId) throws IOException {
        QueryResult qr = resultCache.get(queryId);
        if (qr == null || !qr.status.equals("completed")) {
            sendError(exchange, 404, "Resultados no disponibles aún");
            return;
        }
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQueryParams(query);
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int size = Integer.parseInt(params.getOrDefault("size", "50"));

        List<JSONObject> allResults = qr.results;
        int total = allResults.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        if (fromIndex > total) {
            fromIndex = total;
            toIndex = total;
        }

        List<JSONObject> pageResults = allResults.subList(fromIndex, toIndex);
        JSONObject response = new JSONObject();
        response.put("total", total);
        response.put("page", page);
        response.put("size", size);
        response.put("results", new JSONArray(pageResults));
        sendJson(exchange, 200, response.toString());
    }

    // ========== Lógica de búsqueda (asíncrona) ==========

    private void executeSearch(String queryId, int n) {
        try {
            // 1. Obtener lista de libros del TextServer
            List<BookInfo> books = fetchBookList();
            if (books.size() < 2) {
                // No hay suficientes libros, terminar con cero resultados
                updateQuery(queryId, "completed", 0, Collections.emptyList());
                return;
            }

            // 2. Generar todas las parejas posibles
            List<Pair> pairs = new ArrayList<>();
            for (int i = 0; i < books.size(); i++) {
                for (int j = i + 1; j < books.size(); j++) {
                    pairs.add(new Pair(books.get(i), books.get(j)));
                }
            }

            // 3. Dividir en 3 sublistas (aproximadamente iguales)
            int chunkSize = (int) Math.ceil(pairs.size() / 3.0);
            List<List<Pair>> partitions = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, pairs.size());
                if (start > end) end = start;
                partitions.add(pairs.subList(start, end));
            }

            // 4. Enviar tareas en paralelo a los Workers
            List<CompletableFuture<List<JSONObject>>> futures = new ArrayList<>();
            for (int w = 0; w < 3 && w < partitions.size(); w++) {
                List<Pair> partition = partitions.get(w);
                if (partition.isEmpty()) continue;
                String workerUrl = workerUrls.get(w) + "/task";
                JSONObject taskPayload = new JSONObject();
                taskPayload.put("n", n);
                JSONArray jsonPairs = new JSONArray();
                for (Pair p : partition) {
                    JSONObject pairObj = new JSONObject();
                    pairObj.put("bookA", p.a.id);
                    pairObj.put("titleA", p.a.title);
                    pairObj.put("bookB", p.b.id);
                    pairObj.put("titleB", p.b.title);
                    jsonPairs.put(pairObj);
                }
                taskPayload.put("pairs", jsonPairs);

                CompletableFuture<List<JSONObject>> future = sendAsyncToWorker(workerUrl, taskPayload.toString());
                futures.add(future);
            }

            // 5. Esperar a que todos terminen y recolectar matches
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            List<JSONObject> allMatches = new ArrayList<>();
            for (CompletableFuture<List<JSONObject>> future : futures) {
                try {
                    allMatches.addAll(future.get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 6. Ordenar resultados (por títuloA, títuloB, offsetA)
            allMatches.sort(Comparator.comparing((JSONObject m) -> m.optString("titleA", ""))
                    .thenComparing(m -> m.optString("titleB", ""))
                    .thenComparingInt(m -> m.optInt("offsetA", 0)));

            // 7. Actualizar cache
            updateQuery(queryId, "completed", allMatches.size(), allMatches);

        } catch (Exception e) {
            e.printStackTrace();
            updateQuery(queryId, "completed", 0, Collections.emptyList()); // Falla -> 0 resultados
        }
    }

    // ------- Métodos auxiliares -------

    private List<BookInfo> fetchBookList() throws IOException, InterruptedException {
        String url = textServerUrl + "/books";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Error al obtener lista de libros: " + response.statusCode());
        }
        JSONArray jsonArray = new JSONArray(response.body());
        List<BookInfo> books = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            books.add(new BookInfo(obj.getString("id"), obj.getString("title")));
        }
        return books;
    }

    private CompletableFuture<List<JSONObject>> sendAsyncToWorker(String workerUrl, String jsonBody) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(workerUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        System.err.println("Error del worker: " + response.body());
                        return Collections.<JSONObject>emptyList();
                    }
                    JSONObject respJson = new JSONObject(response.body());
                    JSONArray matches = respJson.getJSONArray("matches");
                    List<JSONObject> list = new ArrayList<>();
                    for (int i = 0; i < matches.length(); i++) {
                        list.add(matches.getJSONObject(i));
                    }
                    return list;
                });
    }

    private void updateQuery(String queryId, String status, int total, List<JSONObject> results) {
        QueryResult qr = new QueryResult(status, total, results);
        resultCache.put(queryId, qr);
    }

    // ------- Utilidades HTTP -------

    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] respBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, respBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respBytes);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        JSONObject error = new JSONObject();
        error.put("error", message);
        sendJson(exchange, statusCode, error.toString());
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length >= 2) {
                params.put(kv[0], kv[1]);
            } else if (kv.length == 1) {
                params.put(kv[0], "");
            }
        }
        return params;
    }

    // ========== Clases internas para datos ==========

    static class BookInfo {
        final String id;
        final String title;
        BookInfo(String id, String title) { this.id = id; this.title = title; }
    }

    static class Pair {
        final BookInfo a, b;
        Pair(BookInfo a, BookInfo b) { this.a = a; this.b = b; }
    }

    static class QueryResult {
        String status;       // "processing" o "completed"
        int total;
        List<JSONObject> results;
        QueryResult(String status, int total, List<JSONObject> results) {
            this.status = status;
            this.total = total;
            this.results = results;
        }
    }
}