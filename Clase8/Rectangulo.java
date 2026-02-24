public class Rectangulo {
    private Coordenada superiorIzq, inferiorDer;

    // Constructor que recibe dos objetos Coordenada (con copia defensiva)
    public Rectangulo(Coordenada superiorIzq, Coordenada inferiorDer) {
        // Validar que la primera coordenada esté arriba y a la izquierda de la segunda
        if (superiorIzq.abcisa() >= inferiorDer.abcisa() || 
            superiorIzq.ordenada() <= inferiorDer.ordenada()) {
            throw new IllegalArgumentException(
                "La primera coordenada no se encuentra arriba y a la izquierda de la segunda");
        }
        // Crear copias para que los objetos estén dentro del espacio de memoria del rectángulo
        this.superiorIzq = new Coordenada(superiorIzq.abcisa(), superiorIzq.ordenada());
        this.inferiorDer = new Coordenada(inferiorDer.abcisa(), inferiorDer.ordenada());
    }

    // Métodos getter (opcionalmente también podrían devolver copias, pero no es necesario aquí)
    public Coordenada superiorIzquierda() { return superiorIzq; }
    public Coordenada inferiorDerecha() { return inferiorDer; }

    // Métodos para cálculos
    public double ancho() {
        return Math.abs(inferiorDer.abcisa() - superiorIzq.abcisa());
    }

    public double alto() {
        return Math.abs(superiorIzq.ordenada() - inferiorDer.ordenada());
    }

    public double area() {
        return ancho() * alto();
    }

    @Override
    public String toString() {
        return "Esquina superior izquierda: " + superiorIzq + "\tEsquina inferior derecha:" +
                inferiorDer + "\n";
    }
}