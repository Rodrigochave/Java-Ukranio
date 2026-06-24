package com.mycompany.app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class App {

    // Datos comunes (cambia si tu réplica usa otra contraseña/usuario)
    private static final String DATABASE_NAME = "Biblioteca";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "Tortugo_666_20";

    // Instancias de Cloud SQL
    private static final String PRIMARY_INSTANCE = "servicio-pub-sub:us-central1:mysql-master";
    private static final String SECONDARY_INSTANCE = "servicio-pub-sub:us-central1:mysql-master-replica";
    public static void main(String[] args) {

        System.out.println("=== CONECTANDO A BASE DE DATOS PRIMARIA ===\n");
        try (Connection connPrimary = getConnection(PRIMARY_INSTANCE)) {
            printUsers(connPrimary, "PRIMARIA");
        } catch (SQLException e) {
            System.err.println("Error conectando a la primaria: " + e.getMessage());
        }

        System.out.println("\n=== CONECTANDO A BASE DE DATOS SECUNDARIA (RÉPLICA) ===\n");
        try (Connection connSecondary = getConnection(SECONDARY_INSTANCE)) {
            printUsers(connSecondary, "SECUNDARIA");
        } catch (SQLException e) {
            System.err.println("Error conectando a la secundaria: " + e.getMessage());
        }
    }

    /**
     * Crea una conexión a una instancia de Cloud SQL usando el conector socket.
     */
    private static Connection getConnection(String instanceConnectionName) throws SQLException {
        String jdbcUrl = String.format(
            "jdbc:mysql://google/%s?cloudSqlInstance=%s&socketFactory=com.google.cloud.sql.mysql.SocketFactory&useSSL=false",
            DATABASE_NAME,
            instanceConnectionName
        );
        return DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD);
    }

    /**
     * Ejecuta SELECT * FROM Usuarios y muestra los resultados.
     * @param conn Conexión activa
     * @param origen Texto para identificar si es primaria o secundaria
     */
    private static void printUsers(Connection conn, String origen) throws SQLException {
        String sql = "SELECT id_usuario, nombre, correo FROM Usuarios";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("--- Usuarios en " + origen + " ---");
            boolean hayRegistros = false;
            while (rs.next()) {
                hayRegistros = true;
                System.out.printf("ID: %d | Usuario: %s | Email: %s%n",
                        rs.getInt("id_usuario"),
                        rs.getString("nombre"),
                        rs.getString("correo"));
            }
            if (!hayRegistros) {
                System.out.println("(No hay registros en la tabla Usuarios)");
            }
        }
    }
}