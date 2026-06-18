import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.Random;

public class CpuServer {

    static void consumeCpu(long busyMs) {
        Random ran = new Random();
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < busyMs) {
            Math.sqrt(ran.nextInt(2147483647));
        }
    }

    static class CpuTask implements Runnable {
        private final int percent;
        private final int seconds;

        CpuTask(int percent, int seconds) {
            this.percent = percent;
            this.seconds = seconds;
        }

        @Override
        public void run() {
            long totalIntervals = (long) seconds * 1000;
            long busyIntervals  = totalIntervals * percent / 100;
            long idleIntervals  = totalIntervals - busyIntervals;

            boolean[] schedule = new boolean[(int) totalIntervals];
            int idx = 0;
            for (int i = 0; i < busyIntervals; i++) schedule[idx++] = true;
            for (int i = 0; i < idleIntervals;  i++) schedule[idx++] = false;

            Random rng = new Random();
            for (int i = schedule.length - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                boolean tmp = schedule[i]; schedule[i] = schedule[j]; schedule[j] = tmp;
            }

            try {
                for (boolean busy : schedule) {
                    if (busy) consumeCpu(1);
                    else      Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class CpuHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String msg = "Solo se acepta POST";
                exchange.sendResponseHeaders(405, msg.length());
                exchange.getResponseBody().write(msg.getBytes());
                exchange.getResponseBody().close();
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());
            int percent = 65;
            int seconds = 10;
            int threads = 1;

            for (String pair : body.split("&")) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    switch (kv[0].trim()) {
                        case "seconds" -> seconds = Integer.parseInt(kv[1].trim());
                        case "threads" -> threads = Integer.parseInt(kv[1].trim());
                    }
                }
            }

            String respMsg = String.format(
                "Iniciando: %d%% CPU, %d segundo(s), %d hilo(s)\n",
                percent, seconds, threads);
            exchange.sendResponseHeaders(200, respMsg.length());
            exchange.getResponseBody().write(respMsg.getBytes());
            exchange.getResponseBody().close();

            final int s = seconds, t = threads;
            new Thread(() -> {
                Thread[] pool = new Thread[t];
                for (int i = 0; i < t; i++) {
                    pool[i] = new Thread(new CpuTask(percent, s));
                    pool[i].start();
                }
                for (Thread th : pool) {
                    try { th.join(); } catch (InterruptedException ignored) {}
                }
                System.out.println("Tarea finalizada.");
            }).start();
        }
    }

    public static void main(String[] args) throws IOException {
        int port = 80;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/cpu", new CpuHandler());
        server.start();
        System.out.println("Servidor escuchando en http://localhost:" + port + "/cpu");
    }
}