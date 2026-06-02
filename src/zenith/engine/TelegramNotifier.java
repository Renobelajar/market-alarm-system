package zenith.engine;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TelegramNotifier {
    // TODO: Ganti pakai Token Bot & Chat ID Telegram lo!
    private static final String BOT_TOKEN = "TOKEN_BOT_DARI_BOTFATHER"; 
    private static final String CHAT_ID = "CHAT_ID_AKUN_LU";

    public static void sendAlertAsync(String message) {
        try {
            // Encode pesan biar spasi dan enter aman di URL
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString());
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + 
                               "/sendMessage?chat_id=" + CHAT_ID + "&text=" + encodedMessage;
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .GET()
                    .build();
            
            // Kirim secara background (Async) biar chart nggak nge-freeze!
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            
        } catch (Exception e) {
            System.err.println("[TELEGRAM ERROR] Gagal kirim notif: " + e.getMessage());
        }
    }
}