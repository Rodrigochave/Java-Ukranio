//Proyecto 2        Nombre: Chavez Aquiagual Rodrigo    Grupo:7CM4
import java.awt.Color;

public class Square {
    private int x, y;
    private int vx, vy;
    private Color color;
    private int vidas;
    private int tamanoActual;
    private final int sizeInicial;
    private final int vidasIniciales;

    public Square(int x, int y, int vx, int vy, Color color, int vidasIniciales, int sizeInicial) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.vidas = vidasIniciales;
        this.tamanoActual = sizeInicial;
        this.sizeInicial = sizeInicial;
        this.vidasIniciales = vidasIniciales;
    }

    // Getters y setters
    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public int getVx() { return vx; }
    public int getVy() { return vy; }
    public void setVx(int vx) { this.vx = vx; }
    public void setVy(int vy) { this.vy = vy; }
    public Color getColor() { return color; }
    public int getVidas() { return vidas; }
    public int getTamanoActual() { return tamanoActual; }
    public void setTamanoActual(int tamano) { this.tamanoActual = tamano; }

    public void mover() {
        x += vx;
        y += vy;
    }

    public void rebotarX() {
        vx = -vx;
    }

    public void rebotarY() {
        vy = -vy;
    }

    public void reducirVida() {
        if (vidas > 0) {
            vidas--;
            actualizarTamanio();
        }
    }

    private void actualizarTamanio() {
        if (vidas <= 0) {
            tamanoActual = 0;
            return;
        }
        double proporcion = ((vidas - 1) * (1.0 - 0.1) / (vidasIniciales - 1)) + 0.1;
        tamanoActual = (int) (sizeInicial * proporcion);
        if (tamanoActual < 1) tamanoActual = 1;
    }
}