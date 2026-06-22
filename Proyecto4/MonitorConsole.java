//Número de proyecto: 4 Nombre:Chavez Aquiagual Rodrigo Grupo: 7CM4 
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
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
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class MonitorConsole {
    private final List<String> workerUrls;
    private Screen screen;
    private volatile boolean running = true;
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public MonitorConsole(List<String> workerUrls) {
        this.workerUrls = workerUrls;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Uso: MonitorConsole <url1> <url2> <url3> ...");
            System.out.println("   o: MonitorConsole --workers <url1,url2,...>");
            return;
        }
        List<String> urls;
        if (args[0].equals("--workers")) {
            urls = Arrays.asList(args[1].split(","));
        } else {
            urls = Arrays.asList(args);
        }
        new MonitorConsole(urls).start();
    }

    private void start() {
        try {
            DefaultTerminalFactory factory = new DefaultTerminalFactory();
            factory.setForceTextTerminal(true);   // compatibilidad máxima
            Terminal terminal = factory.createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();

            Thread inputThread = new Thread(() -> {
                try {
                    while (running) {
                        KeyStroke key = screen.readInput();
                        if (key != null && key.getKeyType() == KeyType.Escape) {
                            running = false;
                            break;
                        }
                    }
                } catch (IOException ignored) {}
            });
            inputThread.setDaemon(true);
            inputThread.start();

            long lastUpdate = 0;
            while (running) {
                // Actualizar cada 2 segundos
                if (System.currentTimeMillis() - lastUpdate > 2000) {
                    updateScreen();
                    lastUpdate = System.currentTimeMillis();
                }
                Thread.sleep(500); // pequeña pausa para no saturar la CPU
            }
            screen.stopScreen();
        } catch (Exception e) {
            System.err.println("Error iniciando monitor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateScreen() {
        try {
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            tg.putString(1, 0, "=== MONITOR DE WORKERS (CPU en tiempo real) ===");
            tg.putString(1, 1, "Worker                    CPU  Gráfico");
            int row = 2;
            for (String url : workerUrls) {
                double cpu = 0.0;
                boolean alive = false;
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(url + "/status"))
                            .timeout(Duration.ofSeconds(2))
                            .GET()
                            .build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() == 200) {
                        org.json.JSONObject json = new org.json.JSONObject(resp.body());
                        cpu = json.optDouble("cpu", 0.0);
                        alive = true;
                    }
                } catch (Exception e) {
                    // Worker no responde, se marcará OFFLINE
                }

                String displayUrl = url.length() > 30 ? url.substring(7, 32) : url;
                tg.setForegroundColor(TextColor.ANSI.WHITE);
                tg.putString(1, row, String.format("%-25s %3.0f%% ", displayUrl, cpu * 100));

                int barLen = (int) (cpu * 20);
                StringBuilder bar = new StringBuilder("[");
                for (int i = 0; i < 20; i++) {
                    bar.append(i < barLen ? "#" : " ");
                }
                bar.append("]");
                tg.setForegroundColor(cpu > 0.7 ? TextColor.ANSI.RED : (cpu > 0.3 ? TextColor.ANSI.YELLOW : TextColor.ANSI.GREEN));
                tg.putString(55, row, bar.toString());

                if (!alive) {
                    tg.setForegroundColor(TextColor.ANSI.RED);
                    tg.putString(55 + bar.length() + 2, row, "OFFLINE");
                }
                row++;
            }
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            tg.putString(1, row + 1, "Presiona ESC para salir.");
            screen.refresh();
        } catch (IOException e) {
            System.err.println("Error al actualizar pantalla: " + e.getMessage());
        }
    }
}