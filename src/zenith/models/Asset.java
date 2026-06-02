package zenith.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.Toolkit;
import zenith.database.AssetDAO;
import zenith.engine.TelegramNotifier; // Pastikan file TelegramNotifier udah lu buat

public abstract class Asset {
    private String symbol;
    private double currentPrice;
    private List<Candle> chartData;
    private List<TrendLine> drawnLines; // Nyimpen coretan garis lu
    
    private int tickCount = 0;
    private long currentTimeframeMs; 
    
    // Fitur Notifikasi & Database
    private List<Double> activeAlerts;
    private AssetDAO daoHelper;

    public Asset(String symbol, double startPrice) {
        this.symbol = symbol;
        this.currentPrice = startPrice;
        this.chartData = new ArrayList<>();
        this.drawnLines = new ArrayList<>();
        
        // Inisialisasi Database dan Load Alert dari MySQL
        this.daoHelper = new AssetDAO();
        this.activeAlerts = daoHelper.getActiveAlerts(symbol);
        
        // Setup chart awal (Default 5 Detik)
        changeTimeframe(5000L); 
    }

    // Abstract method biar tiap pair punya volatilitas beda
    protected abstract double getVolatility();

    // ----------------------------------------------------
    // FITUR: Ganti Timeframe & Regenerasi Masa Lalu (SMC)
    // ----------------------------------------------------
// ----------------------------------------------------
    // FITUR: Ganti Timeframe & Regenerasi Masa Lalu (FIXED)
    // ----------------------------------------------------
    public synchronized void changeTimeframe(long newTimeframeMs) {
        this.currentTimeframeMs = newTimeframeMs;
        this.tickCount = 0; // Reset mesin tik
        this.chartData.clear();
        
        long currentTime = System.currentTimeMillis();
        double tempPrice = this.currentPrice;
        Random rand = new Random();
        
        double tfMultiplier = Math.sqrt(newTimeframeMs / 5000.0); 

        // PERBAIKAN BUG: Looping sekarang turun sampai i >= 0 (Bukan 1)
        // i = 0 artinya candle terakhir yang dibuat punya waktu "Sekarang" (Current Time)
        for (int i = 100; i >= 0; i--) { 
            tempPrice += (rand.nextDouble() - 0.5) * getVolatility() * tfMultiplier;
            Candle pastCandle = new Candle(tempPrice, currentTime - (i * newTimeframeMs));
            
            pastCandle.updatePrice(tempPrice + (rand.nextDouble() * getVolatility() * tfMultiplier)); // High
            pastCandle.updatePrice(tempPrice - (rand.nextDouble() * getVolatility() * tfMultiplier)); // Low
            pastCandle.updatePrice(tempPrice + (rand.nextDouble() - 0.5) * getVolatility() * tfMultiplier); // Close
            
            this.chartData.add(pastCandle);
        }
        this.currentPrice = chartData.get(chartData.size() - 1).getClose();
    }

    // ----------------------------------------------------
    // FITUR: Mesin Pergerakan Harga & Pemicu Telegram
    // ----------------------------------------------------
    public synchronized void simulateTick() {
        Random rand = new Random();
        
        // Volatilitas real-time dibuat halus
        double change = (rand.nextDouble() - 0.5) * (getVolatility() * 0.2);
        this.currentPrice += change;

        if (chartData.isEmpty()) {
            chartData.add(new Candle(this.currentPrice, System.currentTimeMillis()));
        }

        Candle currentCandle = chartData.get(chartData.size() - 1);
        currentCandle.updatePrice(this.currentPrice);

        // --- Cek Alarm SMC ---
        List<Double> hitAlerts = new ArrayList<>();
        for (Double alertPrice : activeAlerts) {
            // Kalau area Liquidity/OB kesapu
            if (Math.abs(this.currentPrice - alertPrice) < (getVolatility() * 1.5)) {
                Toolkit.getDefaultToolkit().beep(); // Bunyi di laptop
                
                String msg = "🚨 ZENITH SYSTEM ALERT 🚨\n\n" +
                             "Pair: " + symbol + "\n" +
                             "Status: POI Reached / Liquidity Sweep Area Hit!\n" +
                             "Price: " + String.format("%.2f", alertPrice);
                TelegramNotifier.sendAlertAsync(msg); // Tembak ke HP via Bot
                
                hitAlerts.add(alertPrice); // Tandai alert yang udah kena
                daoHelper.deactivateAlert(symbol, alertPrice); // Matikan di DB
            }
        }
        activeAlerts.removeAll(hitAlerts); // Bersihin dari layar

        // --- Logic Ganti Candle ---
        tickCount++;
        long targetTicks = currentTimeframeMs / 200; // Asumsi 1 tick engine = 200ms
        if (tickCount >= targetTicks) {
            chartData.add(new Candle(this.currentPrice, System.currentTimeMillis()));
            if (chartData.size() > 500) chartData.remove(0); 
            tickCount = 0;
        }
    }
    
    // FITUR BARU: Hapus Alert
    public synchronized void removeAlert(double targetPrice) {
        this.activeAlerts.remove(targetPrice);
        daoHelper.deleteAlert(symbol, targetPrice); // Hapus permanen dari MySQL
    }

    // ----------------------------------------------------
    // GETTERS & SETTERS
    // ----------------------------------------------------
    public synchronized void addAlert(double targetPrice) {
        this.activeAlerts.add(targetPrice);
        daoHelper.saveAlert(symbol, targetPrice); 
    }

    // Bikin copy list biar nggak crash pas GUI & Engine baca/nulis bersamaan
    public synchronized List<Double> getActiveAlerts() { return new ArrayList<>(activeAlerts); }
    public synchronized double getCurrentPrice() { return currentPrice; }
    public String getSymbol() { return symbol; }
    public synchronized List<Candle> getChartData() { return new ArrayList<>(chartData); }
    public long getCurrentTimeframeMs() { return currentTimeframeMs; }
    public List<TrendLine> getDrawnLines() { return drawnLines; }
}