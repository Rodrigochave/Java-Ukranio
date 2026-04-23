package networking;

public class Main {
    public static void main(String[] args) {
        WebClient webClient = new WebClient();

        // Construir JSON manualmente
        String json = "{"
                + "\"userId\": 1,"
                + "\"title\": \"Online\","
                + "\"body\": \"Chavez Aquiagual Rodrigo\""
                + "}";

        byte[] payload = json.getBytes();
        String url = "https://jsonplaceholder.typicode.com/posts";

        webClient.sendTask(url, payload)
                .thenAccept(response -> {
                    System.out.println("Respuesta del servidor:");
                    System.out.println(response);
                })
                .join();
    }
}