//Proyecto 3        Nombre: Chavez Aquiagual Rodrigo    Grupo:7CM4
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulador de visualización del precio de Bitcoin.
 * Lee precios minuto a minuto de datos.dat (un precio por línea)
 * y muestra un gráfico desplazándose de derecha a izquierda
 */
public class BitcoinVisualizer extends JFrame {
    private double[] prices;                
    private int totalMinutes;                
    private double minPrice, maxPrice;       
    private GraphPanel graphPanel;
    private Timer timer;
    private int currentMinute = 0;            
    private boolean running = false;          

    private JButton startPauseButton;
    private String fechaSimulacion; // fecha del dataset
    public BitcoinVisualizer() {
        loadData(); 
        cargarFecha();                
        initUI();                   
        startTimer();               
        escribirMinutoActual();
    }

    /** Carga los precios desde datos.dat*/
    private void loadData() {
        List<Double> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("datos.dat"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    double price = Double.parseDouble(line);
                    list.add(price);
                }
            }
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this,
                    "No se encontró el archivo datos.dat",
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al leer datos.dat: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        if (list.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "El archivo datos.dat está vacío",
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        totalMinutes = list.size();
        prices = new double[totalMinutes];
        minPrice = Double.MAX_VALUE;
        maxPrice = Double.MIN_VALUE;
        for (int i = 0; i < totalMinutes; i++) {
            prices[i] = list.get(i);
            if (prices[i] < minPrice) minPrice = prices[i];
            if (prices[i] > maxPrice) maxPrice = prices[i];
        }
    }

    private void cargarFecha() {
    try (BufferedReader br = new BufferedReader(new FileReader("fecha.txt"))) {
        fechaSimulacion = br.readLine().trim();
    } catch (IOException e) {
        // Fallback a fecha actual
        fechaSimulacion = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
    }

    private void escribirMinutoActual() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("minuto_actual.txt"))) {
            pw.println(currentMinute);
        } catch (IOException ex) {
        }
    }

    /** Interfaz gráfica. */
    private void initUI() {
        setTitle("Visualizador Bitcoin");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        graphPanel = new GraphPanel();
        add(graphPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();

        startPauseButton = new JButton("Iniciar");
        startPauseButton.addActionListener(e -> toggleSimulation());
        controlPanel.add(startPauseButton);

        JButton resetButton = new JButton("Reiniciar");
        resetButton.addActionListener(e -> {
            currentMinute = 0;
            escribirMinutoActual(); // actualizar archivo al reiniciar
            graphPanel.repaint();
        });
        controlPanel.add(resetButton);

        add(controlPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private void startTimer() {
        timer = new Timer(1000, e -> {
            if (running && currentMinute < totalMinutes - 1) {
                currentMinute++;
                escribirMinutoActual();
                graphPanel.repaint();
            } else if (currentMinute == totalMinutes - 1) {
                running = false;
                startPauseButton.setText("Iniciar");
            }
        });
        timer.start();
    }

    private void toggleSimulation() {
        running = !running;
        startPauseButton.setText(running ? "Pausar" : "Iniciar");
    }

    class GraphPanel extends JPanel {
        private int leftMargin = 80;      // más espacio para etiquetas de precio
        private int rightMargin = 40;
        private int topMargin = 30;
        private int bottomMargin = 70;    // más espacio para fecha y hora

        private final String todayStr = fechaSimulacion; 
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int plotWidth = w - leftMargin - rightMargin;
            int plotHeight = h - topMargin - bottomMargin;
            if (plotWidth <= 0 || plotHeight <= 0) return;

            int bottomY = h - bottomMargin;
            int topY = topMargin;
            int leftX = leftMargin;
            int rightX = leftMargin + plotWidth;

            double yMin = minPrice - (maxPrice - minPrice) * 0.05;
            double yMax = maxPrice + (maxPrice - minPrice) * 0.05;
            double yRange = yMax - yMin;

            g2.setColor(new Color(220, 220, 220)); // gris claro
            g2.setStroke(new BasicStroke(1));

            int numYTicks = 5;
            for (int i = 0; i <= numYTicks; i++) {
                double price = yMin + yRange * i / numYTicks;
                int y = bottomY - (int) ((price - yMin) * plotHeight / yRange);
                g2.drawLine(leftX, y, rightX, y);
            }

            int startMinute = Math.max(0, currentMinute - plotWidth + 1);
            int endMinute = currentMinute;
            int firstHourMinute = ((startMinute + 59) / 60) * 60; 
            for (int m = firstHourMinute; m <= endMinute; m += 60) {
                int x = leftX + (m - startMinute);
                if (x >= leftX && x <= rightX) {
                    g2.drawLine(x, topY, x, bottomY);
                }
            }

            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(leftX, bottomY, rightX, bottomY);   // eje X
            g2.drawLine(leftX, bottomY, leftX, topY);       // eje Y

            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString("Tiempo (horas)", w / 2 - 50, h - 20);
            g2.drawString("Precio (USD)", 15, 30);

            g2.drawString("Fecha: " + fechaSimulacion, leftX, h - 10);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            for (int m = firstHourMinute; m <= endMinute; m += 60) {
                int x = leftX + (m - startMinute);
                if (x >= leftX && x <= rightX) {
                    g2.drawLine(x, bottomY, x, bottomY + 5);
                    int hour = m / 60;
                    int minute = m % 60;
                    String timeStr = String.format("%02d:%02d", hour, minute);
                    g2.drawString(timeStr, x - 15, bottomY + 20);
                }
            }

            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            for (int i = 0; i <= numYTicks; i++) {
                double price = yMin + yRange * i / numYTicks;
                int y = bottomY - (int) ((price - yMin) * plotHeight / yRange);
                if (y >= topY && y <= bottomY) {
                    g2.drawLine(leftX - 5, y, leftX, y);          // marca
                    g2.drawString(String.format("%.2f", price), leftX - 55, y - 3);
                }
            }

            g2.setColor(new Color(173, 216, 230, 100)); 
            int lastX = -1, lastY = -1;
            for (int m = startMinute; m <= endMinute; m++) {
                double price = prices[m];
                int x = leftX + (m - startMinute);           
                int y = bottomY - (int) ((price - yMin) * plotHeight / yRange);
                if (lastX != -1) {
                    Polygon poly = new Polygon();
                    poly.addPoint(lastX, lastY);
                    poly.addPoint(x, y);
                    poly.addPoint(x, bottomY);
                    poly.addPoint(lastX, bottomY);
                    g2.fillPolygon(poly);
                }
                lastX = x;
                lastY = y;
            }
            if (lastX != -1 && lastX < rightX) {
                Polygon poly = new Polygon();
                poly.addPoint(lastX, lastY);
                poly.addPoint(rightX, lastY); 
                poly.addPoint(rightX, bottomY);
                poly.addPoint(lastX, bottomY);
                g2.fillPolygon(poly);
            }

            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(2));
            lastX = -1; lastY = -1;
            for (int m = startMinute; m <= endMinute; m++) {
                double price = prices[m];
                int x = leftX + (m - startMinute);
                int y = bottomY - (int) ((price - yMin) * plotHeight / yRange);
                if (lastX != -1) {
                    g2.drawLine(lastX, lastY, x, y);
                }
                g2.fillOval(x - 2, y - 2, 4, 4);
                lastX = x;
                lastY = y;
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(800, 400);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new BitcoinVisualizer().setVisible(true);
        });
    }
}