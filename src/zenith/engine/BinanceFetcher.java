package zenith.engine;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import zenith.models.Candle;

public class BinanceFetcher {
    private static final HttpClient client = HttpClient.newHttpClient();

    public static List<String> getAllUsdtPairs() {
        List<String> symbols = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.binance.com/api/v3/ticker/price"))
                    .GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            Matcher m = Pattern.compile("\"symbol\":\"([A-Z0-9]+USDT)\"").matcher(response.body());
            while (m.find()) {
                symbols.add(m.group(1));
            }
        } catch (Exception e) {
            System.out.println("[API ERROR] Gagal ambil list coin: " + e.getMessage());
        }
        return symbols;
    }

    public static List<Candle> getRealCandles(String symbol, String interval) {
        List<Candle> klines = new ArrayList<>();
        try {
            String url = "https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=" + interval + "&limit=100";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            Matcher m = Pattern.compile("\\[(\\d+),\"([^\"]+)\",\"([^\"]+)\",\"([^\"]+)\",\"([^\"]+)\"").matcher(response.body());
            while (m.find()) {
                long time = Long.parseLong(m.group(1));
                double open = Double.parseDouble(m.group(2));
                double high = Double.parseDouble(m.group(3));
                double low = Double.parseDouble(m.group(4));
                double close = Double.parseDouble(m.group(5));

                Candle c = new Candle(open, time);
                c.updatePrice(high);
                c.updatePrice(low);
                c.updatePrice(close);
                klines.add(c);
            }
        } catch (Exception e) {
            System.out.println("[API ERROR] Gagal narik chart: " + e.getMessage());
        }
        return klines;
    }

    public static double getCurrentPrice(String symbol) {
        try {
            String url = "https://api.binance.com/api/v3/ticker/price?symbol=" + symbol;
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            Matcher m = Pattern.compile("\"price\":\"([^\"]+)\"").matcher(response.body());
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
        } catch (Exception e) {
        }
        return -1.0;
    }
}
