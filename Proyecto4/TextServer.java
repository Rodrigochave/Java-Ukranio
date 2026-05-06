import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Headers;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;  // asumiendo que usas org.json (simple, sin dependencias extra) o puedes escribir JSON a mano

public class TextServer {
    private static final String BOOKS_DIR = "data/books";
    private static final String METADATA_FILE = "data/metadata.json";

    private final int port;
    private HttpServer server;

    // Mapa en memoria: id -> título (se carga y se persiste)
    private final Map<String, String> metadata = new HashMap<>();

    public static void main(String[] args) {
        int port = 8081;
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        TextServer textServer = new TextServer(port);
        textServer.start();
        System.out.println("Servidor de textos escuchando en puerto " + port);
    }

    public TextServer(int port) {
        this.port = port;
        // Crear directorios si no existen
        try {
            Files.createDirectories(Paths.get(BOOKS_DIR));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de libros", e);
        }
        loadMetadata();
    }

    private void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Contexto para /books (GET lista y DELETE todos)
        server.createContext("/books", this::handleBooks);
        // Contexto para /books/upload (POST)
        server.createContext("/books/upload", this::handleUpload);
        // Contexto para /books/{id} (GET y DELETE individual)
        server.createContext("/books/", this::handleSingleBook);

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
    }

    // -----------------------------------------------
    // Manejadores
    // -----------------------------------------------

    private void handleBooks(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        if ("GET".equals(method)) {
            // Retornar lista de libros
            JSONArray list = new JSONArray();
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                JSONObject obj = new JSONObject();
                obj.put("id", entry.getKey());
                obj.put("title", entry.getValue());
                list.put(obj);
            }
            sendJsonResponse(exchange, 200, list.toString());
        } else if ("DELETE".equals(method)) {
            // Borrar todos los libros
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(BOOKS_DIR))) {
                for (Path file : stream) {
                    Files.delete(file);
                }
            }
            metadata.clear();
            persistMetadata();
            exchange.sendResponseHeaders(204, -1); // No content
            exchange.close();
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            exchange.close();
        }
    }

    private void handleUpload(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        if (!"POST".equals(method)) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        // Obtener título de la cabecera X-Title
        Headers headers = exchange.getRequestHeaders();
        if (!headers.containsKey("X-Title")) {
            sendTextResponse(exchange, 400, "Falta la cabecera X-Title");
            return;
        }
        String title = headers.getFirst("X-Title").trim();
        if (title.isEmpty()) {
            sendTextResponse(exchange, 400, "Título vacío");
            return;
        }

        // Verificar si el título ya existe
        if (metadata.containsValue(title)) {
            sendTextResponse(exchange, 409, "Ya existe un libro con ese título");
            return;
        }

        // Leer el cuerpo
        byte[] body = exchange.getRequestBody().readAllBytes();
        String content = new String(body, "UTF-8");

        // Generar un id único
        String id = UUID.randomUUID().toString().substring(0, 8); // corto pero suficiente
        // Guardar archivo
        Path filePath = Paths.get(BOOKS_DIR, id + ".txt");
        Files.write(filePath, content.getBytes("UTF-8"));

        // Registrar en metadatos
        metadata.put(id, title);
        persistMetadata();

        // Respuesta con id y título
        JSONObject resp = new JSONObject();
        resp.put("id", id);
        resp.put("title", title);
        sendJsonResponse(exchange, 201, resp.toString());
    }

    private void handleSingleBook(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath(); // /books/123
        String[] parts = path.split("/");
        if (parts.length < 3) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }
        String id = parts[2]; // el id después de /books/

        if ("GET".equals(method)) {
            // Devolver contenido del libro
            Path filePath = Paths.get(BOOKS_DIR, id + ".txt");
            if (!Files.exists(filePath)) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            String content = Files.readString(filePath);
            sendTextResponse(exchange, 200, content);
        } else if ("DELETE".equals(method)) {
            // Eliminar un libro específico
            Path filePath = Paths.get(BOOKS_DIR, id + ".txt");
            if (!Files.exists(filePath) || !metadata.containsKey(id)) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            Files.delete(filePath);
            metadata.remove(id);
            persistMetadata();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        } else {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
        }
    }

    // -----------------------------------------------
    // Utilidades de respuesta
    // -----------------------------------------------

    private void sendTextResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] resp = body.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(code, resp.length);
        OutputStream os = exchange.getResponseBody();
        os.write(resp);
        os.close();
        exchange.close();
    }

    private void sendJsonResponse(HttpExchange exchange, int code, String json) throws IOException {
        byte[] resp = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, resp.length);
        OutputStream os = exchange.getResponseBody();
        os.write(resp);
        os.close();
        exchange.close();
    }

    // -----------------------------------------------
    // Persistencia de metadatos
    // -----------------------------------------------

    private void loadMetadata() {
        Path metaPath = Paths.get(METADATA_FILE);
        if (Files.exists(metaPath)) {
            try {
                String json = Files.readString(metaPath);
                JSONObject obj = new JSONObject(json); // formato: { id: title, ... }
                for (String id : obj.keySet()) {
                    metadata.put(id, obj.getString(id));
                }
            } catch (IOException e) {
                System.err.println("Error cargando metadatos, se iniciará vacío");
            }
        }
    }

    private void persistMetadata() {
        JSONObject obj = new JSONObject();
        metadata.forEach(obj::put);
        try {
            Files.write(Paths.get(METADATA_FILE), obj.toString(2).getBytes("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}