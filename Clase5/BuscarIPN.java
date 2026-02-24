public class BuscarIPN {
    public static void main(String[] args) {
        // Verificar parámetro
        if (args.length == 0) {
            System.out.println("Uso: java BuscarIPN <n>");
            return;
        }

        int n = Integer.parseInt(args[0]);

        // Crear la cadena (3 letras + espacio por palabra)
        char[] cadenota = new char[n * 4];

        // Generador de números aleatorios
        java.util.Random rand = new java.util.Random();
        // Generar palabras aleatorias
        for (int i = 0; i < n; i++) {

            int base = i * 4;

            // 3 letras mayúsculas aleatorias (ASCII 65-90)
            cadenota[base] = (char) (rand.nextInt(26) + 65);
            cadenota[base + 1] = (char) (rand.nextInt(26) + 65);
            cadenota[base + 2] = (char) (rand.nextInt(26) + 65);

            // espacio separador
            cadenota[base + 3] = ' ';
        }
        // Buscar "IPN"
        int contador = 0;

        for (int i = 0; i < cadenota.length - 2; i++) {
            if (cadenota[i] == 'I' &&
                cadenota[i + 1] == 'P' &&
                cadenota[i + 2] == 'N') {
                contador++;
            }
        }
        // Resultado final
        System.out.println("\nTotal de apariciones: " + contador);
    }
}
