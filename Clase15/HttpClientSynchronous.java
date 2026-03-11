
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

public class HttpClientSynchronous {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://httpbin.org/get"))
                .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // print response headers
        HttpHeaders headers = response.headers();
        Map<String, List<String>> headersMap = headers.map();
        Iterator<Map.Entry<String, List<String>>> iterator = headersMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<String>> entry = iterator.next();
            String key = entry.getKey();
            List<String> values = entry.getValue();
            // Recorremos la lista de valores individualmente
            for (String value : values) {
                System.out.println(key + ": " + value);
            }
        }
        // print status code
        System.out.println(response.statusCode());

        // print response body
        System.out.println(response.body());

    }

}
