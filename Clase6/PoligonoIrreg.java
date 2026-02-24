public class PoligonoIrreg {

    private Coordenada[] vertices;

    // Constructor: recibe número de vértices y genera coordenadas aleatorias
    public PoligonoIrreg(int numVertices) {

        if(numVertices < 3){
            throw new IllegalArgumentException("Un poligono debe tener al menos 3 vertices");
        }

        vertices = new Coordenada[numVertices];

        for(int i = 0; i < numVertices; i++){
            // Valores aleatorios entre -10 y 10 (para que haya en los 4 cuadrantes)
            double x = Math.random() * 20 - 10;
            double y = Math.random() * 20 - 10;

            vertices[i] = new Coordenada(x, y);
        }
    }
    // Metodo para modificar el vertice n-esimo
    public void modificaVertice(int indice, Coordenada nueva){

        if(indice < 0 || indice >= vertices.length){
            throw new IllegalArgumentException("Indice fuera de rango");
        }

        vertices[indice] = nueva;
    }
    // Sobreescritura para imprimir todos los vertices
    @Override
    public String toString(){

        String resultado = "Vertices del poligono:\n";

        for(int i = 0; i < vertices.length; i++){
            resultado += "Vertice " + i + ": " + vertices[i] + "\n";
        }
        return resultado;
    }
}
