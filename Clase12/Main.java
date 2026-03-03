import java.util.ArrayList;

class Main {
    public static void main(String[] args) {
        // Verificar que se reciban exactamente dos parámetros: m y n
        if (args.length != 2) {
            System.out.println("Uso: java Main <m> <n>");
            System.out.println("  m: número de listas internas");
            System.out.println("  n: cantidad de CURPs por lista");
            return;
        }

        int m = Integer.parseInt(args[0]);
        int n = Integer.parseInt(args[1]);

        // Crear la estructura que contiene m listas de CURPs
        ArrayList<ArrayList<String>> listaDeListas = new ArrayList<>();

        // Llenar cada una de las m listas con n CURPs aleatorias
        for (int i = 0; i < m; i++) {
            ArrayList<String> listaInterna = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                listaInterna.add(getCURP());
            }
            listaDeListas.add(listaInterna);
        }

        // Procesar cada lista: mostrar la original y luego su versión ordenada
        for (int i = 0; i < m; i++) {
            System.out.println("\n--- Lista " + (i + 1) + " (original) ---");
            System.out.println(listaDeListas.get(i));
            ordenarYMostrar(listaDeListas.get(i));
        }
    }

    /**
     * Inserta una nueva CURP en una lista que ya se mantiene ordenada
     * según los primeros 4 caracteres. La lista de entrada debe estar
     * previamente ordenada para que el resultado siga siéndolo.
     * @param lista      Lista ordenada donde se insertará
     * @param nuevaCURP  CURP a insertar
     */
    static void insertarOrdenado(ArrayList<String> lista, String nuevaCURP) {
        int posicion = 0;
        while (posicion < lista.size()) {
            String actual = lista.get(posicion);
            // Comparar solo los primeros 4 caracteres
            String subActual = actual.substring(0, 4);
            String subNueva = nuevaCURP.substring(0, 4);
            if (subNueva.compareTo(subActual) < 0) {
                break;
            }
            posicion++;
        }
        lista.add(posicion, nuevaCURP);
    }
    
     * @return Una CURP aleatoria
    static String getCURP() {
        String Letra = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String Numero = "0123456789";
        String Sexo = "HM";
        String[] Entidad = {"AS", "BC", "BS", "CC", "CS", "CH", "CL", "CM", "DF", "DG", "GT",
                "GR", "HG", "JC", "MC", "MN", "MS", "NT", "NL", "OC", "PL", "QT", "QR", "SP",
                "SL", "SR", "TC", "TL", "TS", "VZ", "YN", "ZS"};

        StringBuilder sb = new StringBuilder(18);

        // 4 letras
        for (int i = 0; i < 4; i++) {
            int indice = (int) (Letra.length() * Math.random());
            sb.append(Letra.charAt(indice));
        }

        // 6 dígitos
        for (int i = 0; i < 6; i++) {
            int indice = (int) (Numero.length() * Math.random());
            sb.append(Numero.charAt(indice));
        }

        // Sexo
        int indiceSexo = (int) (Sexo.length() * Math.random());
        sb.append(Sexo.charAt(indiceSexo));

        // Entidad (2 letras)
        int indiceEntidad = (int) (Math.random() * Entidad.length);
        sb.append(Entidad[indiceEntidad]);

        // 3 letras
        for (int i = 0; i < 3; i++) {
            int indice = (int) (Letra.length() * Math.random());
            sb.append(Letra.charAt(indice));
        }

        // 2 dígitos
        for (int i = 0; i < 2; i++) {
            int indice = (int) (Numero.length() * Math.random());
            sb.append(Numero.charAt(indice));
        }

        return sb.toString();
    }
     * @param listaOriginal Lista de CURPs a ordenar e imprimir
     */
    static void ordenarYMostrar(ArrayList<String> listaOriginal) {
        ArrayList<String> listaOrdenada = new ArrayList<>();
        for (String curp : listaOriginal) {
            insertarOrdenado(listaOrdenada, curp);
        }
        System.out.println("--- Lista ordenada---");
        System.out.println(listaOrdenada);
    }
}