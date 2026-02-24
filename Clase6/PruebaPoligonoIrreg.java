public class PruebaPoligonoIrreg {

    public static void main(String[] args) {

        try {

            // Crear poligono de 7 vertices
            PoligonoIrreg poli = new PoligonoIrreg(7);

            System.out.println("Poligono original:");
            System.out.println(poli);

            // Modificar un vertice
            Coordenada nueva = new Coordenada(5, 5);
            poli.modificaVertice(3, nueva);

            System.out.println("Poligono despues de modificar vertice 3:");
            System.out.println(poli);

        } catch(IllegalArgumentException e){
            System.out.println("Error: " + e.getMessage());
        }
    }
}
