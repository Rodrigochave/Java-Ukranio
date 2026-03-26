import networking.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Arrays;

public class Aggregator {
    private WebClient webClient;

    public Aggregator() {
        this.webClient = new WebClient();
    }

    public List<String> sendTasksToWorkersDynamic(List<String> workersAddresses, List<String> tasks) {
        int numWorkers = workersAddresses.size();
        CompletableFuture<String>[] activeFutures = new CompletableFuture[numWorkers];
        Integer[] taskIndexPerWorker = new Integer[numWorkers];
        String[] workerUrls = workersAddresses.toArray(new String[0]);

        List<String> pendingTasks = new ArrayList<>(tasks);
        String[] resultsArray = new String[tasks.size()];

        // Asignar primeras tareas
        for (int i = 0; i < numWorkers && !pendingTasks.isEmpty(); i++) {
            String task = pendingTasks.remove(0);
            int taskIndex = tasks.indexOf(task);
            taskIndexPerWorker[i] = taskIndex;
            activeFutures[i] = webClient.sendTask(workerUrls[i], task.getBytes());
            System.out.println("Al servidor " + workerUrls[i] + " se le asigna la tarea: " + task);
        }

        // Bucle principal
        while (!pendingTasks.isEmpty() || anyActive(activeFutures)) {

            List<CompletableFuture<String>> nonCompletedFutures = new ArrayList<>();
            for (CompletableFuture<String> f : activeFutures) {
                if (f != null && !f.isDone()) {
                    nonCompletedFutures.add(f);
                }
            }

            if (nonCompletedFutures.isEmpty()) {
                break;
            }

            // Esperar a que termine al menos uno
            CompletableFuture<Object> any = CompletableFuture.anyOf(nonCompletedFutures.toArray(new CompletableFuture[0]));
            any.join();

            // Procesar TODOS los workers que ya han completado (pueden ser varios)
            for (int i = 0; i < numWorkers; i++) {
                if (activeFutures[i] != null && activeFutures[i].isDone()) {
                    // Obtener resultado
                    String result = activeFutures[i].join();
                    int taskIndex = taskIndexPerWorker[i];
                    resultsArray[taskIndex] = result;

                    String completedTask = tasks.get(taskIndex);
                    System.out.println("El servidor " + workerUrls[i] + " completó la tarea " + completedTask);

                    // Asignar nueva tarea si queda alguna
                    if (!pendingTasks.isEmpty()) {
                        String nextTask = pendingTasks.remove(0);
                        int nextTaskIndex = tasks.indexOf(nextTask);
                        taskIndexPerWorker[i] = nextTaskIndex;
                        activeFutures[i] = webClient.sendTask(workerUrls[i], nextTask.getBytes());
                        System.out.println("Al servidor " + workerUrls[i] + " se le asigna la tarea: " + nextTask);
                    } else {
                        activeFutures[i] = null;
                        taskIndexPerWorker[i] = null;
                    }
                }
            }
        }

        // Recolectar resultados en orden original
        List<String> results = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            results.add(resultsArray[i]);
        }
        return results;
    }

    private boolean anyActive(CompletableFuture<?>[] futures) {
        for (CompletableFuture<?> f : futures) {
            if (f != null && !f.isDone()) {
                return true;
            }
        }
        return false;
    }
}