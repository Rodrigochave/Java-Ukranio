public abstract class Figura implements Desplazable {

    protected Coordenada centro;
    protected Coordenada[] vertices;

    public Figura(Coordenada centro, Coordenada[] vertices) {
        this.centro = centro;
        this.vertices = vertices;
    }

    // metodo abstracto
    public abstract double area();

    // mover toda la figura
    @Override
    public void desplazar(double dx, double dy) {

        // mover centro
        centro = new Coordenada(
                centro.abcisa() + dx,
                centro.ordenada() + dy
        );

        // mover vertices
        for (int i = 0; i < vertices.length; i++) {
            vertices[i] = new Coordenada(
                    vertices[i].abcisa() + dx,
                    vertices[i].ordenada() + dy
            );
        }
    }

    public void mostrarVertices() {
        for (Coordenada v : vertices) {
            System.out.println(v);
        }
    }
}
