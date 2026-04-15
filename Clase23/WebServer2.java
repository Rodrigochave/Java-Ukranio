import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class WebServer2 {
    private static final String STATUS_ENDPOINT = "/status";
    private static final String ALUMNOS_ENDPOINT = "/alumnos";

    private final int port;
    private HttpServer server;

    // Almacenamiento de alumnos
    private final Map<Integer, Alumno> alumnos = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public static void main(String[] args) {
        int serverPort = 8080;
        if (args.length == 1) {
            serverPort = Integer.parseInt(args[0]);
        }

        WebServer2 webServer = new WebServer2(serverPort);
        webServer.startServer();

        System.out.println("Servidor escuchando en el puerto " + serverPort);
    }

    public WebServer2(int port) {
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
        HttpContext alumnosContext = server.createContext(ALUMNOS_ENDPOINT);

        statusContext.setHandler(this::handleStatusCheckRequest);
        alumnosContext.setHandler(this::handleAlumnosRequest);

        server.setExecutor(Executors.newFixedThreadPool(1));
        server.start();
    }

private void handleAlumnosRequest(HttpExchange exchange) throws IOException {
    String ruta = exchange.getRequestURI().getPath();
    String metodo = exchange.getRequestMethod();

    System.out.println("Ruta solicitada: " + ruta);
    System.out.println("Método solicitado: " + metodo);

    String[] partes = ruta.split("/");
    
    if (metodo.equalsIgnoreCase("get")) {
        // GET /alumnos/{id}
        if (partes.length == 3) {
            try {
                int id = Integer.parseInt(partes[2]);
                Alumno a = alumnos.get(id);
                if (a != null) {
                    String response = String.format("ID: %d\tNombre: %s\tApellido 1: %s\tApellido 2: %s\tPromedio: %.1f",
                            a.id, a.nombre, a.apellido1, a.apellido2, a.promedio);
                    sendResponse(response.getBytes(), exchange);
                } else {
                    // Enviar respuesta vacía (como en el ejemplo: curl: (52) Empty reply)
                    sendResponse(new byte[0], exchange);
                }
            } catch (NumberFormatException e) {
                sendResponse("ID inválido".getBytes(), exchange);
            }
        } else {
            sendResponse("Use /alumnos/{id}".getBytes(), exchange);
        }
    } else if (metodo.equalsIgnoreCase("post")) {
        // POST /alumnos
        if (partes.length == 2) {
            byte[] requestBytes = exchange.getRequestBody().readAllBytes();
            String body = new String(requestBytes).trim();
            String[] campos = body.split(",");
            if (campos.length == 4) {
                try {
                    String nombre = campos[0].trim();
                    String apellido1 = campos[1].trim();
                    String apellido2 = campos[2].trim();
                    double promedio = Double.parseDouble(campos[3].trim());

                    int nuevoId = nextId.getAndIncrement();
                    Alumno nuevo = new Alumno(nuevoId, nombre, apellido1, apellido2, promedio);
                    alumnos.put(nuevoId, nuevo);
                    
                    String response = String.format("ID: %d\tNombre: %s\tApellido 1: %s\tApellido 2: %s\tPromedio: %.1f",
                            nuevoId, nombre, apellido1, apellido2, promedio);
                    sendResponse(response.getBytes(), exchange);
                } catch (NumberFormatException e) {
                    sendResponse("Promedio inválido".getBytes(), exchange);
                }
            } else {
                sendResponse("Formato: nombre,apellido1,apellido2,promedio".getBytes(), exchange);
            }
        } else {
            sendResponse("Para POST use /alumnos".getBytes(), exchange);
        }
    } else if (metodo.equalsIgnoreCase("put")) {
        // PUT /alumnos/{id}
        if (partes.length == 3) {
            try {
                int id = Integer.parseInt(partes[2]);
                if (alumnos.containsKey(id)) {
                    byte[] requestBytes = exchange.getRequestBody().readAllBytes();
                    String body = new String(requestBytes).trim();
                    String[] campos = body.split(",");
                    if (campos.length == 4) {
                        String nombre = campos[0].trim();
                        String apellido1 = campos[1].trim();
                        String apellido2 = campos[2].trim();
                        double promedio = Double.parseDouble(campos[3].trim());

                        Alumno actualizado = new Alumno(id, nombre, apellido1, apellido2, promedio);
                        alumnos.put(id, actualizado);
                        
                        String response = String.format("ID: %d\tNombre: %s\tApellido 1: %s\tApellido 2: %s\tPromedio: %.1f",
                                id, nombre, apellido1, apellido2, promedio);
                        sendResponse(response.getBytes(), exchange);
                    } else {
                        sendResponse("Formato: nombre,apellido1,apellido2,promedio".getBytes(), exchange);
                    }
                } else {
                    sendResponse("Alumno no encontrado".getBytes(), exchange);
                }
            } catch (NumberFormatException e) {
                sendResponse("ID inválido".getBytes(), exchange);
            }
        } else {
            sendResponse("Use /alumnos/{id}".getBytes(), exchange);
        }
    } else if (metodo.equalsIgnoreCase("delete")) {
        // DELETE /alumnos/{id}
        if (partes.length == 3) {
            try {
                int id = Integer.parseInt(partes[2]);
                if (alumnos.remove(id) != null) {
                    String response = "Alumno con ID: " + id + " removido de la lista de alumnos.";
                    sendResponse(response.getBytes(), exchange);
                } else {
                    sendResponse(new byte[0], exchange); // Respuesta vacía
                }
            } catch (NumberFormatException e) {
                sendResponse("ID inválido".getBytes(), exchange);
            }
        } else {
            sendResponse("Use /alumnos/{id}".getBytes(), exchange);
        }
    } else {
        sendResponse("Método no permitido".getBytes(), exchange);
    }
}

    private void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.close();
            return;
        }
        String responseMessage = "El servidor está vivo\n";
        sendResponse(responseMessage.getBytes(), exchange);
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