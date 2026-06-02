package zenith.main;

import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        // Mengubah desain tombol & windows agar mengikuti tema OS bawaan laptop (tidak kaku)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Gagal memuat Look and Feel native.");
        }

        // Membuka Dashboard Zenith secara aman
        java.awt.EventQueue.invokeLater(() -> {
            ZenithDashboard dashboard = new ZenithDashboard();
            dashboard.setVisible(true);
            
            // Otomatis mengambil data harga koin saat pertama kali aplikasi terbuka
            dashboard.updateDashboardData();
        });
    }
}