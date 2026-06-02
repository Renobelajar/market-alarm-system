package zenith.engine;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TelegramNotifier {
    private static final String BOT_TOKEN = "rahasia anjir ke leak semoga ga ke leak sialan gue lupa hide";
    private static final String CHAT_ID = "@botmarketwr100";

    public static void sendAlertAsync(String message) {
        try {
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString());
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN +
                               "/sendMessage?chat_id=" + CHAT_ID + "&text=" + encodedMessage;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            System.err.println("[TELEGRAM ERROR] Gagal kirim notif: " + e.getMessage());
        }
    }
}
