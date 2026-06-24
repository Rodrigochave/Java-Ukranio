import java.util.ArrayList;
import java.util.List;

public class MemoryHog {
    public static void main(String[] args) throws InterruptedException {
        List<byte[]> blocks = new ArrayList<>();
        int totalMB = 0;
        while (totalMB < 200) {
            byte[] block = new byte[10 * 1024 * 1024]; // 10MB
            for (int i = 0; i < block.length; i++) block[i] = 1; // fuerza asignación real
            blocks.add(block);
            totalMB += 10;
            System.out.println("Memoria reservada: " + totalMB + " MB");
            Thread.sleep(1000);
        }
        System.out.println("Llegó a 200MB, manteniendo memoria...");
        Thread.sleep(60000);
    }
}
