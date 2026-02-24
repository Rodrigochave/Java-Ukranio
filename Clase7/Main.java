public class Main {
    public static void main(String[] args) {

        TrianguloEq t = new TrianguloEq(new Coordenada(0, 0), 4);
        Rectangulo r = new Rectangulo(new Coordenada(5, 5), 6, 3);

        System.out.println("=== ANTES DE DESPLAZAR ===");

        System.out.println("Area triangulo: " + t.area());
        System.out.println("Vertices triangulo:");
        t.mostrarVertices();

        System.out.println("\nArea rectangulo: " + r.area());
        System.out.println("Vertices rectangulo:");
        r.mostrarVertices();

        // desplazar figuras
        t.desplazar(2, 3);
        r.desplazar(-1, 2);

        System.out.println("\n=== DESPUES DE DESPLAZAR ===");

        System.out.println("Area triangulo: " + t.area());
        System.out.println("Vertices triangulo:");
        t.mostrarVertices();

        System.out.println("\nArea rectangulo: " + r.area());
        System.out.println("Vertices rectangulo:");
        r.mostrarVertices();
    }
}
