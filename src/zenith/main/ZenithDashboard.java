package zenith.main;

import zenith.database.AssetDAO;
import zenith.engine.MarketThread;
import zenith.models.Asset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ZenithDashboard extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private CandlestickPanel chartPanel;
    private AssetDAO assetDAO;
    private MarketThread engine;
    private Thread backgroundThread;
    private List<Asset> activeAssets;

    public ZenithDashboard() {
        // 1. Setup Basic Frame
        setTitle("Zenith: High-Frequency Market Watcher");
        setSize(1000, 600); // Ukuran lebih lebar untuk nampilin chart
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null); // Agar window muncul persis di tengah layar

        // 2. Inisialisasi DAO & Komponen UI Custom
        assetDAO = new AssetDAO();
        chartPanel = new CandlestickPanel();
        
        // 3. Setup Tabel (Panel Kiri)
        String[] columns = {"Symbol", "Type", "Live Price"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Mencegah user ngedit isi tabel secara manual (Read-Only)
            }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(new Color(40, 44, 52)); // Warna dark theme untuk tabel
        table.setForeground(Color.WHITE);
        table.setRowHeight(25);
        
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setPreferredSize(new Dimension(300, 0));

        // 4. SplitPane (Membelah UI jadi dua: Tabel di Kiri, Chart di Kanan)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScrollPane, chartPanel);
        splitPane.setDividerLocation(300); // Lebar tabel 300px
        add(splitPane, BorderLayout.CENTER);

       // 5. Panel Bawah (Tombol Kontrol Utama)
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(new Color(30, 33, 39));
        
        // Pilihan Timeframe ala MetaTrader/TradingView (Sampai 1 Bulan)
        String[] tfOptions = {
            "TF: 5 Detik", "TF: 15 Detik", "TF: 30 Detik",
            "TF: 1 Menit", "TF: 5 Menit", "TF: 15 Menit",
            "TF: 1 Jam", "TF: 4 Jam", "TF: 1 Hari",
            "TF: 1 Minggu", "TF: 1 Bulan"
        };
        JComboBox<String> cbTimeframe = new JComboBox<>(tfOptions);
        
        JButton btnAdd = new JButton("Tambah Asset");
        JButton btnAlert = new JButton("🔔 Set Alert");
        JButton btnDelete = new JButton("Hapus");
        
        bottomPanel.add(cbTimeframe);
        bottomPanel.add(btnAdd);
        bottomPanel.add(btnAlert);
        bottomPanel.add(btnDelete);
        add(bottomPanel, BorderLayout.SOUTH);

// --- UPDATE EVENT LISTENER TIMEFRAME ---
        cbTimeframe.addActionListener(e -> {
            int index = cbTimeframe.getSelectedIndex();
            long newTimeframeMs = 5000; // Default
            
            // Konversi dari Index ke Milidetik yang sesungguhnya
            switch (index) {
                case 0: newTimeframeMs = 5000L; break;                 // 5 Detik
                case 1: newTimeframeMs = 15000L; break;                // 15 Detik
                case 2: newTimeframeMs = 30000L; break;                // 30 Detik
                case 3: newTimeframeMs = 60000L; break;                // 1 Menit
                case 4: newTimeframeMs = 300000L; break;               // 5 Menit
                case 5: newTimeframeMs = 900000L; break;               // 15 Menit
                case 6: newTimeframeMs = 3600000L; break;              // 1 Jam
                case 7: newTimeframeMs = 14400000L; break;             // 4 Jam
                case 8: newTimeframeMs = 86400000L; break;             // 1 Hari
                case 9: newTimeframeMs = 604800000L; break;            // 1 Minggu
                case 10: newTimeframeMs = 2592000000L; break;          // 1 Bulan (Abaikan presisi bulan 30/31 hari)
            }
            
            // Beritahu semua aset untuk merubah bentuk masa lalunya
            if (activeAssets != null) {
                for (Asset a : activeAssets) {
                    a.changeTimeframe(newTimeframeMs);
                }
            }
            chartPanel.resetView(); // Snap chart balik ke tengah
        });

        // Event Set Alert (Notifikasi)
        btnAlert.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                String symbol = model.getValueAt(selectedRow, 0).toString();
                String priceStr = JOptionPane.showInputDialog(this, "Set Alert untuk " + symbol + "\nMasukkan Harga Target (Misal OB/Liquidity area):");
                
                if (priceStr != null && !priceStr.trim().isEmpty()) {
                    try {
                        double targetPrice = Double.parseDouble(priceStr);
                        // Cari objek aset dan pasang alert-nya
                        for (Asset a : activeAssets) {
                            if (a.getSymbol().equals(symbol)) {
                                a.setAlert(targetPrice);
                                JOptionPane.showMessageDialog(this, "Alert AKTIF! \nAplikasi akan berbunyi saat " + symbol + " menyentuh " + targetPrice);
                                break;
                            }
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Format angka salah!", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Pilih aset di sidebar kiri dulu!");
            }
        });

        // 6. Load Data Awal dari Database
        loadDataFromDB();

        // 7. Event Listener Tabel (Ganti Chart saat di-klik)
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    String symbol = model.getValueAt(selectedRow, 0).toString();
                    for (Asset a : activeAssets) {
                        if (a.getSymbol().equals(symbol)) {
                            chartPanel.setAsset(a);
                            chartPanel.repaint(); // Gambar ulang chart langsung
                            break;
                        }
                    }
                }
            }
        });

        // 8. Start Engine Multithreading
        engine = new MarketThread(activeAssets, model, chartPanel);
        backgroundThread = new Thread(engine);
        backgroundThread.start();

        // Pilih baris pertama otomatis saat aplikasi dibuka
        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }

        // ==========================================
        //         EVENT LISTENERS CRUD
        // ==========================================

        // [CREATE] Tambah Data
        btnAdd.addActionListener(e -> {
            String symbol = JOptionPane.showInputDialog(this, "Masukkan Symbol (Contoh: EUR/USD):");
            if (symbol == null || symbol.trim().isEmpty()) return; // Batal jika kosong
            
            String[] options = {"Forex", "Crypto"};
            int typeChoice = JOptionPane.showOptionDialog(this, "Pilih Tipe Aset:", "Tipe Asset",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (typeChoice == -1) return; // Batal jika di-close
            String type = options[typeChoice];
            
            String priceStr = JOptionPane.showInputDialog(this, "Harga Awal:");
            if (priceStr == null || priceStr.trim().isEmpty()) return;

            try {
                double price = Double.parseDouble(priceStr);
                if (assetDAO.addAsset(symbol, type, price)) {
                    JOptionPane.showMessageDialog(this, "Aset berhasil ditambahkan ke Database!");
                    refreshSystem(); // Reset thread dan GUI
                }
            } catch (NumberFormatException ex) {
                // Exception Handling biar program nggak force close
                JOptionPane.showMessageDialog(this, "Error: Harga harus berupa angka!", "Input Tidak Valid", JOptionPane.ERROR_MESSAGE);
            }
        });

        // [DELETE] Hapus Data
        btnDelete.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                String symbol = model.getValueAt(selectedRow, 0).toString();
                // Pop-up Konfirmasi
                int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus " + symbol + " dari pantauan?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (assetDAO.deleteAsset(symbol)) {
                        JOptionPane.showMessageDialog(this, "Aset telah dihapus!");
                        refreshSystem();
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Pilih baris aset di tabel terlebih dahulu!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    // Method untuk Load Ulang Data
    private void loadDataFromDB() {
        model.setRowCount(0); // Bersihkan isi tabel GUI
        activeAssets = assetDAO.getAllAssets();
        for (Asset asset : activeAssets) {
            String type = asset.getClass().getSimpleName();
            model.addRow(new Object[]{asset.getSymbol(), type, String.format("%.2f", asset.getCurrentPrice())});
        }
    }

    // Method untuk Restart Engine kalau ada perubahan CRUD
    private void refreshSystem() {
        if (engine != null) {
            engine.stopEngine(); // Matikan thread lama dengan aman
        }
        loadDataFromDB(); 
        
        // Buat engine thread baru dengan data ter-update
        engine = new MarketThread(activeAssets, model, chartPanel);
        backgroundThread = new Thread(engine);
        backgroundThread.start();
        
        // Auto-select baris pertama lagi
        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    public static void main(String[] args) {
        // [OPSIONAL] Pasang Look and Feel (Tema) Nimbus bawaan Java biar tombolnya nggak kaku
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Abaikan jika tema gagal diload
        }

        // Jalankan GUI dalam thread khusus antarmuka pengguna
        SwingUtilities.invokeLater(() -> {
            new ZenithDashboard().setVisible(true);
        });
    }
}