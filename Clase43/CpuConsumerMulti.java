import java.util.Random;

public class CpuConsumerMulti {

    static void consumeCpu(long busyMs) {
        Random ran = new Random();
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < busyMs) {
            Math.sqrt(ran.nextInt(2147483647));
        }
    }

    static class CpuTask implements Runnable {
        private final int percent;
        private final int seconds;

        CpuTask(int percent, int seconds) {
            this.percent = percent;
            this.seconds = seconds;
        }

        @Override
        public void run() {
            long totalIntervals = (long) seconds * 1000;
            long busyIntervals  = totalIntervals * percent / 100;
            long idleIntervals  = totalIntervals - busyIntervals;

            boolean[] schedule = new boolean[(int) totalIntervals];
            int idx = 0;
            for (int i = 0; i < busyIntervals; i++) schedule[idx++] = true;
            for (int i = 0; i < idleIntervals;  i++) schedule[idx++] = false;

            Random rng = new Random();
            for (int i = schedule.length - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                boolean tmp = schedule[i]; schedule[i] = schedule[j]; schedule[j] = tmp;
            }

            try {
                for (boolean busy : schedule) {
                    if (busy) consumeCpu(1);
                    else      Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int percent = 65;
        int seconds = args.length >= 1 ? Integer.parseInt(args[0]) : 10;
        int threads = args.length >= 2 ? Integer.parseInt(args[1]) : 1;

        System.out.println("Uso de CPU: " + percent + "% por núcleo");
        System.out.println("Duración:   " + seconds + " segundos");
        System.out.println("Hilos:      " + threads);

        Thread[] pool = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            pool[i] = new Thread(new CpuTask(percent, seconds));
            pool[i].start();
        }
        for (Thread t : pool) t.join();
    }
}