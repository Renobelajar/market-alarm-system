package zenith.main;

import zenith.models.Asset;
import zenith.models.Candle;
import zenith.models.TrendLine;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CandlestickPanel extends JPanel {
    private Asset asset;

    private int zoomLevel = 8;
    private int offsetX = 0;
    private int offsetY = 0;
    private int lastMouseX, lastMouseY;

    private final int RIGHT_MARGIN = 70;
    private final int BOTTOM_MARGIN = 30;

    private boolean isDrawingMode = false;
    private TrendLine tempLine = null;

    private boolean isAlertMode = false;
    private double hoverPrice = 0.0;
    private Runnable onAlertSetCallback;

    private double currentMinPrice = 0;
    private double currentMaxPrice = 0;

    public CandlestickPanel() {
        setBackground(new Color(18, 20, 24));

        addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0) zoomLevel += 2;
            else zoomLevel -= 2;
            if (zoomLevel < 2) zoomLevel = 2;
            if (zoomLevel > 50) zoomLevel = 50;
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (asset == null) return;

                if (isAlertMode) {
                    double targetPrice = getPriceFromY(e.getY());
                    asset.addAlert(targetPrice);
                    isAlertMode = false;
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    if (onAlertSetCallback != null) onAlertSetCallback.run();
                    repaint();
                    JOptionPane.showMessageDialog(CandlestickPanel.this, "✅ Alert dipasang di harga:\n" + String.format("%.2f", targetPrice));
                    return;
                }

                for (Double alertPrice : asset.getActiveAlerts()) {
                    int alertY = getYFromPrice(alertPrice);
                    if (Math.abs(e.getY() - alertY) <= 5) {
                        showAlertMenu(e, alertPrice);
                        return;
                    }
                }

                if (isDrawingMode) {
                    long ts = getTimestampFromX(e.getX());
                    double price = getPriceFromY(e.getY());
                    tempLine = new TrendLine(ts, price, ts, price);
                } else {
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDrawingMode && tempLine != null) {
                    asset.getDrawnLines().add(tempLine);
                    tempLine = null;
                    repaint();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (isAlertMode) {
                    hoverPrice = getPriceFromY(e.getY());
                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDrawingMode && tempLine != null) {
                    tempLine.endTimestamp = getTimestampFromX(e.getX());
                    tempLine.endPrice = getPriceFromY(e.getY());
                    repaint();
                } else if (!isDrawingMode && !isAlertMode) {
                    offsetX += (e.getX() - lastMouseX);
                    offsetY += (e.getY() - lastMouseY);
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    repaint();
                }
            }
        });
    }

    private void showAlertMenu(MouseEvent e, double alertPrice) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem delItem = new JMenuItem("🗑️ Hapus Alert di Harga: " + String.format("%.2f", alertPrice));
        JMenuItem closeItem = new JMenuItem("❌ Batal / Tutup");

        delItem.addActionListener(ev -> {
            asset.removeAlert(alertPrice);
            repaint();
        });

        menu.add(delItem);
        menu.addSeparator();
        menu.add(closeItem);

        menu.show(this, e.getX(), e.getY());
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
        resetView();
    }
    
    public Asset getAsset() {
        return this.asset;
    }

    public void resetView() {
        this.offsetX = 0; this.offsetY = 0; this.zoomLevel = 8;
        repaint();
    }

    public void setDrawingMode(boolean isDrawing) {
        this.isDrawingMode = isDrawing;
        if (isDrawing) setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        else setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    public void setAlertMode(boolean mode, Runnable onSetCallback) {
        this.isAlertMode = mode;
        this.onAlertSetCallback = onSetCallback;
        if (mode) setCursor(new Cursor(Cursor.HAND_CURSOR));
        else setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        repaint();
    }

    private double getPriceFromY(int y) {
        int chartHeight = getHeight() - BOTTOM_MARGIN;
        double priceRange = currentMaxPrice - currentMinPrice;
        if (priceRange == 0) priceRange = 1;
        return currentMaxPrice - (((y - offsetY) / (double) chartHeight) * priceRange);
    }
    private long getTimestampFromX(int x) {
        if (asset == null || asset.getChartData().isEmpty()) return 0;
        int chartWidth = getWidth() - RIGHT_MARGIN;
        int spacing = zoomLevel + 2;
        double candlesFromRight = (double)(chartWidth - spacing + offsetX - x) / spacing;
        long latestTime = asset.getChartData().get(asset.getChartData().size() - 1).getTimestamp();
        return latestTime - (long)(candlesFromRight * asset.getCurrentTimeframeMs());
    }
    private int getYFromPrice(double price) {
        int chartHeight = getHeight() - BOTTOM_MARGIN;
        double priceRange = currentMaxPrice - currentMinPrice;
        if (priceRange == 0) priceRange = 1;
        return chartHeight - (int) (((price - currentMinPrice) / priceRange) * chartHeight) + offsetY;
    }
    private int getXFromTimestamp(long timestamp) {
        if (asset == null || asset.getChartData().isEmpty()) return 0;
        int chartWidth = getWidth() - RIGHT_MARGIN;
        int spacing = zoomLevel + 2;
        long latestTime = asset.getChartData().get(asset.getChartData().size() - 1).getTimestamp();
        double candlesFromRight = (double)(latestTime - timestamp) / asset.getCurrentTimeframeMs();
        return chartWidth - spacing + offsetX - (int)(candlesFromRight * spacing);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (asset == null || asset.getChartData().isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<Candle> candles = asset.getChartData();
        int width = getWidth(), height = getHeight();
        int chartWidth = width - RIGHT_MARGIN, chartHeight = height - BOTTOM_MARGIN;

        currentMinPrice = Double.MAX_VALUE;
        currentMaxPrice = Double.MIN_VALUE;
        for (Candle c : candles) {
            if (c.getLow() < currentMinPrice) currentMinPrice = c.getLow();
            if (c.getHigh() > currentMaxPrice) currentMaxPrice = c.getHigh();
        }
        double priceRange = currentMaxPrice - currentMinPrice;
        if (priceRange == 0) priceRange = 1;

        g2.setColor(new Color(40, 44, 52));
        for (int i = 0; i < chartHeight; i += 40) g2.drawLine(0, i, chartWidth, i);
        for (int i = chartWidth; i > 0; i -= 60) g2.drawLine(i, 0, i, chartHeight);

        Shape originalClip = g2.getClip();
        g2.clipRect(0, 0, chartWidth, chartHeight);

        int spacing = zoomLevel + 2;
        int xPos = chartWidth - spacing + offsetX;
        for (int i = candles.size() - 1; i >= 0; i--) {
            Candle c = candles.get(i);
            if (xPos + zoomLevel < 0 || xPos > chartWidth) { xPos -= spacing; continue; }

            int yHigh = getYFromPrice(c.getHigh());
            int yLow = getYFromPrice(c.getLow());
            int yOpen = getYFromPrice(c.getOpen());
            int yClose = getYFromPrice(c.getClose());

            if (c.isBullish()) g2.setColor(new Color(38, 166, 154));
            else g2.setColor(new Color(239, 83, 80));

            g2.drawLine(xPos + zoomLevel / 2, yHigh, xPos + zoomLevel / 2, yLow);
            int bodyY = Math.min(yOpen, yClose);
            int bodyHeight = Math.max(1, Math.abs(yOpen - yClose));
            g2.fillRect(xPos, bodyY, zoomLevel, bodyHeight);

            if (i % 5 == 0) {
                g2.setColor(Color.GRAY); g2.setFont(new Font("Arial", Font.PLAIN, 10));
                String pattern = asset.getCurrentTimeframeMs() >= 86400000L ? "dd MMM" : "HH:mm:ss";
                String timeStr = new SimpleDateFormat(pattern).format(new Date(c.getTimestamp()));
                g2.setClip(originalClip);
                g2.drawString(timeStr, xPos - 20, height - 10);
                g2.clipRect(0, 0, chartWidth, chartHeight);
            }
            xPos -= spacing;
        }

        g2.setColor(new Color(33, 150, 243));
        g2.setStroke(new BasicStroke(2));
        for (TrendLine line : asset.getDrawnLines()) {
            g2.drawLine(getXFromTimestamp(line.startTimestamp), getYFromPrice(line.startPrice),
                        getXFromTimestamp(line.endTimestamp), getYFromPrice(line.endPrice));
        }
        if (tempLine != null) {
            g2.drawLine(getXFromTimestamp(tempLine.startTimestamp), getYFromPrice(tempLine.startPrice),
                        getXFromTimestamp(tempLine.endTimestamp), getYFromPrice(tempLine.endPrice));
        }

        Stroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        g2.setStroke(dashed);
        for (Double alertPrice : asset.getActiveAlerts()) {
            int alertY = getYFromPrice(alertPrice);
            if (alertY >= 0 && alertY <= chartHeight) {
                g2.setColor(new Color(255, 82, 82, 200));
                g2.drawLine(0, alertY, chartWidth, alertY);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                g2.drawString("🔔 POI: " + String.format("%.2f", alertPrice), 10, alertY - 5);
            }
        }

        if (isAlertMode) {
            int hoverY = getYFromPrice(hoverPrice);
            g2.setColor(new Color(255, 193, 7, 200));
            Stroke hoverDashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);
            g2.setStroke(hoverDashed);
            g2.drawLine(0, hoverY, chartWidth, hoverY);

            g2.setColor(new Color(255, 193, 7));
            g2.fillRoundRect(chartWidth + 2, hoverY - 7, RIGHT_MARGIN - 4, 14, 3, 3);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            g2.drawString(String.format("%.2f", hoverPrice), chartWidth + 5, hoverY + 4);
            g2.drawString("KLIK KIRI BUAT PASANG", chartWidth / 2 - 50, hoverY - 5);
        }

        g2.setStroke(new BasicStroke(1));
        g2.setClip(originalClip);

        g2.setColor(new Color(25, 27, 33)); g2.fillRect(chartWidth, 0, RIGHT_MARGIN, height);
        g2.setColor(new Color(40, 44, 52)); g2.drawLine(chartWidth, 0, chartWidth, height);
        g2.setColor(Color.LIGHT_GRAY); g2.setFont(new Font("Arial", Font.BOLD, 11));
        for (int i = 20; i < chartHeight; i += 40) {
            g2.drawString(String.format("%.2f", getPriceFromY(i)), chartWidth + 5, i);
        }

        g2.setColor(new Color(255, 255, 255, 30)); g2.setFont(new Font("Arial", Font.BOLD, 40));
        g2.drawString(asset.getSymbol(), 20, 50);

        double currentPrice = asset.getCurrentPrice();
        int liveY = getYFromPrice(currentPrice);

        g2.setColor(new Color(255, 152, 0, 180));
        g2.drawLine(0, liveY, chartWidth, liveY);

        g2.setColor(new Color(255, 152, 0));
        g2.fillRect(chartWidth + 2, liveY - 7, RIGHT_MARGIN - 4, 14);
        g2.setColor(Color.BLACK); g2.setFont(new Font("Arial", Font.BOLD, 10));
        g2.drawString(String.format("%.2f", currentPrice), chartWidth + 5, liveY + 4);

        long elapsed = System.currentTimeMillis() - candles.get(candles.size() - 1).getTimestamp();
        long remaining = asset.getCurrentTimeframeMs() - elapsed;
        if (remaining < 0) remaining = 0;

        long sec = (remaining / 1000) % 60;
        long min = (remaining / 60000) % 60;
        long hr = (remaining / 3600000);
        String countDownStr = hr > 0 ? String.format("%02d:%02d:%02d", hr, min, sec) : String.format("%02d:%02d", min, sec);

        g2.setColor(new Color(255, 152, 0, 180));
        g2.drawString(countDownStr, chartWidth + 5, liveY + 18);
    }
}