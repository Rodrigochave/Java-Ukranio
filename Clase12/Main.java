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

        ArrayList<ArrayList<String>> listaDeListas = new ArrayList<>();

        for (int i = 0; i < m; i++) {
            ArrayList<String> listaInterna = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                listaInterna.add(getCURP());
            }
            listaDeListas.add(listaInterna);
        }

        // 1. Imprimir todas las listas originales
        System.out.println("\n=== LISTAS ORIGINALES ===");
        for (int i = 0; i < m; i++) {
            System.out.println("\n--- Lista " + (i + 1) + " (original) ---");
            System.out.println(listaDeListas.get(i));
        }

        // 2. Imprimir todas las listas ordenadas
        System.out.println("\n=== LISTAS ORDENADAS (por primeros 4 caracteres) ===");
        for (int i = 0; i < m; i++) {
            System.out.println("\n--- Lista " + (i + 1) + " (ordenada) ---");
            ArrayList<String> ordenada = obtenerOrdenada(listaDeListas.get(i));
            System.out.println(ordenada);
        }
    }
    static void insertarOrdenado(ArrayList<String> lista, String nuevaCURP) {
        int posicion = 0;
        while (posicion < lista.size()) {
            String actual = lista.get(posicion);
            String subActual = actual.substring(0, 4);
            String subNueva = nuevaCURP.substring(0, 4);
            if (subNueva.compareTo(subActual) < 0) {
                break;
            }
            posicion++;
        }
        lista.add(posicion, nuevaCURP);
    }
    static String getCURP() {
        String Letra = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String Numero = "0123456789";
        String Sexo = "HM";
        String[] Entidad = {"AS", "BC", "BS", "CC", "CS", "CH", "CL", "CM", "DF", "DG", "GT",
                "GR", "HG", "JC", "MC", "MN", "MS", "NT", "NL", "OC", "PL", "QT", "QR", "SP",
                "SL", "SR", "TC", "TL", "TS", "VZ", "YN", "ZS"};

        StringBuilder sb = new StringBuilder(18);

        for (int i = 0; i < 4; i++) {
            int indice = (int) (Letra.length() * Math.random());
            sb.append(Letra.charAt(indice));
        }
        for (int i = 0; i < 6; i++) {
            int indice = (int) (Numero.length() * Math.random());
            sb.append(Numero.charAt(indice));
        }
        int indiceSexo = (int) (Sexo.length() * Math.random());
        sb.append(Sexo.charAt(indiceSexo));
        int indiceEntidad = (int) (Math.random() * Entidad.length);
        sb.append(Entidad[indiceEntidad]);
        for (int i = 0; i < 3; i++) {
            int indice = (int) (Letra.length() * Math.random());
            sb.append(Letra.charAt(indice));
        }
        for (int i = 0; i < 2; i++) {
            int indice = (int) (Numero.length() * Math.random());
            sb.append(Numero.charAt(indice));
        }
        return sb.toString();
    }
    static ArrayList<String> obtenerOrdenada(ArrayList<String> listaOriginal) {
        ArrayList<String> listaOrdenada = new ArrayList<>();
        for (String curp : listaOriginal) {
            insertarOrdenado(listaOrdenada, curp);
        }
        return listaOrdenada;
    }
}