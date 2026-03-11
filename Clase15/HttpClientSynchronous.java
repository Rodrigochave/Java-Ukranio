package com.mkyong.java11.jep321;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;      // <-- Nuevo import
import java.util.Arrays;       // <-- Nuevo import

public class HttpClientSynchronous {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://httpbin.org/get"))
                .setHeader("User-Agent", "Java 11 HttpClient Bot")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        HttpHeaders headers = response.headers();
        Map<String, List<String>> headersMap = headers.map();
        Iterator<Map.Entry<String, List<String>>> iterator = headersMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<String>> entry = iterator.next();
            String key = entry.getKey();
            List<String> values = entry.getValue();
            for (String value : values) {
                System.out.println(key + ": " + value);
            }
        }

        // Copiar el mapa original a un nuevo mapa mutable
        Map<String, List<String>> newHeadersMap = new HashMap<>(headersMap);

        // Agregar los headers Set-Cookie
        List<String> setCookieValues = Arrays.asList("Max-Age=0", "id=123", "theme=dark");

        newHeadersMap.put("Set-Cookie", setCookieValues);

        // Imprimir el nuevo mapa usando forEach
        System.out.println("\n--- Nuevo mapa con Set-Cookie añadidos ---");
        newHeadersMap.forEach((key, values) -> System.out.println(key + ": " + values));

        // Imprimir el mismo mapa pero ordenado alfabéticamente por clave usando Stream
        System.out.println("\n--- Mismos headers ordenados por clave ---");
        newHeadersMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));
        System.out.println(response.statusCode());

        System.out.println(response.body());
    }
}