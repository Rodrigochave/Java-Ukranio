import java.util.ArrayList;
import java.util.Iterator;

class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java Main <n>");
            return;
        }
        int n = Integer.parseInt(args[0]);
        ArrayList<String> listaCURP = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String nuevaCURP = getCURP();
            insertarOrdenado(listaCURP, nuevaCURP);
            // Imprimir lista después de cada inserción
            System.out.println("\nLista después de insertar: " + nuevaCURP);
            System.out.println(listaCURP);
        }
    }
    // Método para insertar en orden usando Iterator
    static void insertarOrdenado(ArrayList<String> lista, String nuevaCURP) {
        Iterator<String> it = lista.iterator();
        int posicion = 0;

        while (it.hasNext()) {
            String actual = it.next();

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
    // Método generador de CURP (igual al anterior)
    static String getCURP() {

        String Letra = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String Numero = "0123456789";
        String Sexo = "HM";
        String Entidad[] = {"AS","BC","BS","CC","CS","CH","CL","CM","DF","DG","GT",
                "GR","HG","JC","MC","MN","MS","NT","NL","OC","PL","QT","QR","SP",
                "SL","SR","TC","TL","TS","VZ","YN","ZS"};

        int indice;
        StringBuilder sb = new StringBuilder(18);

        for (int i = 1; i < 5; i++) {
            indice = (int) (Letra.length() * Math.random());
            sb.append(Letra.charAt(indice));
        }

        for (int i = 5; i < 11; i++) {
            indice = (int) (Numero.length() * Math.random());
            sb.append(Numero.charAt(indice));
        }

        indice = (int) (Sexo.length() * Math.random());
        sb.append(Sexo.charAt(indice));

        sb.append(Entidad[(int)(Math.random() * 32)]);

        for (int i = 14; i < 17; i++) {
            indice = (int) (Letra.length() * Math.random());
            sb.append(Letra.charAt(indice));
        }

        for (int i = 17; i < 19; i++) {
            indice = (int) (Numero.length() * Math.random());
            sb.append(Numero.charAt(indice));
        }

        return sb.toString();
    }
}