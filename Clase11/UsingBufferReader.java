import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
public class UsingBufferReader {
    public static void main(String[] args) {
        try (
            BufferedReader bfri = new BufferedReader(
                new InputStreamReader(System.in))
        ) {
            System.out.print("Enter the Path: ");
            String path = bfri.readLine();
            Map<Character, Integer> mapa = new HashMap<>();
            try (
                BufferedReader bfro = new BufferedReader(
                    new FileReader(path))
            ) {
                String linea;
                while ((linea = bfro.readLine()) != null) {
                    for (int i = 0; i < linea.length(); i++) {
                        char c = linea.charAt(i);
                        mapa.put(c, mapa.getOrDefault(c, 0) + 1);
                    }
                    // contar salto de línea
                    mapa.put('\n', mapa.getOrDefault('\n', 0) + 1);
                }
            }
            //Convertimos el Map a ArrayList
            ArrayList<Map.Entry<Character, Integer>> lista =
                    new ArrayList<>(mapa.entrySet());
            //Ordenamos usando Comparator (menor a mayor)
            Collections.sort(lista, new Comparator<Map.Entry<Character, Integer>>() {
                @Override
                public int compare(Map.Entry<Character, Integer> e1,
                                   Map.Entry<Character, Integer> e2) {
                    return e1.getValue().compareTo(e2.getValue());
                }
            });
            System.out.println("\nCaracteres ordenados por ocurrencia:\n");
            for (Map.Entry<Character, Integer> entry : lista) {
                System.out.println("'" + entry.getKey() + "' : " + entry.getValue());
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}