import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Random;

public class ClienteAlumnos {
    private static final String SERVER_URL = "http://localhost:8080/alumnos";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {
        // Leer archivos igual que CombinacionesArchivos
        ArrayList<String> nombres = leerArchivo("nombres.txt");
        ArrayList<String> apellidos = leerArchivo("apellidos.txt");
        Random random = new Random(12345); // Semilla fija

        for (String nombre : nombres) {
            for (String ap1 : apellidos) {
                for (String ap2 : apellidos) {
                    double promedio = random.nextDouble() * 100;
                    promedio = Math.round(promedio * 10) / 100.0;

                    // Construir JSON con String.format
                    String json = String.format(
                        "{\"nombre\":\"%s\",\"apellido1\":\"%s\",\"apellido2\":\"%s\",\"promedio\":%.2f}",
                        nombre, ap1, ap2, promedio
                    );

                    enviarPost(json);
                }
            }
        }
    }

    private static void enviarPost(String json) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Enviado: " + json);
            System.out.println("Código respuesta: " + response.statusCode());
            System.out.println("Cuerpo: " + response.body());
            System.out.println("---");
        } catch (IOException | InterruptedException e) {
            System.err.println("Error en POST: " + e.getMessage());
        }
    }

    private static ArrayList<String> leerArchivo(String nombreArchivo) {
        ArrayList<String> lista = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(nombreArchivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (!linea.isEmpty()) {
                    lista.add(linea);
                }
            }
        } catch (IOException e) {
            System.out.println("Error leyendo " + nombreArchivo);
            e.printStackTrace();
        }
        return lista;
    }
}