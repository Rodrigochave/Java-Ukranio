//Proyecto 2        Nombre: Chavez Aquiagual Rodrigo    Grupo:7CM4
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MovimientoCuadrado extends JPanel implements ActionListener {
    private static final int SIZE = 300;
    private static final int NUM_SQUARES = 6;
    private static final int SPEED = 20;
    private static final int AREA_SIZE = 1000;
    private static final int INITIAL_LIVES = 30;
    private static final int BORDER = 1;
    private static final int STATS_HEIGHT = 300;

    private Square[] squares;
    private int numActivos;
    private Timer timer;

    private boolean animacionVictoria = false;
    private double progresoVictoria = 0.0;
    private Square ganador;
    private int ganadorXIni, ganadorYIni;
    private int ganadorTamIni;
    private static final double VEL_ANIMACION = 0.02;

    public MovimientoCuadrado() {
        setPreferredSize(new Dimension(AREA_SIZE + 2 * BORDER, AREA_SIZE + 2 * BORDER + STATS_HEIGHT));
        setBackground(Color.WHITE);

        squares = new Square[NUM_SQUARES];
        crearCuadradosAleatorios();

        timer = new Timer(30, this);
        timer.start();
    }

    private void crearCuadradosAleatorios() {
        numActivos = 0;
        for (int i = 0; i < NUM_SQUARES; i++) {
            int intentos = 0;
            boolean valido;
            int x, y;
            do {
                valido = true;
                x = (int) (Math.random() * (AREA_SIZE - SIZE));
                y = (int) (Math.random() * (AREA_SIZE - SIZE));
                for (int j = 0; j < numActivos; j++) {
                    Square otro = squares[j];
                    if (Math.abs(x - otro.getX()) < SIZE && Math.abs(y - otro.getY()) < SIZE) {
                        valido = false;
                        break;
                    }
                }
                intentos++;
                if (intentos > 1000) break;
            } while (!valido);

            double angulo = Math.random() * 2 * Math.PI;
            int vx = (int) (SPEED * Math.cos(angulo));
            int vy = (int) (SPEED * Math.sin(angulo));
            if (vx == 0 && vy == 0) vx = SPEED;

            Color color = new Color((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256));
            squares[numActivos] = new Square(x, y, vx, vy, color, INITIAL_LIVES, SIZE);
            numActivos++;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (int i = 0; i < numActivos; i++) {
            Square sq = squares[i];
            g.setColor(sq.getColor());
            g.fillRect(sq.getX() + BORDER, sq.getY() + BORDER, sq.getTamanoActual(), sq.getTamanoActual());
        }

        g.setColor(Color.BLACK);
        g.drawRect(0, 0, AREA_SIZE + BORDER, AREA_SIZE + BORDER);

        dibujarEstadisticas(g);
    }

    private void dibujarEstadisticas(Graphics g) {
        int statsY = AREA_SIZE + 2 * BORDER + 20;
        int barWidth = 200;
        int barHeight = 25;
        int spacing = 10;
        int startX = 20;

        g.setColor(Color.BLACK);
        g.drawString("Vidas restantes:", startX, statsY - 10);

        for (int i = 0; i < numActivos; i++) {
            Square sq = squares[i];
            int x = startX;
            int y = statsY + i * (barHeight + spacing);

            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(x, y, barWidth, barHeight);

            g.setColor(sq.getColor());
            double proporcion = ((sq.getVidas() - 1) * (1.0 - 0.1) / (INITIAL_LIVES - 1)) + 0.1;
            int liveWidth = (int) (barWidth * proporcion);
            g.fillRect(x, y, liveWidth, barHeight);

            g.setColor(Color.BLACK);
            g.drawRect(x, y, barWidth, barHeight);
            g.drawString(String.valueOf(sq.getVidas()), x + barWidth + 5, y + barHeight / 2 + 5);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (animacionVictoria) {
            progresoVictoria += VEL_ANIMACION;
            if (progresoVictoria >= 1.0) {
                progresoVictoria = 1.0;
                ganador.setX(0);
                ganador.setY(0);
                ganador.setTamanoActual(AREA_SIZE);
            } else {
                ganador.setX((int) (ganadorXIni + (0 - ganadorXIni) * progresoVictoria));
                ganador.setY((int) (ganadorYIni + (0 - ganadorYIni) * progresoVictoria));
                ganador.setTamanoActual((int) (ganadorTamIni + (AREA_SIZE - ganadorTamIni) * progresoVictoria));
            }
            repaint();
            return;
        }

        for (int i = 0; i < numActivos; i++) {
            squares[i].mover();
        }

        for (int i = 0; i < numActivos; i++) {
            Square sq = squares[i];
            if (sq.getX() < 0) {
                sq.setX(0);
                sq.rebotarX();
            }
            if (sq.getX() + sq.getTamanoActual() > AREA_SIZE) {
                sq.setX(AREA_SIZE - sq.getTamanoActual());
                sq.rebotarX();
            }
            if (sq.getY() < 0) {
                sq.setY(0);
                sq.rebotarY();
            }
            if (sq.getY() + sq.getTamanoActual() > AREA_SIZE) {
                sq.setY(AREA_SIZE - sq.getTamanoActual());
                sq.rebotarY();
            }
        }

        for (int i = 0; i < numActivos; i++) {
            for (int j = i + 1; j < numActivos; j++) {
                Square a = squares[i];
                Square b = squares[j];
                if (hayColision(a, b)) {
                    procesarColision(a, b);
                }
            }
        }

        int nuevosActivos = 0;
        for (int i = 0; i < numActivos; i++) {
            Square sq = squares[i];
            if (sq.getVidas() > 0) {
                squares[nuevosActivos] = sq;
                nuevosActivos++;
            }
        }
        numActivos = nuevosActivos;

        if (numActivos == 1 && !animacionVictoria) {
            ganador = squares[0];
            ganadorXIni = ganador.getX();
            ganadorYIni = ganador.getY();
            ganadorTamIni = ganador.getTamanoActual();
            animacionVictoria = true;
            progresoVictoria = 0.0;
        }

        repaint();
    }

    private boolean hayColision(Square a, Square b) {
        return (a.getX() < b.getX() + b.getTamanoActual() &&
                b.getX() < a.getX() + a.getTamanoActual() &&
                a.getY() < b.getY() + b.getTamanoActual() &&
                b.getY() < a.getY() + a.getTamanoActual());
    }

    private void procesarColision(Square a, Square b) {
        a.rebotarX();
        a.rebotarY();
        b.rebotarX();
        b.rebotarY();

        a.reducirVida();
        b.reducirVida();

        int centroAx = a.getX() + a.getTamanoActual() / 2;
        int centroAy = a.getY() + a.getTamanoActual() / 2;
        int centroBx = b.getX() + b.getTamanoActual() / 2;
        int centroBy = b.getY() + b.getTamanoActual() / 2;

        a.setX(centroAx - a.getTamanoActual() / 2);
        a.setY(centroAy - a.getTamanoActual() / 2);
        b.setX(centroBx - b.getTamanoActual() / 2);
        b.setY(centroBy - b.getTamanoActual() / 2);

        a.setX(Math.max(0, Math.min(a.getX(), AREA_SIZE - a.getTamanoActual())));
        a.setY(Math.max(0, Math.min(a.getY(), AREA_SIZE - a.getTamanoActual())));
        b.setX(Math.max(0, Math.min(b.getX(), AREA_SIZE - b.getTamanoActual())));
        b.setY(Math.max(0, Math.min(b.getY(), AREA_SIZE - b.getTamanoActual())));

        if (hayColision(a, b)) {
            resolverColision(a, b);
        }
    }

    private void resolverColision(Square a, Square b) {
        int overlapX = Math.min(a.getX() + a.getTamanoActual(), b.getX() + b.getTamanoActual()) - Math.max(a.getX(), b.getX());
        int overlapY = Math.min(a.getY() + a.getTamanoActual(), b.getY() + b.getTamanoActual()) - Math.max(a.getY(), b.getY());

        if (overlapX < overlapY) {
            if (a.getX() < b.getX()) {
                a.setX(a.getX() - overlapX / 2);
                b.setX(b.getX() + overlapX / 2);
            } else {
                a.setX(a.getX() + overlapX / 2);
                b.setX(b.getX() - overlapX / 2);
            }
        } else {
            if (a.getY() < b.getY()) {
                a.setY(a.getY() - overlapY / 2);
                b.setY(b.getY() + overlapY / 2);
            } else {
                a.setY(a.getY() + overlapY / 2);
                b.setY(b.getY() - overlapY / 2);
            }
        }

        a.setX(Math.max(0, Math.min(a.getX(), AREA_SIZE - a.getTamanoActual())));
        a.setY(Math.max(0, Math.min(a.getY(), AREA_SIZE - a.getTamanoActual())));
        b.setX(Math.max(0, Math.min(b.getX(), AREA_SIZE - b.getTamanoActual())));
        b.setY(Math.max(0, Math.min(b.getY(), AREA_SIZE - b.getTamanoActual())));
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Movimiento de 6 Cuadrados con Vidas");
        MovimientoCuadrado panel = new MovimientoCuadrado();
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}