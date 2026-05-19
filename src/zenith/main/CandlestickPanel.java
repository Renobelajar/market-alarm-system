package zenith.main;

import zenith.models.Asset;
import zenith.models.Candle;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CandlestickPanel extends JPanel {
    private Asset asset;
    
    // Konfigurasi Navigasi Chart
    private int zoomLevel = 8; // Lebar candle
    private int offsetX = 0; // Geser Kanan/Kiri
    private int offsetY = 0; // Geser Atas/Bawah
    private int lastMouseX, lastMouseY;
    
    // Margin untuk Sumbu Harga dan Waktu
    private final int RIGHT_MARGIN = 70;
    private final int BOTTOM_MARGIN = 30;

    public CandlestickPanel() {
        setBackground(new Color(18, 20, 24)); // Warna khas TradingView/MT4 Dark
        
        // Fitur Zoom In/Out pake Scroll Mouse
        addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0) {
                zoomLevel += 2; // Scroll up = Zoom In
            } else {
                zoomLevel -= 2; // Scroll down = Zoom Out
            }
            if (zoomLevel < 2) zoomLevel = 2; // Batas mentok kecil
            if (zoomLevel > 50) zoomLevel = 50; // Batas mentok gede
            repaint();
        });

        // Fitur Geser (Pan/Drag) pake klik & tahan
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastMouseX;
                int dy = e.getY() - lastMouseY;
                offsetX += dx;
                offsetY += dy;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                repaint();
            }
        });
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
        // Reset posisi view setiap ganti pair/aset
        this.offsetX = 0;
        this.offsetY = 0;
    }
    
    public void resetView() {
        this.offsetX = 0;
        this.offsetY = 0;
        this.zoomLevel = 8; // Balikin ukuran candle ke normal
        repaint(); // Paksa gambar ulang
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (asset == null || asset.getChartData().isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g;
        // Bikin garis chart lebih mulus (Anti-aliasing)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<Candle> candles = asset.getChartData();
        int width = getWidth();
        int height = getHeight();
        
        // Area menggambar chart (dikurangi margin agar tidak nabrak teks)
        int chartWidth = width - RIGHT_MARGIN;
        int chartHeight = height - BOTTOM_MARGIN;

        // 1. CARI HARGA MIN & MAX (Untuk skala Y)
        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;
        for (Candle c : candles) {
            if (c.getLow() < minPrice) minPrice = c.getLow();
            if (c.getHigh() > maxPrice) maxPrice = c.getHigh();
        }
        double priceRange = maxPrice - minPrice;
        if (priceRange == 0) priceRange = 1;

        // 2. GAMBAR BACKGROUND GRID (Kotak-kotak tabel)
        g2.setColor(new Color(40, 44, 52)); // Warna Grid
        // Grid Horizontal
        for (int i = 0; i < chartHeight; i += 40) {
            g2.drawLine(0, i, chartWidth, i);
        }
        // Grid Vertical
        for (int i = chartWidth; i > 0; i -= 60) {
            g2.drawLine(i, 0, i, chartHeight);
        }

        // 3. BATASI AREA GAMBAR (Kliping) AGAR CANDLE TIDAK KELUAR BATAS
        Shape originalClip = g2.getClip();
        g2.clipRect(0, 0, chartWidth, chartHeight);

        // 4. MENGGAMBAR CANDLESTICK
        int spacing = zoomLevel + 2;
        // Posisi X candle paling baru (paling kanan)
        int xPos = chartWidth - spacing + offsetX; 

        for (int i = candles.size() - 1; i >= 0; i--) {
            Candle c = candles.get(i);
            
            // Skip gambar jika candle sudah di luar layar kiri/kanan (Optimasi Memori)
            if (xPos + zoomLevel < 0 || xPos > chartWidth) {
                xPos -= spacing;
                continue; 
            }

            // Rumus posisi Y (ditambah offsetY dari hasil geser mouse)
            int yHigh = chartHeight - (int) (((c.getHigh() - minPrice) / priceRange) * chartHeight) + offsetY;
            int yLow = chartHeight - (int) (((c.getLow() - minPrice) / priceRange) * chartHeight) + offsetY;
            int yOpen = chartHeight - (int) (((c.getOpen() - minPrice) / priceRange) * chartHeight) + offsetY;
            int yClose = chartHeight - (int) (((c.getClose() - minPrice) / priceRange) * chartHeight) + offsetY;

            if (c.isBullish()) {
                g2.setColor(new Color(38, 166, 154)); // Hijau
            } else {
                g2.setColor(new Color(239, 83, 80)); // Merah
            }

            // Gambar Sumbu (Wick)
            g2.drawLine(xPos + zoomLevel / 2, yHigh, xPos + zoomLevel / 2, yLow);

            // Gambar Body Candle
            int bodyY = Math.min(yOpen, yClose);
            int bodyHeight = Math.abs(yOpen - yClose);
            if (bodyHeight == 0) bodyHeight = 1;
            // ... (kode gambar body candle tetep sama)
            g2.fillRect(xPos, bodyY, zoomLevel, bodyHeight);

            // --- UPDATE: Label Waktu Pintar Menyesuaikan Timeframe ---
            if (i % 5 == 0) { // Cetak label setiap 5 candle biar nggak numpuk
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("Arial", Font.PLAIN, 10));
                
                String timeFormatPattern;
                long tfMs = asset.getCurrentTimeframeMs();
                
                // Logic Format Waktu: 
                // Kalau TF Harian/Mingguan/Bulanan -> Tampilkan Tanggal & Bulan
                // Kalau TF Jam/Menit -> Tampilkan Jam & Menit
                // Kalau TF Detik -> Tampilkan lengkap pakai Detik
                if (tfMs >= 86400000L) { // >= 1 Hari
                    timeFormatPattern = "dd MMM yyyy";
                } else if (tfMs >= 3600000L) { // >= 1 Jam
                    timeFormatPattern = "dd MMM HH:mm";
                } else {
                    timeFormatPattern = "HH:mm:ss";
                }
                
                String timeStr = new SimpleDateFormat(timeFormatPattern).format(new Date(c.getTimestamp()));
                
                g2.setClip(originalClip); 
                g2.drawString(timeStr, xPos - 20, height - 10); // Posisi text waktu di bawah
                g2.setClip(0, 0, chartWidth, chartHeight); 
            }

            xPos -= spacing;
        }
        // ... (lanjut ke Langkah 5 sidebar kanan)

        // Matikan kliping agar bisa menggambar Panel Kanan (Harga)
        g2.setClip(originalClip);

        // 5. MENGGAMBAR SIDEBAR KANAN (Harga Y-Axis)
        g2.setColor(new Color(25, 27, 33)); // Background border kanan
        g2.fillRect(chartWidth, 0, RIGHT_MARGIN, height);
        g2.setColor(new Color(40, 44, 52)); // Garis pembatas
        g2.drawLine(chartWidth, 0, chartWidth, height);

        g2.setColor(Color.LIGHT_GRAY);
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        
        // Cetak skala harga di panel kanan
        for (int i = 20; i < chartHeight; i += 40) {
            // Hitung balik harga berdasarkan posisi Y pixel (termasuk offset)
            double priceAtY = maxPrice - (((i - offsetY) / (double)chartHeight) * priceRange);
            g2.drawString(String.format("%.2f", priceAtY), chartWidth + 5, i);
        }

        // 6. WATERMARK NAMA ASSET
        g2.setColor(new Color(255, 255, 255, 30));
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        g2.drawString(asset.getSymbol(), 20, 50);
        
        // --- UPDATE: Indikator Live Price Line (KECIL & RAPI) ---
        double currentPrice = asset.getCurrentPrice();
        int liveY = chartHeight - (int) (((currentPrice - minPrice) / priceRange) * chartHeight) + offsetY;
        
        g2.setColor(new Color(255, 152, 0, 180)); // Orange transparan
        // Garis horizontal putus di batas chart, nggak tembus ke sidebar
        g2.drawLine(0, liveY, chartWidth, liveY); 
        
        // Label harga live di sidebar kanan (Dibuat kecil seperti TradingView)
        g2.setColor(new Color(255, 152, 0)); // Background label
        g2.fillRect(chartWidth + 2, liveY - 7, RIGHT_MARGIN - 4, 14); // Kotak lebih tipis
        g2.setColor(Color.BLACK); // Warna font hitam biar kontras dan jelas
        g2.setFont(new Font("Arial", Font.BOLD, 10)); // Font dikecilin
        g2.drawString(String.format("%.2f", currentPrice), chartWidth + 5, liveY + 4);
    }
}