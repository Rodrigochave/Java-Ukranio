import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class PoligonoIrreg {
    private List<Coordenada> vertices;

    public PoligonoIrreg(int numVertices) {
        if (numVertices < 3) {
            throw new IllegalArgumentException("Un polígono debe tener al menos 3 vértices");
        }
        vertices = new ArrayList<>(numVertices);
        for (int i = 0; i < numVertices; i++) {
            double x = Math.random() * 20 - 10;
            double y = Math.random() * 20 - 10;
            vertices.add(new Coordenada(x, y));
        }
    }

    public void anadeVertice(Coordenada vertice) {
        vertices.add(vertice);
    }

    public void modificaVertice(int indice, Coordenada nueva) {
        if (indice < 0 || indice >= vertices.size()) {
            throw new IllegalArgumentException("Índice fuera de rango");
        }
        vertices.set(indice, nueva);
    }

    // Ordena los vértices por magnitud de menor a mayor
    public void ordenaVertices() {
        vertices.sort(Comparator.comparingDouble(Coordenada::getMagnitud));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Los vertices del poligono irregular son:\n");
        for (Coordenada c : vertices) {
            sb.append(c.toString()).append("\n");
        }
        sb.append("Vertices totales: ").append(vertices.size());
        return sb.toString();
    }
}