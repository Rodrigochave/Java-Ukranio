public class TrianguloEq extends Figura {

    private double lado;

    public TrianguloEq(Coordenada centro, double lado) {
        super(centro, new Coordenada[3]);
        this.lado = lado;

        double h = Math.sqrt(3) * lado / 2;

        vertices[0] = new Coordenada(
                centro.abcisa(),
                centro.ordenada() + (2*h/3)
        );

        vertices[1] = new Coordenada(
                centro.abcisa() - lado/2,
                centro.ordenada() - h/3
        );

        vertices[2] = new Coordenada(
                centro.abcisa() + lado/2,
                centro.ordenada() - h/3
        );
    }

    @Override
    public double area() {
        return (Math.sqrt(3)/4) * lado * lado;
    }
}
