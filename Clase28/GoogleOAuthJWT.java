import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GoogleOAuthJWT {
    public static void main(String[] args) throws Exception {
        // 1. Leer JSON credentials
        String json = new String(Files.readAllBytes(Paths.get("credentials.json")));
        JsonObject creds = JsonParser.parseString(json).getAsJsonObject();
        String clientEmail = creds.get("client_email").getAsString();
        String privateKeyPem = creds.get("private_key").getAsString();
        String tokenUri = creds.get("token_uri").getAsString();

        // 2. Limpiar clave privada PEM
        privateKeyPem = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyPem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(spec);

        // 3. Crear Header JWT
        String headerJson = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        String header = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));

        // 4. Crear Payload JWT
        long now = Instant.now().getEpochSecond();
        String payloadJson = "{"
                + "\"iss\":\"" + clientEmail + "\","
                + "\"scope\":\"https://www.googleapis.com/auth/devstorage.read_only\","
                + "\"aud\":\"" + tokenUri + "\","
                + "\"exp\":" + (now + 3600) + ","
                + "\"iat\":" + now
                + "}";
        String payload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

        // 5. Firmar JWT
        String unsignedToken = header + "." + payload;
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(unsignedToken.getBytes(StandardCharsets.UTF_8));
        byte[] signed = signature.sign();
        String jwt = unsignedToken + "." + base64UrlEncode(signed);

        // 6. Enviar a OAuth token endpoint
        String requestBody = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer"
                + "&assertion=" + jwt;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(tokenUri))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

        // 7. Verificar respuesta del token y extraer access_token
        if (tokenResponse.statusCode() != 200) {
            System.err.println("Error obteniendo token: " + tokenResponse.statusCode());
            System.err.println(tokenResponse.body());
            return;
        }
        JsonObject tokenJson = JsonParser.parseString(tokenResponse.body()).getAsJsonObject();
        String accessToken = tokenJson.get("access_token").getAsString();
        System.out.println("Token obtenido correctamente.");

        // 8. Descargar el archivo del bucket usando el token
        String fileUrl = "https://storage.googleapis.com/storage/v1/b/elmms/o/Roadofthedead.png?alt=media";
        HttpRequest downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(fileUrl))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<byte[]> downloadResponse = client.send(downloadRequest, HttpResponse.BodyHandlers.ofByteArray());

        if (downloadResponse.statusCode() == 200) {
            // Guardar el archivo localmente
            String outputFileName = "Roadofthedead.jpg";
            try (FileOutputStream fos = new FileOutputStream(outputFileName)) {
                fos.write(downloadResponse.body());
            }
            System.out.println("Archivo descargado correctamente: " + outputFileName);
        } else {
            System.err.println("Error descargando el archivo: " + downloadResponse.statusCode());
            System.err.println(new String(downloadResponse.body(), StandardCharsets.UTF_8));
        }
    }

    private static String base64UrlEncode(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }
}