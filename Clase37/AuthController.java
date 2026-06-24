package com.escom.app.server;

import com.escom.app.service.AuthService;
import com.escom.app.util.JSON;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AuthController implements HttpHandler {

    private final AuthService authService = new AuthService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/api/register".equals(path) && method.equals("POST")) {
            handleRegister(exchange);
        } else if ("/api/login".equals(path) && method.equals("POST")) {
            handleLogin(exchange);
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void handleRegister(HttpExchange ex) throws IOException {
        String body = readBody(ex);

        var data = JSON.parse(body);
        String curp = (String) data.get("curp");
        String pass = (String) data.get("password");

        boolean ok = authService.register(curp, pass);

        if (ok) send(ex, 200, "{\"status\": \"registrado\"}");
        else send(ex, 400, "{\"error\": \"Usuario ya existe\"}");
    }

    private void handleLogin(HttpExchange ex) throws IOException {
        String body = readBody(ex);

        var data = JSON.parse(body);
        String curp = (String) data.get("curp");
        String pass = (String) data.get("password");

        String token = authService.login(curp, pass);

        if (token != null) send(ex, 200, "{\"jwt\": \"" + token + "\"}");
        else send(ex, 401, "{\"error\": \"Credenciales inválidas\"}");
    }

    private String readBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private void send(HttpExchange ex, int status, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
}
