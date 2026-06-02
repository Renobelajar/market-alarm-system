package zenith.models;
public class Crypto extends Asset {
    public Crypto(String symbol, double startPrice) { super(symbol, startPrice); }
    @Override protected double getVolatility() { return 50.0; }
}
