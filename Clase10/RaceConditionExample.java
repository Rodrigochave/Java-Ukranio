public class RaceConditionExample {
    // Variable compartida inicializada a cero
    static int variable_compartida = 0;
    // Constantes para identificar los hilos
    private static final String HILO_1 = "Hilo-1";
    private static final String HILO_2 = "Hilo-2";
    private static int n = 0;
    public static void main(String[] args) throws InterruptedException {
        // Verificar que se proporcionó un argumento
        if (args.length != 1) {
            System.out.println("Uso: java RaceConditionExample <numero_iteraciones>");
            System.exit(1);
        }
        n = Integer.parseInt(args[0]);
        // Crear una instancia de Runnable (reutilizada para ambos hilos)
        Runnable tarea = new Runnable() {
            @Override
            public void run() {
                // Ejecutar el método modifica() n veces
                for (int i = 0; i < n; i++) {
                    modifica();
                }
            }
        };
        // Crear dos hilos con nombres identificativos
        Thread hilo1 = new Thread(tarea, HILO_1);
        Thread hilo2 = new Thread(tarea, HILO_2);
        // Iniciar ambos hilos
        hilo1.start();
        hilo2.start();
        // Esperar a que ambos hilos terminen completamente
        hilo1.join();
        hilo2.join();
        System.out.println("Valor final de variable_compartida: " + variable_compartida);
    }
    // Método sincronizado para garantizar exclusión mutua
    public static synchronized void modifica() {
        String id = Thread.currentThread().getName();
        if (id.equals(HILO_1)) {
            variable_compartida++;  // Incremento
        }
        if (id.equals(HILO_2)) {
            variable_compartida--;  // Decremento
        }
    }
}