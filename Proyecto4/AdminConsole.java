import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class AdminConsole {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private final String textServerUrl;
    private final Path localDir;
    private Screen screen;

    public AdminConsole(String textServerUrl, Path localDir) {
        this.textServerUrl = textServerUrl.endsWith("/") 
                ? textServerUrl.substring(0, textServerUrl.length() - 1) 
                : textServerUrl;
        this.localDir = localDir;
        if (!Files.exists(localDir)) {
            try {
                Files.createDirectories(localDir);
            } catch (IOException ignored) {}
        }
    }

    public static void main(String[] args) {
        String url = "http://localhost:8081";
        String dir = "./libros";
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--text-server": url = args[++i]; break;
                case "--dir": dir = args[++i]; break;
                default:
                    System.out.println("Uso: AdminConsole --text-server <URL> --dir <directorio>");
                    return;
            }
        }
        new AdminConsole(url, Paths.get(dir)).start();
    }

    public void start() {
        try {
            DefaultTerminalFactory factory = new DefaultTerminalFactory();
            Terminal terminal = factory.createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();
            mainLoop();
            screen.stopScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mainLoop() throws IOException {
        while (true) {
            drawMenu();
            char choice = readChar();
            switch (choice) {
                case '1': listRemoteBooks(); break;
                case '2': selectAndUpload(); break;
                case '3': uploadAll(); break;
                case '4': deleteOneBook(); break;
                case '5': deleteAllBooks(); break;
                case '6': return;
                default:
                    showMessage("Opción no válida", TextColor.ANSI.RED);
            }
        }
    }

    private void drawMenu() throws IOException {
        screen.clear();
        com.googlecode.lanterna.graphics.TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.putString(1, 1, "=== CONSOLA DE ADMINISTRACIÓN DE TEXTOS ===");
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.putString(1, 3, "1. Visualizar archivos en el servidor");
        tg.putString(1, 4, "2. Seleccionar y subir un archivo local");
        tg.putString(1, 5, "3. Subir todos los archivos locales");
        tg.putString(1, 6, "4. Eliminar un archivo del servidor");
        tg.putString(1, 7, "5. Eliminar todos los archivos del servidor");
        tg.putString(1, 8, "6. Salir");
        tg.setForegroundColor(TextColor.ANSI.YELLOW);
        tg.putString(1, 10, "Elige una opción (1-6): ");
        screen.refresh();
    }

    private char readChar() throws IOException {
        while (true) {
            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.Character) {
                return key.getCharacter();
            }
        }
    }

    private void waitForKey() throws IOException {
        screen.readInput();
    }

    private void showMessage(String msg, TextColor color) throws IOException {
        com.googlecode.lanterna.graphics.TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(color);
        // Limpiar líneas inferiores
        for (int i = 12; i < 20; i++) {
            tg.putString(1, i, " ".repeat(80));
        }
        tg.putString(1, 12, msg);
        tg.putString(1, 13, "Presiona cualquier tecla para continuar...");
        screen.refresh();
        waitForKey();
    }

    // ──────────── Operaciones con el servidor ────────────

    private void listRemoteBooks() {
        try {
            JSONArray books = getJSON("/books");
            if (books.isEmpty()) {
                showMessage("No hay libros en el servidor.", TextColor.ANSI.YELLOW);
            } else {
                StringBuilder sb = new StringBuilder("Libros en servidor:\n");
                for (int i = 0; i < books.length(); i++) {
                    JSONObject b = books.getJSONObject(i);
                    sb.append(String.format("%s [%s]\n", b.getString("title"), b.getString("id")));
                }
                showMessage(sb.toString(), TextColor.ANSI.GREEN);
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void selectAndUpload() {
        try {
            List<Path> localFiles = Files.list(localDir)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .collect(Collectors.toList());
            if (localFiles.isEmpty()) {
                showMessage("No hay archivos .txt en " + localDir, TextColor.ANSI.YELLOW);
                return;
            }
            StringBuilder list = new StringBuilder("Archivos disponibles:\n");
            for (int i = 0; i < localFiles.size(); i++) {
                list.append(i + 1).append(". ").append(localFiles.get(i).getFileName()).append("\n");
            }
            list.append("Elige un número (0 para cancelar): ");
            showMessage(list.toString(), TextColor.ANSI.WHITE);
            int choice = readNumber();
            if (choice <= 0 || choice > localFiles.size()) return;
            Path chosen = localFiles.get(choice - 1);
            String title = chosen.getFileName().toString().replace(".txt", "");
            byte[] content = Files.readAllBytes(chosen);
            uploadBook(title, content);
            showMessage("Libro \"" + title + "\" subido correctamente.", TextColor.ANSI.GREEN);
        } catch (Exception e) {
            showError(e);
        }
    }

    private void uploadAll() {
        try {
            List<Path> localFiles = Files.list(localDir)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .collect(Collectors.toList());
            if (localFiles.isEmpty()) {
                showMessage("No hay archivos .txt en " + localDir, TextColor.ANSI.YELLOW);
                return;
            }
            int uploaded = 0;
            for (Path f : localFiles) {
                String title = f.getFileName().toString().replace(".txt", "");
                byte[] content = Files.readAllBytes(f);
                try {
                    uploadBook(title, content);
                    uploaded++;
                } catch (Exception e) {
                    // Ignorar duplicados (409) u otros errores
                }
            }
            showMessage("Subidos " + uploaded + " de " + localFiles.size() + " archivos.", TextColor.ANSI.GREEN);
        } catch (Exception e) {
            showError(e);
        }
    }

    private void uploadBook(String title, byte[] content) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(textServerUrl + "/books/upload"))
                .header("X-Title", title)
                .POST(HttpRequest.BodyPublishers.ofByteArray(content))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException("Error " + response.statusCode() + ": " + response.body());
        }
    }

    private void deleteOneBook() {
        try {
            JSONArray books = getJSON("/books");
            if (books.isEmpty()) {
                showMessage("No hay libros para eliminar.", TextColor.ANSI.YELLOW);
                return;
            }
            StringBuilder list = new StringBuilder("Libros en servidor:\n");
            for (int i = 0; i < books.length(); i++) {
                JSONObject b = books.getJSONObject(i);
                list.append(i + 1).append(". ").append(b.getString("title")).append(" [").append(b.getString("id")).append("]\n");
            }
            list.append("Elige un número (0 para cancelar): ");
            showMessage(list.toString(), TextColor.ANSI.WHITE);
            int choice = readNumber();
            if (choice <= 0 || choice > books.length()) return;
            String id = books.getJSONObject(choice - 1).getString("id");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(textServerUrl + "/books/" + id))
                    .DELETE()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204) {
                showMessage("Libro eliminado correctamente.", TextColor.ANSI.GREEN);
            } else {
                showMessage("Error al eliminar: " + response.body(), TextColor.ANSI.RED);
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void deleteAllBooks() {
        try {
            showMessage("¿Seguro que deseas eliminar TODOS los libros? (s/n): ", TextColor.ANSI.RED);
            char confirm = readChar();
            if (confirm != 's' && confirm != 'S') return;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(textServerUrl + "/books"))
                    .DELETE()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204) {
                showMessage("Todos los libros eliminados.", TextColor.ANSI.GREEN);
            } else {
                showMessage("Error: " + response.body(), TextColor.ANSI.RED);
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    // ──────────── Métodos auxiliares ────────────

    private JSONArray getJSON(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(textServerUrl + path))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return new JSONArray(response.body());
        } else {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    private int readNumber() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.Enter) break;
            if (key.getKeyType() == KeyType.Character) {
                char c = key.getCharacter();
                if (Character.isDigit(c)) {
                    sb.append(c);
                }
            }
        }
        return sb.length() == 0 ? 0 : Integer.parseInt(sb.toString());
    }

    private void showError(Exception e) {
        try {
            showMessage("Error: " + e.getMessage(), TextColor.ANSI.RED);
        } catch (IOException ignored) {}
    }
}