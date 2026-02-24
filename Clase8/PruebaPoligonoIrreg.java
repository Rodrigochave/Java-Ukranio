public class PruebaPoligonoIrreg {
    public static void main(String[] args) {
        try {
            // Crear polígono con 7 vértices
            PoligonoIrreg poli = new PoligonoIrreg(7);

            System.out.println("Polígono antes de ordenar:");
            System.out.println(poli);

            // Ordenar por magnitud
            poli.ordenaVertices();

            System.out.println("\nPolígono después de ordenar por magnitud (de menor a mayor):");
            System.out.println(poli);

        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}