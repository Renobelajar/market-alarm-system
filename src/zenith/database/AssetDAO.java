package zenith.database;

import zenith.models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AssetDAO {
    private Connection conn;

    public AssetDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    // [READ] Mengambil data dari database
    public List<Asset> getAllAssets() {
        List<Asset> assets = new ArrayList<>();
        String query = "SELECT * FROM watch_assets";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String symbol = rs.getString("symbol");
                String type = rs.getString("asset_type");
                double price = rs.getDouble("base_price");

                // Polymorphism: Instansiasi objek berdasarkan tipe dari DB
                if (type.equalsIgnoreCase("Crypto")) {
                    assets.add(new Crypto(symbol, price));
                } else {
                    assets.add(new Forex(symbol, price));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return assets;
    }

    // [CREATE] Menambah aset baru
    public boolean addAsset(String symbol, String type, double price) {
        String query = "INSERT INTO watch_assets (symbol, asset_type, base_price) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, symbol);
            pstmt.setString(2, type);
            pstmt.setDouble(3, price);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // [DELETE] Menghapus aset
    public boolean deleteAsset(String symbol) {
        String query = "DELETE FROM watch_assets WHERE symbol = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, symbol);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // [UPDATE] Mengubah data aset
    public boolean updateAsset(String oldSymbol, double newPrice) {
        String query = "UPDATE watch_assets SET base_price = ? WHERE symbol = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDouble(1, newPrice);
            pstmt.setString(2, oldSymbol);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // [CREATE] Simpan Alert ke DB
    public boolean saveAlert(String symbol, double targetPrice) {
        String query = "INSERT INTO price_alerts (symbol, target_price) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, symbol);
            pstmt.setDouble(2, targetPrice);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // [READ] Load Alert aktif dari DB untuk satu aset
    public List<Double> getActiveAlerts(String symbol) {
        List<Double> alerts = new ArrayList<>();
        String query = "SELECT target_price FROM price_alerts WHERE symbol = ? AND is_active = TRUE";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, symbol);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    alerts.add(rs.getDouble("target_price"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts;
    }
    
    // [UPDATE] Matikan Alert di DB setelah tersentuh
    public void deactivateAlert(String symbol, double targetPrice) {
        String query = "UPDATE price_alerts SET is_active = FALSE WHERE symbol = ? AND target_price = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, symbol);
            pstmt.setDouble(2, targetPrice);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // [DELETE] Hapus Alert sepenuhnya dari Database
    public void deleteAlert(String symbol, double targetPrice) {
        String query = "DELETE FROM price_alerts WHERE symbol = ? AND target_price = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, symbol);
            pstmt.setDouble(2, targetPrice);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}