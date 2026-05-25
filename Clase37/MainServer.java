package com.escom.app.server;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class MainServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/register", new AuthController());
        server.createContext("/api/login", new AuthController());
        server.createContext("/api/secure", new SecureController());

        System.out.println("Servidor escuchando en http://localhost:8080 ...");
        server.start();
    }
}
