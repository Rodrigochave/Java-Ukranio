import java.io.*;
import java.util.*;

public class CombinacionesArchivos {

    public static void main(String[] args) {

        ArrayList<String> nombres = leerArchivo("nombres.txt");
        ArrayList<String> apellidos = leerArchivo("apellidos.txt");
        Random random = new Random(12345); // semilla fija

        // Generar combinaciones
        for (String nombre : nombres) {
            for (String ap1 : apellidos) {
                for (String ap2 : apellidos) {

                    String completo = nombre + " " + ap1 + " " + ap2;
                    double numero = random.nextDouble() * 100;
                    numero = Math.round(numero * 10) / 100.0;
                    System.out.println(completo + " " + numero);
                }
            }
        }
    }

    // Método para leer archivo línea por línea
    public static ArrayList<String> leerArchivo(String nombreArchivo) {

        ArrayList<String> lista = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(nombreArchivo))) {

            String linea;

            while ((linea = br.readLine()) != null) {

                linea = linea.trim();

                if (!linea.isEmpty()) { // evitar líneas vacías
                    lista.add(linea);
                }
            }

        } catch (IOException e) {
            System.out.println("Error leyendo archivo: " + nombreArchivo);
            e.printStackTrace();
        }

        return lista;
    }
}
