package zenith.database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {
    private static Connection conn;

    public static Connection getConnection() {
        if (conn == null) {
            try {
                String url = "jdbc:mysql://localhost:3306/db_zenith";
                String user = "root";
                String pass = "";

                Class.forName("com.mysql.cj.jdbc.Driver");

                conn = DriverManager.getConnection(url, user, pass);
                System.out.println("[DB] Terhubung ke MySQL!");
            } catch (Exception e) {
                System.out.println("[DB ERROR] Koneksi gagal: " + e.getMessage());
            }
        }
        return conn;
    }
}
