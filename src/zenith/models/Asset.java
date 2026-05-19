package zenith.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JOptionPane;
import java.awt.Toolkit;

public abstract class Asset {
    private String symbol;
    private double currentPrice;
    private List<Candle> chartData;
    
    private int tickCount = 0;
    private double targetAlertPrice = 0.0;
    private boolean isAlertActive = false;
    private long currentTimeframeMs; // Menyimpan TF saat ini

    public Asset(String symbol, double startPrice) {
        this.symbol = symbol;
        this.currentPrice = startPrice;
        this.chartData = new ArrayList<>();
        // Default awal 5 Detik (5000 milidetik)
        changeTimeframe(5000); 
    }

    protected abstract double getVolatility();

    // FITUR BARU: Regenerasi History saat TF diganti
    public synchronized void changeTimeframe(long newTimeframeMs) {
        this.currentTimeframeMs = newTimeframeMs;
        this.tickCount = 0;
        this.chartData.clear();
        
        long currentTime = System.currentTimeMillis();
        double tempPrice = this.currentPrice;
        Random rand = new Random();
        
        // MATEMATIKA DEWA: Skala Volatilitas berdasarkan Waktu (Random Walk Theory)
        // Candle 1 Jam pasti bentuk harganya lebih besar dari 1 Menit
        double tfMultiplier = Math.sqrt(newTimeframeMs / 5000.0); 

        for (int i = 100; i >= 1; i--) {
            tempPrice += (rand.nextDouble() - 0.5) * getVolatility() * tfMultiplier;
            Candle pastCandle = new Candle(tempPrice, currentTime - (i * newTimeframeMs));
            
            pastCandle.updatePrice(tempPrice + (rand.nextDouble() * getVolatility() * tfMultiplier)); // High
            pastCandle.updatePrice(tempPrice - (rand.nextDouble() * getVolatility() * tfMultiplier)); // Low
            pastCandle.updatePrice(tempPrice + (rand.nextDouble() - 0.5) * getVolatility() * tfMultiplier); // Close
            
            this.chartData.add(pastCandle);
        }
        // Sinkronisasi harga terkini agar tidak loncat
        this.currentPrice = chartData.get(chartData.size() - 1).getClose();
    }

    public synchronized void simulateTick() {
        Random rand = new Random();
        double tfMultiplier = Math.sqrt(currentTimeframeMs / 5000.0);
        
        // Volatilitas tick real-time lebih kecil biar pergerakannya mulus
        double change = (rand.nextDouble() - 0.5) * (getVolatility() * 0.2);
        this.currentPrice += change;

        if (chartData.isEmpty()) {
            chartData.add(new Candle(this.currentPrice, System.currentTimeMillis()));
        }

        Candle currentCandle = chartData.get(chartData.size() - 1);
        currentCandle.updatePrice(this.currentPrice);

        if (isAlertActive && Math.abs(this.currentPrice - targetAlertPrice) < (getVolatility() * 1.5)) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(null, 
                "🚨 ALERT SMC!\n" + symbol + " menyentuh area " + String.format("%.2f", targetAlertPrice), 
                "Order Block Hit", JOptionPane.WARNING_MESSAGE);
            isAlertActive = false; 
        }

        tickCount++;
        // 1 Tick = 200ms. Hitung butuh berapa tick untuk 1 candle
        long targetTicks = currentTimeframeMs / 200; 
        if (tickCount >= targetTicks) {
            chartData.add(new Candle(this.currentPrice, System.currentTimeMillis()));
            if (chartData.size() > 500) chartData.remove(0); 
            tickCount = 0;
        }
    }

    public synchronized double getCurrentPrice() { return currentPrice; }
    public String getSymbol() { return symbol; }
    public synchronized List<Candle> getChartData() { return chartData; }
    public long getCurrentTimeframeMs() { return currentTimeframeMs; }
    public synchronized void setAlert(double targetPrice) { this.targetAlertPrice = targetPrice; this.isAlertActive = true; }
}