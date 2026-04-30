import java.security.MessageDigest;

public class FuerzaBrutaHash2 {

    public static void main(String[] args) throws Exception {

        String passwordSecreta = "perro"; // palabra conocida de cinco letras

        String hashObjetivo = sha256(passwordSecreta);

        System.out.println("Hash objetivo: " + hashObjetivo);
        System.out.println("Buscando contraseña...\n");

        long inicio = System.currentTimeMillis();

        int intentos = 0;

        for (char a = 'a'; a <= 'z'; a++) {
            for (char b = 'a'; b <= 'z'; b++) {
                for (char c = 'a'; c <= 'z'; c++) {
                    for (char d = 'a'; d <= 'z'; d++) {
                        for (char e = 'a'; e <= 'z'; e++) {

                            String intento = "" + a + b + c + d + e;

                            intentos++;

                            String hash = sha256(intento);

                            if (hash.equals(hashObjetivo)) {

                                long fin = System.currentTimeMillis();

                                System.out.println("Contraseña encontrada: " + intento);
                                System.out.println("Intentos: " + intentos);
                                System.out.println("Tiempo(ms): " + (fin - inicio));

                                return;
                            }
                        }
                    }
                }
            }
        }

        System.out.println("No encontrada");
    }

    public static String sha256(String texto) throws Exception {

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        byte[] hash = md.digest(texto.getBytes());

        StringBuilder hex = new StringBuilder();

        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }
}