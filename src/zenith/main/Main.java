package zenith.main;

import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Gagal memuat Look and Feel native.");
        }

        java.awt.EventQueue.invokeLater(() -> {
            ZenithDashboard dashboard = new ZenithDashboard();
            dashboard.setVisible(true);
            
            dashboard.updateDashboardData();
        });
    }
}