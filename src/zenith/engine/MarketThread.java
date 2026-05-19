package zenith.engine;
import zenith.main.CandlestickPanel;
import zenith.models.Asset;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import javax.swing.SwingUtilities;

public class MarketThread implements Runnable {
    private List<Asset> marketAssets;
    private DefaultTableModel tableModel;
    private CandlestickPanel chartPanel;
    private boolean isRunning = true;

    public MarketThread(List<Asset> assets, DefaultTableModel model, CandlestickPanel chartPanel) {
        this.marketAssets = assets;
        this.tableModel = model;
        this.chartPanel = chartPanel;
    }

    public void stopEngine() { this.isRunning = false; }

    @Override
    public void run() {
        while (isRunning) {
            try {
                for (int i = 0; i < marketAssets.size(); i++) {
                    Asset asset = marketAssets.get(i);
                    asset.simulateTick(); // Sudah nggak butuh parameter, otomatis dari dalam kelas!
                    
                    final int rowIndex = i;
                    final double newPrice = asset.getCurrentPrice();
                    
                    SwingUtilities.invokeLater(() -> {
                        tableModel.setValueAt(String.format("%.2f", newPrice), rowIndex, 2);
                        if (chartPanel != null) chartPanel.repaint();
                    });
                }
                Thread.sleep(200); // 1 Tick = 200 ms
            } catch (InterruptedException e) {
                System.out.println("[ERROR] Thread terhenti.");
            }
        }
    }
}