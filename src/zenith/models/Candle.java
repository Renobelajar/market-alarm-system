package zenith.models;

public class Candle {
    private double open, high, low, close;
    private long timestamp; // Menyimpan waktu candle

    public Candle(double openPrice, long timestamp) {
        this.open = openPrice;
        this.high = openPrice;
        this.low = openPrice;
        this.close = openPrice;
        this.timestamp = timestamp;
    }

    public void updatePrice(double currentPrice) {
        this.close = currentPrice;
        if (currentPrice > this.high) this.high = currentPrice;
        if (currentPrice < this.low) this.low = currentPrice;
    }

    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getClose() { return close; }
    public long getTimestamp() { return timestamp; }
    public boolean isBullish() { return close >= open; }
}