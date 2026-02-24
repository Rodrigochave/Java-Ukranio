public class BuscarIPN_SB {

    public static void main(String[] args) {

        // Verificar argumento
        if (args.length == 0) {
            System.out.println("Uso: java BuscarIPN_SB <n>");
            return;
        }

        int n = Integer.parseInt(args[0]);

        // StringBuilder para la cadena
        StringBuilder cadenota = new StringBuilder(n * 4);

        java.util.Random rand = new java.util.Random();

        // Generar n palabras aleatorias
        for (int i = 0; i < n; i++) {
            // generar 3 letras mayúsculas
            char c1 = (char)(rand.nextInt(26) + 65);
            char c2 = (char)(rand.nextInt(26) + 65);
            char c3 = (char)(rand.nextInt(26) + 65);

            // concatenar con append
            cadenota.append(c1);
            cadenota.append(c2);
            cadenota.append(c3);
            cadenota.append(" "); // espacio separador
        }
        // Buscar "IPN"
        String texto = cadenota.toString();
        int contador = 0;
        int posicion = 0;
        // Buscar todas las ocurrencias
        while ((posicion = texto.indexOf("IPN", posicion)) != -1) {
            contador++;
            posicion++; 
        }
        System.out.println("\nTotal de apariciones: " + contador);
    }
}
