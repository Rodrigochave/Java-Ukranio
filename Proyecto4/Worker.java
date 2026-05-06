import com.sun.management.OperatingSystemMXBean;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Worker que procesa pares de libros para encontrar frases comunes de n palabras.
 */
public class Worker {
    private static final String TASK_ENDPOINT = "/task";
    private static final String STATUS_ENDPOINT = "/status";

    private final int port;
    private final String textServerBase;
    private HttpServer server;

    // Pool de hilos para procesamiento interno de tareas
    private final ExecutorService processingPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    // Cliente HTTP para obtener textos del TextServer
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) {
        int port = 8080;
        String textServer = "http://localhost:8081"; // valor por defecto

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":
                    port = Integer.parseInt(args[++i]);
                    break;
                case "--text-server":
                    textServer = args[++i];
                    break;
                default:
                    System.err.println("Uso: Worker --port <puerto> --text-server <URL>");
                    return;
            }
        }

        Worker worker = new Worker(port, textServer);
        worker.start();
        System.out.println("Worker escuchando en puerto " + port);
        System.out.println("Servidor de textos: " + textServer);
    }

    public Worker(int port, String textServerBase) {
        this.port = port;
        this.textServerBase = textServerBase.endsWith("/") 
                ? textServerBase.substring(0, textServerBase.length()-1) 
                : textServerBase;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        server.createContext(TASK_ENDPOINT, this::handleTask);
        server.createContext(STATUS_ENDPOINT, this::handleStatus);

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
    }

    // ========== HANDLERS ==========

    private void handleTask(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        try {
            byte[] body = exchange.getRequestBody().readAllBytes();
            String jsonInput = new String(body, StandardCharsets.UTF_8);
            JSONObject input = new JSONObject(jsonInput);

            JSONArray pairsArray = input.getJSONArray("pairs");
            int n = input.getInt("n");

            List<Future<List<JSONObject>>> futures = new ArrayList<>();

            for (int i = 0; i < pairsArray.length(); i++) {
                JSONObject pairObj = pairsArray.getJSONObject(i);
                String bookA = pairObj.getString("bookA");
                String bookB = pairObj.getString("bookB");
                // Leer títulos directamente del JSON (evita llamada extra al TextServer)
                String titleA = pairObj.optString("titleA", "Sin título");
                String titleB = pairObj.optString("titleB", "Sin título");

                futures.add(processingPool.submit(() -> processPair(bookA, bookB, titleA, titleB, n)));
            }

            // Recoger resultados
            JSONArray allMatches = new JSONArray();
            for (Future<List<JSONObject>> future : futures) {
                try {
                    List<JSONObject> pairMatches = future.get();
                    allMatches.putAll(pairMatches);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            JSONObject response = new JSONObject();
            response.put("matches", allMatches);
            byte[] respBytes = response.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, respBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(respBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String err = "{\"error\":\"" + e.getMessage() + "\"}";
            byte[] errBytes = err.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, errBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errBytes);
            }
        }
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = osBean.getProcessCpuLoad();
        if (cpuLoad < 0) cpuLoad = 0.0;

        JSONObject status = new JSONObject();
        status.put("status", "OK");
        status.put("cpu", Math.round(cpuLoad * 1000.0) / 1000.0);

        byte[] respBytes = status.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, respBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respBytes);
        }
    }

    // ========== PROCESAMIENTO DE UN PAR ==========

    private List<JSONObject> processPair(String bookAId, String bookBId, 
                                         String titleA, String titleB, int n) throws Exception {
        // Obtener textos desde el TextServer
        String textA = getBookText(bookAId);
        String textB = getBookText(bookBId);

        List<Token> tokensA = tokenize(textA);
        List<Token> tokensB = tokenize(textB);

        Map<String, List<Integer>> mapA = buildNgramMap(tokensA, n);
        Map<String, List<Integer>> mapB = buildNgramMap(tokensB, n);

        List<JSONObject> matches = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entryA : mapA.entrySet()) {
            String phrase = entryA.getKey();
            if (mapB.containsKey(phrase)) {
                List<Integer> offsetsA = entryA.getValue();
                List<Integer> offsetsB = mapB.get(phrase);
                for (int offA : offsetsA) {
                    for (int offB : offsetsB) {
                        JSONObject match = new JSONObject();
                        match.put("titleA", titleA);
                        match.put("offsetA", offA);
                        match.put("titleB", titleB);
                        match.put("offsetB", offB);
                        match.put("phrase", phrase);
                        matches.add(match);
                    }
                }
            }
        }
        return matches;
    }

    // ========== COMUNICACIÓN CON TEXT SERVER ==========

    /**
     * Descarga el texto completo de un libro desde el TextServer.
     */
    private String getBookText(String bookId) throws IOException, InterruptedException {
        String url = textServerBase + "/books/" + bookId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Error al obtener libro " + bookId + ": " + response.statusCode());
        }
        return response.body();
    }

    // ========== TOKENIZACIÓN Y N-GRAMAS ==========

    private List<Token> tokenize(String text) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        StringBuilder currentWord = new StringBuilder();
        Integer wordStart = null;

        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                if (currentWord.length() == 0) {
                    wordStart = pos;
                }
                currentWord.append(c);
            } else {
                if (currentWord.length() > 0) {
                    tokens.add(new Token(currentWord.toString().toLowerCase(), wordStart));
                    currentWord.setLength(0);
                    wordStart = null;
                }
            }
            pos++;
        }
        if (currentWord.length() > 0) {
            tokens.add(new Token(currentWord.toString().toLowerCase(), wordStart));
        }
        return tokens;
    }

    private Map<String, List<Integer>> buildNgramMap(List<Token> tokens, int n) {
        Map<String, List<Integer>> map = new HashMap<>();
        if (tokens.size() < n) return map;

        for (int i = 0; i <= tokens.size() - n; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if (j > 0) sb.append(" ");
                sb.append(tokens.get(i + j).word);
            }
            String phrase = sb.toString();
            int offset = tokens.get(i).offset;
            map.computeIfAbsent(phrase, k -> new ArrayList<>()).add(offset);
        }
        return map;
    }

    // ========== CLASE INTERNA ==========

    private static class Token {
        final String word;
        final int offset;

        Token(String word, int offset) {
            this.word = word;
            this.offset = offset;
        }
    }
}