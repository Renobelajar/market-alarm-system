package zenith.models;
public class Forex extends Asset {
    public Forex(String symbol, double startPrice) { super(symbol, startPrice); }
    @Override protected double getVolatility() { return 2.5; }
}