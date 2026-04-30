import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class TestSHA {

    public static void main(String[] args) throws Exception {

        String texto = "perro";

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        byte[] hash = md.digest(texto.getBytes());

        StringBuilder hex = new StringBuilder();

        for(byte b : hash){
            hex.append(String.format("%02x", b));
        }

        System.out.println(hex.toString());
    }
}