import java.util.Random;

public class RaceConditionExample2 {
    // Clase que representa la pila compartida
    static class Pila {
        private char[] elementos = new char[10];
        private int tope = 0; // Número de elementos (0 = vacía)

        // Método sincronizado para apilar
        public synchronized void apilar(char c) throws InterruptedException {
            while (tope == 10) { // Pila llena
                wait();
            }
            elementos[tope] = c; // Se coloca en la posición tope (0..9)
            tope++;
            notifyAll(); // Notifica a consumidores y visualizador
        }

        // Método sincronizado para desapilar
        public synchronized char desapilar() throws InterruptedException {
            while (tope == 0) { // Pila vacía
                wait();
            }
            tope--;
            char c = elementos[tope];
            notifyAll(); // Notifica a productores y visualizador
            return c;
        }

        // Método sincronizado para mostrar el estado actual
        public synchronized void mostrarEstado() {
            // Limpiar pantalla
            System.out.print("\033[H\033[2J");
            System.out.flush();
            System.out.println("=== Estado de la pila ===");
            System.out.print("Contenido: [");
            for (int i = 0; i < tope; i++) {
                System.out.print(elementos[i]);
                if (i < tope - 1) System.out.print(", ");
            }
            System.out.println("]");
            System.out.println("Tope (número de elementos): " + tope);
            System.out.println("-------------------------\n");
        }
    }

    public static void main(String[] args) {
        Pila pila = new Pila();
        Random rand = new Random();

        // Hilo productor
        Thread productor = new Thread(() -> {
            try {
                while (true) {
                    int tp = rand.nextInt(1000) + 500; // 500-1500 ms
                    Thread.sleep(tp);
                    char c = (char) ('A' + rand.nextInt(26)); // Letra mayúscula aleatoria
                    pila.apilar(c);
                }
            } catch (InterruptedException e) {
                System.out.println("Productor interrumpido");
            }
        }, "Productor");

        // Hilo consumidor
        Thread consumidor = new Thread(() -> {
            try {
                while (true) {
                    int tc = rand.nextInt(1000) + 500;
                    Thread.sleep(tc);
                    pila.desapilar();
                }
            } catch (InterruptedException e) {
                System.out.println("Consumidor interrumpido");
            }
        }, "Consumidor");

        // Hilo visualizador
        Thread visualizador = new Thread(() -> {
            try {
                // Mostrar estado inicial
                synchronized (pila) {
                    pila.mostrarEstado();
                }
                while (true) {
                    synchronized (pila) {
                        pila.wait(); // Espera a que ocurra una modificación
                        pila.mostrarEstado();
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Visualizador interrumpido");
            }
        }, "Visualizador");

        // Iniciar todos los hilos
        productor.start();
        consumidor.start();
        visualizador.start();
    }
}