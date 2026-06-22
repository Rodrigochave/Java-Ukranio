//Número de proyecto: 4 Nombre:Chavez Aquiagual Rodrigo Grupo: 7CM4 
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.graphics.TextGraphics;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
        TextGraphics tg = screen.newTextGraphics();
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

    // ──────────── Utilidades de entrada / salida ────────────

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

    /**
     * Muestra un mensaje (varias líneas) y espera una tecla para continuar.
     */
    private void showMessage(String msg, TextColor color) throws IOException {
        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(color);
        // Limpiar líneas 12-24
        for (int i = 12; i < 25; i++) {
            tg.putString(1, i, " ".repeat(80));
        }
        String[] lines = msg.split("\n");
        int row = 12;
        for (String line : lines) {
            if (row > 24) break;
            tg.putString(1, row, line);
            row++;
        }
        tg.putString(1, row, "Presiona cualquier tecla para continuar...");
        screen.refresh();
        waitForKey();
    }

    /**
     * Muestra un mensaje paginado (similar a showPagedMessage) pero retorna el char leído.
     */
    private char showPagedMessage(String msg, TextColor color) throws IOException {
        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(color);
        for (int i = 12; i < 25; i++) {
            tg.putString(1, i, " ".repeat(80));
        }
        String[] lines = msg.split("\n");
        int row = 12;
        for (String line : lines) {
            if (row > 24) break;
            tg.putString(1, row, line);
            row++;
        }
        screen.refresh();
        KeyStroke key = screen.readInput();
        if (key.getKeyType() == KeyType.Character) {
            return key.getCharacter();
        }
        return 0;
    }

    /**
     * Lee un número entero positivo mostrando los dígitos en pantalla y finalizando con Enter.
     * Muestra el prompt en la línea 12 (limpia antes).
     */
    private int readNumberInteractive(String prompt) throws IOException {
        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        // Limpiar línea 12
        tg.putString(1, 12, " ".repeat(80));
        tg.putString(1, 12, prompt);
        screen.refresh();

        StringBuilder sb = new StringBuilder();
        while (true) {
            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.Enter) {
                break;
            }
            if (key.getKeyType() == KeyType.Character) {
                char c = key.getCharacter();
                if (Character.isDigit(c)) {
                    sb.append(c);
                    // Mostrar el número actualizado
                    tg.putString(1, 12, prompt + sb.toString());
                    screen.refresh();
                } else if (c == 8 || c == 127) { // backspace
                    if (sb.length() > 0) {
                        sb.deleteCharAt(sb.length() - 1);
                        tg.putString(1, 12, prompt + sb.toString() + " ");
                        screen.refresh();
                    }
                }
            }
        }
        if (sb.length() == 0) return 0;
        return Integer.parseInt(sb.toString());
    }

    // ──────────── Lógica de paginación para listas ────────────

    /**
     * Muestra una lista de strings en páginas y permite seleccionar un índice (1-based).
     * Retorna el índice seleccionado, o 0 si se cancela (Enter sin número o 'q').
     */
    private int selectFromList(List<String> items, String title, TextColor color) throws IOException {
        int pageSize = 8;
        int total = items.size();
        int pages = (int) Math.ceil((double) total / pageSize);

        for (int p = 0; p < pages; p++) {
            int start = p * pageSize;
            int end = Math.min(start + pageSize, total);

            StringBuilder sb = new StringBuilder();
            sb.append(title).append(" (").append(p + 1).append("/").append(pages).append("):\n");
            for (int i = start; i < end; i++) {
                sb.append(i + 1).append(". ").append(items.get(i)).append("\n");
            }

            String prompt;
            if (p < pages - 1) {
                prompt = "\nEnter = elegir número | cualquier tecla = siguiente página | q = salir";
            } else {
                prompt = "\nEnter = elegir número | cualquier tecla = volver al inicio | q = salir";
            }
            sb.append(prompt);

            char response = showPagedMessage(sb.toString(), color);
            if (response == 'q' || response == 'Q') {
                return 0;
            }
            if (response == 0) { // Enter o tecla especial
                // Mostrar cuadro de entrada numérica
                int choice = readNumberInteractive("Ingresa el número (0 para cancelar): ");
                if (choice < 1 || choice > total) {
                    return 0;
                }
                return choice;
            }
            // cualquier otra tecla: siguiente página (o volver al inicio si es la última)
        }
        return 0;
    }

    // ──────────── Operaciones del menú ────────────

    private void listRemoteBooks() {
        try {
            JSONArray books = getJSON("/books");
            if (books.isEmpty()) {
                showMessage("No hay libros en el servidor.", TextColor.ANSI.YELLOW);
                return;
            }

            List<String> bookNames = new ArrayList<>();
            for (int i = 0; i < books.length(); i++) {
                JSONObject b = books.getJSONObject(i);
                bookNames.add(b.getString("title") + " [" + b.getString("id") + "]");
            }
            // Solo visualización, no selección
            int pageSize = 10;
            int total = bookNames.size();
            int pages = (int) Math.ceil((double) total / pageSize);
            for (int p = 0; p < pages; p++) {
                int start = p * pageSize;
                int end = Math.min(start + pageSize, total);
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Libros en servidor (%d/%d):\n", p + 1, pages));
                for (int i = start; i < end; i++) {
                    sb.append(bookNames.get(i)).append("\n");
                }
                String prompt = (p < pages - 1) ? "\nCualquier tecla = siguiente página | q = salir" : "\nCualquier tecla = volver al menú";
                sb.append(prompt);
                char resp = showPagedMessage(sb.toString(), TextColor.ANSI.GREEN);
                if (resp == 'q' || resp == 'Q') break;
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

            List<String> fileNames = localFiles.stream()
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());

            int choice = selectFromList(fileNames, "Archivos locales", TextColor.ANSI.WHITE);
            if (choice <= 0) return;

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
            // Confirmación
            showMessage("Se subirán " + localFiles.size() + " archivos. ¿Continuar? (s/n): ", TextColor.ANSI.YELLOW);
            char confirm = readChar();
            if (confirm != 's' && confirm != 'S') return;

            int uploaded = 0;
            for (Path f : localFiles) {
                String title = f.getFileName().toString().replace(".txt", "");
                byte[] content = Files.readAllBytes(f);
                try {
                    uploadBook(title, content);
                    uploaded++;
                } catch (Exception e) {
                    // Ignorar duplicados
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

            List<String> bookEntries = new ArrayList<>();
            for (int i = 0; i < books.length(); i++) {
                JSONObject b = books.getJSONObject(i);
                bookEntries.add(b.getString("title") + " [" + b.getString("id") + "]");
            }

            int choice = selectFromList(bookEntries, "Libros en servidor", TextColor.ANSI.WHITE);
            if (choice <= 0) return;

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

    private void showError(Exception e) {
        try {
            showMessage("Error: " + e.getMessage(), TextColor.ANSI.RED);
        } catch (IOException ignored) {}
    }
}