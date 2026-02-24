import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MovimientoCuadrado extends JPanel implements ActionListener {
    private static final int SIZE = 150;               // Tamaño inicial de cada cuadrado
    private static final int NUM_SQUARES = 6;           // Número de cuadrados
    private static final int SPEED = 10;                 // Magnitud de la velocidad (píxeles/frame)
    private static final int AREA_SIZE = 1000;           // Tamaño del área de juego (marco interior)
    private static final int INITIAL_LIVES = 10;         // Vidas iniciales de cada cuadrado
    private static final int BORDER = 1;                  // Grosor del marco
    private static final int STATS_HEIGHT = 300;          // Altura de la zona de estadísticas

    private List<Square> squares;                         // Lista de cuadrados activos
    private Timer timer;

    // Clase interna que representa un cuadrado con posición, velocidad, color, vidas y tamaño actual
    private static class Square {
        int x, y;                // Esquina superior izquierda (coordenadas lógicas dentro del área 0..AREA_SIZE)
        int vx, vy;              // Velocidad en píxeles/frame
        Color color;
        int vidas;                // Vidas restantes
        int tamanoActual;         // Tamaño actual (se reduce con las vidas)

        Square(int x, int y, int vx, int vy, Color color, int vidas, int tamanoActual) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.vidas = vidas;
            this.tamanoActual = tamanoActual;
        }
    }

    public MovimientoCuadrado() {
        // Tamaño total del panel: área de juego + borde + zona de estadísticas
        setPreferredSize(new Dimension(AREA_SIZE + 2 * BORDER, AREA_SIZE + 2 * BORDER + STATS_HEIGHT));
        setBackground(Color.WHITE);

        squares = new ArrayList<>();
        crearCuadradosAleatorios();

        timer = new Timer(30, this); // 30 ms entre frames
        timer.start();
    }

    // Genera 6 cuadrados con posiciones aleatorias sin superposición y velocidades aleatorias
    private void crearCuadradosAleatorios() {
        for (int i = 0; i < NUM_SQUARES; i++) {
            int intentos = 0;
            boolean valido;
            int x, y;
            do {
                valido = true;
                x = (int) (Math.random() * (AREA_SIZE - SIZE));
                y = (int) (Math.random() * (AREA_SIZE - SIZE));
                // Verificar que no se superponga con cuadrados ya creados
                for (Square otro : squares) {
                    if (hayColisionInicial(x, y, otro)) {
                        valido = false;
                        break;
                    }
                }
                intentos++;
                if (intentos > 1000) break; // Evitar bucle infinito
            } while (!valido);

            // Velocidad con dirección aleatoria (misma magnitud)
            double angulo = Math.random() * 2 * Math.PI;
            int vx = (int) (SPEED * Math.cos(angulo));
            int vy = (int) (SPEED * Math.sin(angulo));
            // Asegurar que no sea cero (en caso de redondeo)
            if (vx == 0 && vy == 0) {
                vx = SPEED;
            }

            Color color = new Color((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256));
            squares.add(new Square(x, y, vx, vy, color, INITIAL_LIVES, SIZE));
        }
    }

    // Verifica si un nuevo cuadrado en (x,y) colisiona con uno existente (usando tamaño inicial)
    private boolean hayColisionInicial(int x, int y, Square otro) {
        return (Math.abs(x - otro.x) < SIZE) && (Math.abs(y - otro.y) < SIZE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 1. Dibujar el fondo blanco (ya está)
        // 2. Dibujar los cuadrados (desplazados por el borde)
        for (Square sq : squares) {
            g.setColor(sq.color);
            // Las coordenadas lógicas están en 0..AREA_SIZE, se desplazan +BORDER para el marco
            g.fillRect(sq.x + BORDER, sq.y + BORDER, sq.tamanoActual, sq.tamanoActual);
        }

        // 3. Dibujar el marco negro alrededor del área de juego
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, AREA_SIZE + BORDER, AREA_SIZE + BORDER); // El marco ocupa desde (0,0) hasta (AREA_SIZE+1, AREA_SIZE+1)

        // 4. Dibujar las estadísticas debajo del marco
        dibujarEstadisticas(g);
    }

    // Dibuja las barras de vida de cada cuadrado en la parte inferior
    private void dibujarEstadisticas(Graphics g) {
        int statsY = AREA_SIZE + 2 * BORDER + 15; // Posición Y inicial de las estadísticas
        int barWidth = 120;
        int barHeight = 20;
        int spacing = 10;
        int startX = 10;

        g.setColor(Color.BLACK);
        g.drawString("Vidas restantes:", startX, statsY - 5);

        int i = 0;
        for (Square sq : squares) {
            int x = startX + i * (barWidth + spacing);
            int y = statsY;

            // Fondo de la barra (gris)
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(x, y, barWidth, barHeight);

            // Barra de progreso (color del cuadrado) proporcional a las vidas
            g.setColor(sq.color);
            int liveWidth = (int) (barWidth * ((double) sq.vidas / INITIAL_LIVES));
            g.fillRect(x, y, liveWidth, barHeight);

            // Borde negro de la barra
            g.setColor(Color.BLACK);
            g.drawRect(x, y, barWidth, barHeight);

            // Número de vidas
            g.drawString(String.valueOf(sq.vidas), x + barWidth + 5, y + barHeight / 2 + 5);

            i++;
            if (i >= 6) break; // Máximo 6 cuadrados
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Mover todos los cuadrados
        for (Square sq : squares) {
            sq.x += sq.vx;
            sq.y += sq.vy;
        }

        // Rebote con las paredes (límites lógicos 0..AREA_SIZE)
        for (Square sq : squares) {
            // Pared izquierda
            if (sq.x < 0) {
                sq.x = 0;
                sq.vx = -sq.vx;
            }
            // Pared derecha
            if (sq.x + sq.tamanoActual > AREA_SIZE) {
                sq.x = AREA_SIZE - sq.tamanoActual;
                sq.vx = -sq.vx;
            }
            // Pared superior
            if (sq.y < 0) {
                sq.y = 0;
                sq.vy = -sq.vy;
            }
            // Pared inferior
            if (sq.y + sq.tamanoActual > AREA_SIZE) {
                sq.y = AREA_SIZE - sq.tamanoActual;
                sq.vy = -sq.vy;
            }
        }

        // Detectar y procesar colisiones entre cuadrados
        for (int i = 0; i < squares.size(); i++) {
            for (int j = i + 1; j < squares.size(); j++) {
                Square a = squares.get(i);
                Square b = squares.get(j);
                if (hayColision(a, b)) {
                    procesarColision(a, b);
                }
            }
        }

        // Eliminar cuadrados con vidas <= 0
        Iterator<Square> it = squares.iterator();
        while (it.hasNext()) {
            Square sq = it.next();
            if (sq.vidas <= 0) {
                it.remove();
            }
        }

        repaint();
    }

    // Verifica si dos cuadrados se superponen (usando sus tamaños actuales)
    private boolean hayColision(Square a, Square b) {
        return (a.x < b.x + b.tamanoActual &&
                b.x < a.x + a.tamanoActual &&
                a.y < b.y + b.tamanoActual &&
                b.y < a.y + a.tamanoActual);
    }

    // Procesa la colisión entre dos cuadrados: reduce vidas, ajusta tamaño y posición, invierte velocidades y separa
    private void procesarColision(Square a, Square b) {
        // Invertir velocidades (rebote)
        a.vx = -a.vx;
        a.vy = -a.vy;
        b.vx = -b.vx;
        b.vy = -b.vy;

        // Reducir vidas (mínimo 0)
        a.vidas = Math.max(0, a.vidas - 1);
        b.vidas = Math.max(0, b.vidas - 1);

        // Guardar centros antes de cambiar tamaño
        int centroAx = a.x + a.tamanoActual / 2;
        int centroAy = a.y + a.tamanoActual / 2;
        int centroBx = b.x + b.tamanoActual / 2;
        int centroBy = b.y + b.tamanoActual / 2;

        // Actualizar tamaños según vidas
        a.tamanoActual = (int) (SIZE * ((double) a.vidas / INITIAL_LIVES));
        b.tamanoActual = (int) (SIZE * ((double) b.vidas / INITIAL_LIVES));
        // Asegurar que al menos sea 1 si hay vidas (para que se vea)
        if (a.vidas > 0 && a.tamanoActual < 1) a.tamanoActual = 1;
        if (b.vidas > 0 && b.tamanoActual < 1) b.tamanoActual = 1;

        // Reubicar para mantener el centro
        a.x = centroAx - a.tamanoActual / 2;
        a.y = centroAy - a.tamanoActual / 2;
        b.x = centroBx - b.tamanoActual / 2;
        b.y = centroBy - b.tamanoActual / 2;

        // Asegurar que no se salgan de los bordes después del reajuste
        a.x = Math.max(0, Math.min(a.x, AREA_SIZE - a.tamanoActual));
        a.y = Math.max(0, Math.min(a.y, AREA_SIZE - a.tamanoActual));
        b.x = Math.max(0, Math.min(b.x, AREA_SIZE - b.tamanoActual));
        b.y = Math.max(0, Math.min(b.y, AREA_SIZE - b.tamanoActual));

        // Separar para evitar superposición residual (si aún están superpuestos)
        if (hayColision(a, b)) {
            resolverColision(a, b);
        }
    }

    // Separa dos cuadrados que aún se superponen y ajusta velocidades (ya invertidas)
    private void resolverColision(Square a, Square b) {
        // Calcular superposición en cada eje
        int overlapX = Math.min(a.x + a.tamanoActual, b.x + b.tamanoActual) - Math.max(a.x, b.x);
        int overlapY = Math.min(a.y + a.tamanoActual, b.y + b.tamanoActual) - Math.max(a.y, b.y);

        if (overlapX < overlapY) {
            // Separar en horizontal
            if (a.x < b.x) {
                a.x -= overlapX / 2;
                b.x += overlapX / 2;
            } else {
                a.x += overlapX / 2;
                b.x -= overlapX / 2;
            }
        } else {
            // Separar en vertical
            if (a.y < b.y) {
                a.y -= overlapY / 2;
                b.y += overlapY / 2;
            } else {
                a.y += overlapY / 2;
                b.y -= overlapY / 2;
            }
        }

        // Ajustar para que no se salgan de los bordes
        a.x = Math.max(0, Math.min(a.x, AREA_SIZE - a.tamanoActual));
        a.y = Math.max(0, Math.min(a.y, AREA_SIZE - a.tamanoActual));
        b.x = Math.max(0, Math.min(b.x, AREA_SIZE - b.tamanoActual));
        b.y = Math.max(0, Math.min(b.y, AREA_SIZE - b.tamanoActual));
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Movimiento de 6 Cuadrados con Vidas");
        MovimientoCuadrado panel = new MovimientoCuadrado();
        frame.add(panel);
        frame.pack(); // Ajusta el tamaño al preferido
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}