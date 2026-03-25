package com.mkyong.java11.jep321;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpClientSynchronous {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) throws IOException, InterruptedException {

        // Números a multiplicar (se pueden cambiar o pedir por teclado)
        String numbers = "2,3";

        // Construir petición POST al endpoint /task
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(numbers))
                .uri(URI.create("http://localhost:8080/task"))
                .setHeader("User-Agent", "Java 11 HttpClient Bot")
                .setHeader("X-Debug", "true")   // Solicitar tiempo de procesamiento
                .setHeader("Content-Type", "text/plain") // Tipo de contenido
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Mostrar cabeceras de la respuesta
        HttpHeaders headers = response.headers();
        System.out.println("--- CABECERAS DE RESPUESTA ---");
        headers.map().forEach((k, v) -> System.out.println(k + ":" + v));

        // Mostrar código de estado
        System.out.println("\nCódigo de estado: " + response.statusCode());

        // Mostrar cuerpo de la respuesta (resultado)
        System.out.println("\n--- CUERPO DE RESPUESTA ---");
        System.out.println(response.body());

        // Extraer y mostrar el tiempo de procesamiento (si existe)
        if (headers.firstValue("X-Debug-Info").isPresent()) {
            System.out.println("\n--- TIEMPO DE PROCESAMIENTO (desde cabecera) ---");
            System.out.println(headers.firstValue("X-Debug-Info").get());
        }
    }
}