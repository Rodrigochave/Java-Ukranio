package com.mycompany.app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

public class App {

    public static void main(String[] args) {

        // Datos de conexión
        String instanceConnectionName = "servicio-pub-sub:us-central1:mysql-master";
        String databaseName = "Biblioteca";
        String username = "root";
        String password = "Tortugo_666_20";
        
        // URL JDBC
        String jdbcUrl = String.format(
            "jdbc:mysql://google/%s?cloudSqlInstance=%s&socketFactory=com.google.cloud.sql.mysql.SocketFactory&useSSL=false",
            databaseName,
            instanceConnectionName
        );

        Scanner scanner = new Scanner(System.in);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {

            // =========================================
            // MOSTRAR USUARIOS ACTUALES
            // =========================================

            System.out.println("=== USUARIOS ACTUALES ===\n");

            String selectSQL =
                "SELECT id_usuario, nombre, correo FROM Usuarios";

            PreparedStatement selectStmt =
                conn.prepareStatement(selectSQL);

            ResultSet rs = selectStmt.executeQuery();

            while (rs.next()) {

                System.out.println(
                    "ID: " + rs.getInt("id_usuario")
                    + " | Usuario: " + rs.getString("nombre")
                    + " | Email: " + rs.getString("correo")
                );
            }

            rs.close();
            selectStmt.close();

            // =========================================
            // CAPTURAR DATOS DESDE CONSOLA
            // =========================================

            System.out.println("\n=== NUEVO USUARIO ===");

            System.out.print("Ingresa el ID del usuario: ");
            int id = scanner.nextInt();

            scanner.nextLine();

            System.out.print("Ingresa el nombre: ");
            String nombre = scanner.nextLine();

            System.out.print("Ingresa el correo: ");
            String correo = scanner.nextLine();

            // =========================================
            // INSERTAR USUARIO
            // =========================================

            String insertarSQL =
                "INSERT INTO Usuarios(id_usuario, nombre, correo) VALUES (?, ?, ?)";

            PreparedStatement insertStmt =
                conn.prepareStatement(insertarSQL);

            insertStmt.setInt(1, id);
            insertStmt.setString(2, nombre);
            insertStmt.setString(3, correo);

            insertStmt.executeUpdate();

            insertStmt.close();

            System.out.println("\nUsuario insertado correctamente.");

            // =========================================
            // MOSTRAR TABLA ACTUALIZADA
            // =========================================

            System.out.println("\n=== TABLA ACTUALIZADA ===\n");

            PreparedStatement nuevoSelect =
                conn.prepareStatement(selectSQL);

            ResultSet rs2 = nuevoSelect.executeQuery();

            while (rs2.next()) {

                System.out.println(
                    "ID: " + rs2.getInt("id_usuario")
                    + " | Usuario: " + rs2.getString("nombre")
                    + " | Email: " + rs2.getString("correo")
                );
            }

            rs2.close();
            nuevoSelect.close();

            scanner.close();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}