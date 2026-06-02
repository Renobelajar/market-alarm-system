package zenith.models;

import java.util.ArrayList;
import java.util.List;
import java.awt.Toolkit;
import zenith.database.AssetDAO;
import zenith.engine.BinanceFetcher;
import zenith.engine.TelegramNotifier;

public abstract class Asset {
    private String symbol;
    private double currentPrice;
    private List<Candle> chartData;
    private List<TrendLine> drawnLines;

    private int tickCount = 0;
    private long currentTimeframeMs;

    private List<Double> activeAlerts;
    private AssetDAO daoHelper;

    public Asset(String symbol, double startPrice) {
        this.symbol = symbol;
        this.currentPrice = startPrice;
        this.chartData = new ArrayList<>();
        this.drawnLines = new ArrayList<>();

        this.daoHelper = new AssetDAO();
        this.activeAlerts = daoHelper.getActiveAlerts(symbol);

        changeTimeframe(60000L);
    }

    protected abstract double getVolatility();

    private String getBinanceInterval(long ms) {
        if (ms <= 60000L) return "1m";
        if (ms == 300000L) return "5m";
        if (ms == 900000L) return "15m";
        if (ms == 3600000L) return "1h";
        if (ms == 14400000L) return "4h";
        if (ms == 86400000L) return "1d";
        if (ms == 604800000L) return "1w";
        return "1M";
    }

    public synchronized void changeTimeframe(long newTimeframeMs) {
        this.currentTimeframeMs = newTimeframeMs;
        this.tickCount = 0;

        String interval = getBinanceInterval(newTimeframeMs);
        List<Candle> realData = BinanceFetcher.getRealCandles(symbol, interval);

        if (realData != null && !realData.isEmpty()) {
            this.chartData = realData;
            this.currentPrice = chartData.get(chartData.size() - 1).getClose();
        }
    }

    public synchronized void simulateTick() {
        double livePrice = BinanceFetcher.getCurrentPrice(symbol);
        if (livePrice <= 0) return;

        this.currentPrice = livePrice;

        List<Double> hitAlerts = new ArrayList<>();
        for (Double alertPrice : activeAlerts) {
            double toleransi = alertPrice * 0.0005;
            if (Math.abs(this.currentPrice - alertPrice) <= toleransi) {
                Toolkit.getDefaultToolkit().beep();

                String msg = "🚨 ZENITH LIVE ALERT 🚨\n\n" +
                             "Pair: " + symbol + "\n" +
                             "Status: POI Reached!\n" +
                             "Live Price: " + String.format("%.4f", currentPrice);
                TelegramNotifier.sendAlertAsync(msg);

                hitAlerts.add(alertPrice);
                daoHelper.deactivateAlert(symbol, alertPrice);
            }
        }
        activeAlerts.removeAll(hitAlerts);

        if (!chartData.isEmpty()) {
            Candle currentCandle = chartData.get(chartData.size() - 1);
            currentCandle.updatePrice(this.currentPrice);
        }

        tickCount++;
        if (tickCount >= 10) {
            changeTimeframe(currentTimeframeMs);
            tickCount = 0;
        }
    }

    public synchronized void addAlert(double targetPrice) {
        this.activeAlerts.add(targetPrice);
        daoHelper.saveAlert(symbol, targetPrice);
    }
    public synchronized void removeAlert(double targetPrice) {
        this.activeAlerts.remove(targetPrice);
        daoHelper.deleteAlert(symbol, targetPrice);
    }
    public synchronized List<Double> getActiveAlerts() { return new ArrayList<>(activeAlerts); }
    public synchronized double getCurrentPrice() { return currentPrice; }
    public String getSymbol() { return symbol; }
    public synchronized List<Candle> getChartData() { return new ArrayList<>(chartData); }
    public long getCurrentTimeframeMs() { return currentTimeframeMs; }
    public List<TrendLine> getDrawnLines() { return drawnLines; }
}
