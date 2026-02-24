import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
public class MovimientoCuadrado extends JPanel implements ActionListener {
 private int x = 0; // Posición horizontal del cuadrado
 private final int y = 100; // Posición vertical fija
 private final int SIZE = 50; // Tamaño del cuadrado
 private final int VELOCIDAD = 5;// Pixeles que avanza por frame
 private Timer timer;
 public MovimientoCuadrado() {
 timer = new Timer(30, this); // 30 ms entre frames (~33 FPS)
 timer.start();
 }
 @Override
 protected void paintComponent(Graphics g) {
 super.paintComponent(g);
 // Dibujar el cuadrado
 g.setColor(Color.RED);
 g.fillRect(x, y, SIZE, SIZE);
 }
 @Override
 public void actionPerformed(ActionEvent e) {
 // Actualizar posición
 x += VELOCIDAD;
 // Si llega al borde derecho, vuelve a empezar
 if (x > getWidth()) {
 x = -SIZE;
 }
 repaint(); // Redibujar
 }
 public static void main(String[] args) {
 JFrame frame = new JFrame("Movimiento de Cuadrado");
 MovimientoCuadrado panel = new MovimientoCuadrado();
 frame.add(panel);
 frame.setSize(600, 300);
 frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 frame.setLocationRelativeTo(null);
 frame.setVisible(true);
 }
}