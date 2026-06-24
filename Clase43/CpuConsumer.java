import java.util.Random;

public class CpuConsumer {

    static void consumeCpu(long busyMs) {
        Random ran = new Random();
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < busyMs) {
            Math.sqrt(ran.nextInt(2147483647));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int percent = 65;
        int seconds = args.length >= 1 ? Integer.parseInt(args[0]) : 10;

        System.out.println("Uso de CPU: " + percent + "%");
        System.out.println("Duración:   " + seconds + " segundos");

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

        for (boolean busy : schedule) {
            if (busy) consumeCpu(1);
            else      Thread.sleep(1);
        }
    }
}