import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class ThreadPoolOrdenaArreglo {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Uso: java ThreadPoolOrdenaArreglo <m> <n> <poolSize>");
            return;
        }
        int m = Integer.parseInt(args[0]);
        int n = Integer.parseInt(args[1]);
        int poolSize = Integer.parseInt(args[2]);

        // Generar listas originales
        ArrayList<ArrayList<String>> listaDeListas = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            ArrayList<String> lista = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                lista.add(getCURP());
            }
            listaDeListas.add(lista);
        }

        // Crear pool del tamaño indicado
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);

        // Enviar tareas de ordenamiento
        for (int i = 0; i < m; i++) {
            final ArrayList<String> original = listaDeListas.get(i);
            pool.submit(() -> {
                ArrayList<String> ordenada = new ArrayList<>();
                for (String curp : original) {
                    insertarOrdenado(ordenada, curp);
                }
                // No se imprime nada
            });
        }

        pool.shutdown();
        try {
            pool.awaitTermination(1, TimeUnit.HOURS); // Espera hasta 1 hora
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void insertarOrdenado(ArrayList<String> lista, String nuevaCURP) {
        int pos = 0;
        while (pos < lista.size()) {
            String actual = lista.get(pos);
            if (nuevaCURP.substring(0, 4).compareTo(actual.substring(0, 4)) < 0)
                break;
            pos++;
        }
        lista.add(pos, nuevaCURP);
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
}