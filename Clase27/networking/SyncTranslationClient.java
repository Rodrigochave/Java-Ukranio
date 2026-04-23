package networking;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class SyncTranslationClient {

    public static void main(String[] args) throws IOException, InterruptedException {
        String textoOriginal = "People%20have%20the%20right%20to%20disagree%20with%20your%20opinions%20and%20to%20dissent.";
        String targetLang = "es";
        String apiKey = "AIzaSyCA507rNodnQ4spkRVGUUuk5C1r-Na4MYI";     

        String url = String.format(
            "https://translation.googleapis.com/language/translate/v2?target=%s&key=%s&q=%s",
            targetLang,
            apiKey,
            textoOriginal
        );
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))            // establecer la URL completa con parámetros
                .GET()                          // método GET (explícito)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Código de estado HTTP: " + response.statusCode());
        System.out.println("Respuesta JSON de Google Cloud Translation:");
        System.out.println(response.body());
    }
}