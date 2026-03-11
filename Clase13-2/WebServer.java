/*
 *  MIT License
 *
 *  Copyright (c) 2019 Michael Pogrebinsky - Distributed Systems & Cloud Computing with Java
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;

public class WebServer {
    private static final String TASK_ENDPOINT = "/task";
    private static final String STATUS_ENDPOINT = "/status";
    private static final String SEARCH_ENDPOINT = "/searchtoken";  // Nuevo endpoint

    private final int port;
    private HttpServer server;

    public static void main(String[] args) {
        int serverPort = 8080;
        if (args.length == 1) {
            serverPort = Integer.parseInt(args[0]);
        }

        WebServer webServer = new WebServer(serverPort);
        webServer.startServer();

        System.out.println("Servidor escuchando en el puerto " + serverPort);
    }

    public WebServer(int port) {
        this.port = port;
    }

    public void startServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        HttpContext statusContext = server.createContext(STATUS_ENDPOINT);
        HttpContext taskContext = server.createContext(TASK_ENDPOINT);
        HttpContext searchContext = server.createContext(SEARCH_ENDPOINT); // Nuevo contexto

        statusContext.setHandler(this::handleStatusCheckRequest);
        taskContext.setHandler(this::handleTaskRequest);
        searchContext.setHandler(this::handleSearchTokenRequest); // Manejador nuevo

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
    }

    // Manejador para /task (existente, sin cambios)
    private void handleTaskRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
            exchange.close();
            return;
        }

        Headers headers = exchange.getRequestHeaders();

        if (headers.containsKey("X-Test") && headers.get("X-Test").get(0).equalsIgnoreCase("true")) {
            String dummyResponse = "123\n";
            sendResponse(dummyResponse.getBytes(), exchange);
            return;
        }

        boolean isDebugMode = false;
        if (headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true")) {
            isDebugMode = true;
        }

        long startTime = System.nanoTime();

        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        byte[] responseBytes = calculateResponse(requestBytes);

        long finishTime = System.nanoTime();

        if (isDebugMode) {
            long elapsed = finishTime - startTime;
            long seconds = elapsed / 1_000_000_000;
            long millis = (elapsed % 1_000_000_000) / 1_000_000;
            String debugMessage = String.format("La operacion tomo %d nanosegundos = %d segundos con %d milisegundos.",
                    elapsed, seconds, millis);
            exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        sendResponse(responseBytes, exchange);
    }

    private byte[] calculateResponse(byte[] requestBytes) {
        String bodyString = new String(requestBytes);
        String[] stringNumbers = bodyString.split(",");

        BigInteger result = BigInteger.ONE;

        for (String number : stringNumbers) {
            BigInteger bigInteger = new BigInteger(number);
            result = result.multiply(bigInteger);
        }

        return String.format("El resultado de la multiplicación es %s\n", result).getBytes();
    }

    // Manejador para /status (existente)
    private void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.close();
            return;
        }

        String responseMessage = "El servidor está vivo\n";
        sendResponse(responseMessage.getBytes(), exchange);
    }

    // NUEVO MANEJADOR PARA /searchtoken
    private void handleSearchTokenRequest(HttpExchange exchange) throws IOException {
        // Solo aceptamos POST
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
            exchange.close();
            return;
        }

        Headers headers = exchange.getRequestHeaders();

        // Verificar si modo debug está activado
        boolean isDebugMode = false;
        if (headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true")) {
            isDebugMode = true;
        }

        long startTime = System.nanoTime();

        // Leer cuerpo de la petición
        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        String body = new String(requestBytes).trim();

        // Parsear: se espera "n,subcadena" (ej. "34567890,SOL")
        String[] parts = body.split(",");
        if (parts.length != 2) {
            String error = "Formato inválido. Use: <número>,<tres letras>\n";
            sendResponse(error.getBytes(), exchange);
            return;
        }

        int numTokens;
        try {
            numTokens = Integer.parseInt(parts[0].trim());
        } catch (NumberFormatException e) {
            String error = "El primer parámetro debe ser un número entero.\n";
            sendResponse(error.getBytes(), exchange);
            return;
        }

        String target = parts[1].trim();
        if (target.length() != 3) {
            String error = "La subcadena debe tener exactamente 3 letras.\n";
            sendResponse(error.getBytes(), exchange);
            return;
        }

        // Generar la cadenota
        char[] cadenota = new char[numTokens * 4]; // cada token: 3 letras + espacio
        Random rand = new Random();

        for (int i = 0; i < numTokens; i++) {
            int base = i * 4;
            // Letras mayúsculas aleatorias (A-Z)
            cadenota[base] = (char) (rand.nextInt(26) + 'A');
            cadenota[base + 1] = (char) (rand.nextInt(26) + 'A');
            cadenota[base + 2] = (char) (rand.nextInt(26) + 'A');
            cadenota[base + 3] = ' '; // espacio separador
        }

        // Buscar la subcadena objetivo
        int count = 0;
        char c0 = target.charAt(0);
        char c1 = target.charAt(1);
        char c2 = target.charAt(2);

        for (int i = 0; i < cadenota.length - 2; i++) {
            if (cadenota[i] == c0 && cadenota[i + 1] == c1 && cadenota[i + 2] == c2) {
                count++;
            }
        }

        // Preparar respuesta
        String responseMessage = "Total de apariciones: " + count + "\n";
        byte[] responseBytes = responseMessage.getBytes();

        long finishTime = System.nanoTime();

        // Si modo debug, agregar header con tiempo
        if (isDebugMode) {
            long elapsed = finishTime - startTime;
            long seconds = elapsed / 1_000_000_000;
            long millis = (elapsed % 1_000_000_000) / 1_000_000;
            String debugMessage = String.format("La operacion tomo %d nanosegundos = %d segundos con %d milisegundos.",
                    elapsed, seconds, millis);
            exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        sendResponse(responseBytes, exchange);
    }


    private void sendResponse(byte[] responseBytes, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
        exchange.close();
    }
}
