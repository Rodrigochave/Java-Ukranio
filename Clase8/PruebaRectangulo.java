public class PruebaRectangulo {
    public static void main(String[] args) {
        try {
            Coordenada supIzq = new Coordenada(2, 3);
            Coordenada infDer = new Coordenada(5, 1);

            Rectangulo rect2 = new Rectangulo(supIzq, infDer);

            System.out.println("\nRectángulo usando constructor con objetos Coordenada:");
            System.out.println(rect2);
            System.out.println("Área: " + rect2.area());
        } catch (IllegalArgumentException e) {
            System.out.println("Error al crear el objeto Rectangulo: " + e.getMessage());
        }
    }
}