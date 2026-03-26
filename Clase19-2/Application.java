import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Application {
    private static final String WORKER_ADDRESS_1 = "http://localhost:8081/searchtoken";
    private static final String WORKER_ADDRESS_2 = "http://localhost:8082/searchtoken";

    public static void main(String[] args) {
        Aggregator aggregator = new Aggregator();

        List<String> listaTareas = new ArrayList<>();
        listaTareas.add("1757600,IPN");
        listaTareas.add("17576,SAL");
        listaTareas.add("70000,MAS");
        listaTareas.add("1757600,PEZ");
        listaTareas.add("1757600,SOL");

        System.out.println("Las tareas a resolver son las siguientes:");
        for (int i = 0; i < listaTareas.size(); i++) {
            System.out.println("Tarea " + i + " :" + listaTareas.get(i));
        }
        System.out.println(); // línea en blanco

        List<String> workers = Arrays.asList(WORKER_ADDRESS_1, WORKER_ADDRESS_2);
        List<String> results = aggregator.sendTasksToWorkersDynamic(workers, listaTareas);

        System.out.println(); // separador
        for (int i = 0; i < listaTareas.size(); i++) {
            String task = listaTareas.get(i);
            String count = results.get(i);
            System.out.println("Para la tarea " + task + " El numero de apariciones es " + count);
        }
    }
}