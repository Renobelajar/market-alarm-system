package zenith.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static Connection connection;
    private static final String URL = "jdbc:mysql://localhost:3306/db_zenith";
    private static final String USER = "root"; // Sesuaikan dengan user DB
    private static final String PASSWORD = ""; // Sesuaikan dengan password DB

    // Enkapsulasi & Singleton: Hanya satu koneksi yang dibuat
    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("[DB] Terhubung ke MySQL!");
            } catch (SQLException e) {
                System.err.println("[DB ERROR] Koneksi gagal: " + e.getMessage());
            }
        }
        return connection;
    }
}