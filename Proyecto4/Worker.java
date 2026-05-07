//Número de proyecto: 4 Nombre:Chavez Aquiagual Rodrigo Grupo: 7CM4 
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

import org.json.JSONArray;
import org.json.JSONObject;

public class Worker {
    private static final String TASK_ENDPOINT = "/task";
    private static final String STATUS_ENDPOINT = "/status";
    private static final int MAX_CHARS = 500_000;   // estable y suficiente
    private static final int MAX_MATCHES = 100;     // límite por pareja

    private final int port;
    private final String textServerBase;
    private HttpServer server;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) {
        int port = 8080;
        String textServer = "http://localhost:8081";

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
                ? textServerBase.substring(0, textServerBase.length() - 1)
                : textServerBase;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            System.err.println("Error: puerto " + port + " ya está en uso");
            return;
        }

        server.createContext(TASK_ENDPOINT, this::handleTask);
        server.createContext(STATUS_ENDPOINT, this::handleStatus);

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
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

            // --- Cachés GLOBALES para TODA la tarea ---
            Map<String, String> textCache = new HashMap<>();
            Map<String, Map<String, List<Integer>>> ngramCache = new HashMap<>();

            JSONArray allMatches = new JSONArray();

            // Procesar secuencialmente, reutilizando libros ya descargados y cacheados
            for (int i = 0; i < pairsArray.length(); i++) {
                JSONObject pairObj = pairsArray.getJSONObject(i);

                String bookA = pairObj.getString("bookA");
                String bookB = pairObj.getString("bookB");
                String titleA = pairObj.optString("titleA", "Sin título");
                String titleB = pairObj.optString("titleB", "Sin título");

                try {
                    List<JSONObject> matches = processPair(bookA, bookB, titleA, titleB, n,
                            textCache, ngramCache);
                    allMatches.putAll(matches);
                } catch (Exception e) {
                    System.err.println("Error en par " + bookA + "/" + bookB + ": " + e.getMessage());
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

        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

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

    // ========== PROCESAMIENTO (cache global, sin bloques) ==========

    private List<JSONObject> processPair(String bookAId, String bookBId,
                                         String titleA, String titleB, int n,
                                         Map<String, String> textCache,
                                         Map<String, Map<String, List<Integer>>> ngramCache)
            throws Exception {
        // Obtener o construir mapa de n-gramas para libro A (cache global)
        Map<String, List<Integer>> mapA = ngramCache.computeIfAbsent(bookAId, id -> {
            String text = textCache.computeIfAbsent(id, bid -> {
                try { return getBookText(bid); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
            return buildNgramMap(tokenize(text), n);
        });

        // Obtener o construir mapa de n-gramas para libro B (cache global)
        Map<String, List<Integer>> mapB = ngramCache.computeIfAbsent(bookBId, id -> {
            String text = textCache.computeIfAbsent(id, bid -> {
                try { return getBookText(bid); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
            return buildNgramMap(tokenize(text), n);
        });

        List<JSONObject> matches = new ArrayList<>();

        outer:
        for (Map.Entry<String, List<Integer>> entryA : mapA.entrySet()) {
            String phrase = entryA.getKey();
            if (mapB.containsKey(phrase)) {
                for (int offA : entryA.getValue()) {
                    for (int offB : mapB.get(phrase)) {

                        JSONObject match = new JSONObject();
                        match.put("titleA", titleA);
                        match.put("offsetA", offA);
                        match.put("titleB", titleB);
                        match.put("offsetB", offB);
                        match.put("phrase", phrase);

                        matches.add(match);
                        if (matches.size() >= MAX_MATCHES) {
                            break outer;
                        }
                    }
                }
            }
        }

        return matches;
    }

    // ========== TEXT SERVER (con truncado) ==========

    private String getBookText(String bookId) throws IOException, InterruptedException {
        String url = textServerBase + "/books/" + bookId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0")          // por si acaso
                .timeout(java.time.Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Error al obtener libro " + bookId + ": " + response.statusCode());
        }

        String fullText = response.body();
        if (fullText.length() > MAX_CHARS) {
            System.err.println("Truncando libro " + bookId + " a " + MAX_CHARS + " caracteres");
            return fullText.substring(0, MAX_CHARS);
        }
        return fullText;
    }

    // ========== TOKENIZACIÓN ==========

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

    private static class Token {
        final String word;
        final int offset;

        Token(String word, int offset) {
            this.word = word;
            this.offset = offset;
        }
    }
}