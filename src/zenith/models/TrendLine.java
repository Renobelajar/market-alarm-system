package zenith.models;

public class TrendLine {
    public long startTimestamp;
    public double startPrice;
    public long endTimestamp;
    public double endPrice;

    public TrendLine(long startTimestamp, double startPrice, long endTimestamp, double endPrice) {
        this.startTimestamp = startTimestamp;
        this.startPrice = startPrice;
        this.endTimestamp = endTimestamp;
        this.endPrice = endPrice;
    }
}
