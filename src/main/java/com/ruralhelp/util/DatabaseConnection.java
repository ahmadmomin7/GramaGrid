package com.ruralhelp.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages JDBC connection lifecycle and database connectivity validation.
 */
public class DatabaseConnection {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found in classpath: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        DatabaseConfig.loadConfig();
        return DriverManager.getConnection(
                DatabaseConfig.getUrl(),
                DatabaseConfig.getUser(),
                DatabaseConfig.getPassword()
        );
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static String getDiagnostics() {
        try (Connection conn = getConnection()) {
            return "SUCCESS: Connected to " + conn.getCatalog() + " as " + DatabaseConfig.getUser();
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage() + " (SQLState: " + e.getSQLState() + ")";
        }
    }

    public static void closeQuietly(AutoCloseable... closeables) {
        if (closeables == null) return;
        for (AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {}
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Testing Database Connection...");
        System.out.println("Diagnostics: " + getDiagnostics());
        if (testConnection()) {
            System.out.println("DATABASE CONNECTION TEST PASSED SUCCESSFULLY!");
        } else {
            System.err.println("DATABASE CONNECTION TEST FAILED!");
        }
    }
}
