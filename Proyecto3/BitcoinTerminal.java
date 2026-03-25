//Proyecto 3        Nombre: Chavez Aquiagual Rodrigo    Grupo:7CM4
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BitcoinTerminal {
    private static double[] prices;
    private static int totalMinutes;
    private static AtomicInteger currentMinute = new AtomicInteger(0);
    
    private static double montoInversion = 0.0;
    private static boolean compraRealizada = false;
    private static boolean ventaRealizada = false;
    private static double precioCompra = 0.0;
    private static double cantidadBTC = 0.0;
    private static double comisionCompra = 0.0;
    private static double precioVenta = 0.0;
    private static double comisionVenta = 0.0;
    private static double dolaresRecibidos = 0.0;
    private static double gananciaTotal = 0.0;
    
    private static volatile boolean running = true;
    private static Thread actualizador;
    private static volatile boolean pausarActualizador = false;
    private static String fechaSimulacion; // fecha del dataset
    public static void main(String[] args) {
        cargarPrecios();
        cargarFecha();
        iniciarLectorMinuto();

        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
        try (Terminal terminal = terminalFactory.createTerminal();
             Screen screen = new TerminalScreen(terminal)) {

            screen.startScreen();
            screen.setCursorPosition(null);

            // Hilo de actualización de pantalla
            actualizador = new Thread(() -> {
                while (running) {
                    if (!pausarActualizador) {
                        try {
                            dibujarInterfaz(screen.newTextGraphics(), screen.getTerminalSize());
                            screen.refresh();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            actualizador.start();

            // Menú
            while (running) {
              
                esperarOpcion(screen);
            }

            actualizador.interrupt();
            screen.stopScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void esperarOpcion(Screen screen) throws IOException {
        while (true) {
            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.Character) {
                char c = key.getCharacter();
                if (c >= '1' && c <= '4') {
                    procesarOpcion(c - '0', screen);
                    break;
                }
            }
        }
    }

    private static void procesarOpcion(int opcion, Screen screen) throws IOException {
        switch (opcion) {
            case 1:
                if (compraRealizada) {
                    mostrarMensaje(screen, "Ya has realizado una compra. No puedes cambiar el monto.", 2);
                    return;
                }
                if (ventaRealizada) {
                    mostrarMensaje(screen, "La simulación ha terminado. Usa opción 4 para reiniciar.", 2);
                    return;
                }
                montoInversion = leerMonto(screen);
                break;
            case 2:
                if (montoInversion <= 0) {
                    mostrarMensaje(screen, "Primero debes ingresar un monto positivo.", 2);
                    return;
                }
                if (compraRealizada) {
                    mostrarMensaje(screen, "Ya has realizado una compra.", 2);
                    return;
                }
                if (ventaRealizada) {
                    mostrarMensaje(screen, "La simulación ha terminado. Usa opción 4 para reiniciar.", 2);
                    return;
                }
                realizarCompra();
                break;
            case 3:
                if (!compraRealizada) {
                    mostrarMensaje(screen, "Primero debes comprar Bitcoin.", 2);
                    return;
                }
                if (ventaRealizada) {
                    mostrarMensaje(screen, "Ya has vendido. Usa opción 4 para reiniciar.", 2);
                    return;
                }
                realizarVenta();
                break;
            case 4:
                reiniciarSimulacion();
                mostrarMensaje(screen, "Simulación reiniciada.", 2);
                break;
        }
    }

    private static double leerMonto(Screen screen) throws IOException {
        pausarActualizador = true;
        StringBuilder input = new StringBuilder();
        boolean ingresando = true;

        dibujarInterfaz(screen.newTextGraphics(), screen.getTerminalSize());
        screen.refresh();

        while (ingresando) {
            TextGraphics tg = screen.newTextGraphics();
            tg.setForegroundColor(TextColor.ANSI.YELLOW);
            tg.putString(2, 26, "                                             ");
            tg.putString(2, 26, "Ingrese monto (máx 100,000): " + input.toString() + "_");
            screen.refresh();

            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.Character) {
                char c = key.getCharacter();
                if (Character.isDigit(c) || c == '.') {
                    input.append(c);
                }
            } else if (key.getKeyType() == KeyType.Backspace) {
                if (input.length() > 0) {
                    input.deleteCharAt(input.length() - 1);
                }
            } else if (key.getKeyType() == KeyType.Enter) {
                try {
                    double monto = Double.parseDouble(input.toString());
                    if (monto > 0 && monto <= 100000) {
                        ingresando = false;
                    } else {
                        mostrarMensaje(screen, "Monto debe ser entre 0.01 y 100,000", 26);
                        input = new StringBuilder();
                    }
                } catch (NumberFormatException e) {
                    mostrarMensaje(screen, "Número inválido", 26);
                    input = new StringBuilder();
                }
            }
        }

        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(TextColor.ANSI.YELLOW);
        tg.putString(2, 26, "                                             ");
        screen.refresh();

        pausarActualizador = false; 
        return Double.parseDouble(input.toString());
    }

    private static void realizarCompra() {
        int min = currentMinute.get();
        precioCompra = prices[min];
        comisionCompra = montoInversion * 0.015;
        double montoNeto = montoInversion - comisionCompra;
        cantidadBTC = montoNeto / precioCompra;
        compraRealizada = true;
    }

    private static void realizarVenta() {
        int min = currentMinute.get();
        precioVenta = prices[min];
        double valorBruto = cantidadBTC * precioVenta;
        comisionVenta = valorBruto * 0.015;
        dolaresRecibidos = valorBruto - comisionVenta;
        double costoTotalCompra = montoInversion;
        gananciaTotal = dolaresRecibidos - costoTotalCompra;
        ventaRealizada = true;
    }

    private static void reiniciarSimulacion() {
        montoInversion = 0.0;
        compraRealizada = false;
        ventaRealizada = false;
        precioCompra = 0.0;
        cantidadBTC = 0.0;
        comisionCompra = 0.0;
        precioVenta = 0.0;
        comisionVenta = 0.0;
        dolaresRecibidos = 0.0;
        gananciaTotal = 0.0;
    }

    private static void mostrarMensaje(Screen screen, String mensaje, int linea) throws IOException {
        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(TextColor.ANSI.RED);
        tg.putString(2, linea, mensaje);
        screen.refresh();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
  
        tg.putString(2, linea, "                                             ");
        screen.refresh();
    }


    private static void cargarPrecios() {
        List<Double> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("datos.dat"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    list.add(Double.parseDouble(line));
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer datos.dat: " + e.getMessage());
            System.exit(1);
        }
        if (list.isEmpty()) {
            System.err.println("datos.dat vacío");
            System.exit(1);
        }
        totalMinutes = list.size();
        prices = new double[totalMinutes];
        for (int i = 0; i < totalMinutes; i++) {
            prices[i] = list.get(i);
        }
    }
    private static void cargarFecha() {
        try (BufferedReader br = new BufferedReader(new FileReader("fecha.txt"))) {
            fechaSimulacion = br.readLine().trim();
        } catch (IOException e) {
            // Fallback a fecha actual si no existe el archivo
            fechaSimulacion = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        }
    }
    private static void iniciarLectorMinuto() {
        Thread lector = new Thread(() -> {
            while (running) {
                try (BufferedReader br = new BufferedReader(new FileReader("minuto_actual.txt"))) {
                    String linea = br.readLine();
                    if (linea != null) {
                        int min = Integer.parseInt(linea.trim());
                        if (min >= 0 && min < totalMinutes) {
                            currentMinute.set(min);
                        }
                    }
                } catch (IOException | NumberFormatException e) {
                    // ignorar
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        lector.start();
    }

    private static void dibujarInterfaz(TextGraphics tg, TerminalSize size) {
        tg.setBackgroundColor(TextColor.ANSI.BLACK);
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.fill(' ');

        int min = currentMinute.get();
        double precioActual = prices[min];
        String precioStr = String.format("%,.2f", precioActual);
        int hora = min / 60;
        int minuto = min % 60;
        String horaStr = String.format("%02d:%02d", hora, minuto);
        String fechaStr = fechaSimulacion;
        tg.setForegroundColor(TextColor.ANSI.YELLOW);
        tg.putString(2, 1, "Precio del Bitcoin: USD$ " + precioStr);
        tg.putString(2, 2, "Fecha: " + fechaStr);
        tg.putString(2, 3, "Hora: " + horaStr);
        tg.putString(2, 4, String.format("Monto a invertir: USD$ %,.2f", montoInversion));

        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.putString(2, 6, "Desglose de la operación de COMPRA");
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        if (compraRealizada) {
            tg.putString(4, 7, String.format("Cantidad de dolares gastados: USD$ %,.2f", montoInversion));
            tg.putString(4, 8, String.format("Comisión (1.5%%): USD$ %,.2f", comisionCompra));
            tg.putString(4, 9, String.format("Precio del Bitcoin en el momento de la compra: USD$ %,.2f", precioCompra));
            tg.putString(4, 10, String.format("Bitcoin recibido: BTC$ %,.6f", cantidadBTC));
        } else {
            tg.putString(4, 7, "Cantidad de dolares gastados: USD$ 0.00");
            tg.putString(4, 8, "Comisión (1.5%): USD$ 0.00");
            tg.putString(4, 9, "Precio del Bitcoin en el momento de la compra: USD$ 0.00");
            tg.putString(4, 10, "Bitcoin recibido: BTC$ 0.00");
        }

        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.putString(2, 12, "Desglose de la operación de VENTA");
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        if (ventaRealizada) {
            tg.putString(4, 13, String.format("Cantidad de Bitcoin vendido: BTC$ %,.6f", cantidadBTC));
            tg.putString(4, 14, String.format("Comisión (1.5%%): USD$ %,.2f", comisionVenta));
            tg.putString(4, 15, String.format("Precio del Bitcoin en el momento de la venta: USD$ %,.2f", precioVenta));
            tg.putString(4, 16, String.format("Dolares recibidos: USD$ %,.2f", dolaresRecibidos));
        } else {
            tg.putString(4, 13, "Cantidad de Bitcoin vendido: BTC$ 0.00");
            tg.putString(4, 14, "Comisión (1.5%): USD$ 0.00");
            tg.putString(4, 15, "Precio del Bitcoin en el momento de la venta: USD$ 0.00");
            tg.putString(4, 16, "Dolares recibidos: USD$ 0.00");
        }

        tg.setForegroundColor(TextColor.ANSI.GREEN);
        tg.putString(2, 18, String.format("Total de ganancias: USD$ %,.2f", gananciaTotal));

        tg.setForegroundColor(TextColor.ANSI.MAGENTA);
        tg.putString(2, 20, "1.- Introducir el monto de inversión");
        tg.putString(2, 21, "2.- Iniciar la compra de Bitcoin");
        tg.putString(2, 22, "3.- Iniciar la venta de Bitcoin");
        tg.putString(2, 23, "4.- Reiniciar simulación");
        tg.setForegroundColor(TextColor.ANSI.YELLOW);
        tg.putString(2, 25, "Ingresa una opción: ");
    }
}