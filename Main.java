class Calculadora {

    int suma(int a, int b) {
        return a + b;
    }

    double suma(double a, double b) {
        return a + b;
    }
}

public class Main {
    public static void main(String[] args) {

        Calculadora calc = new Calculadora();

        int resultado1 = calc.suma(5, 3);
        double resultado2 = calc.suma(2.5, 3.7);

        System.out.println("Suma enteros: " + resultado1);
        System.out.println("Suma decimales: " + resultado2);
    }
}
