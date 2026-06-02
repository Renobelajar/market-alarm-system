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
        setTitle("Zenith: High-Frequency Market Watcher (SMC Edition)");
        setSize(1200, 700); // Ukuran lebih luas biar chart lega
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null); 

        // 2. Inisialisasi DAO & Chart Panel
        assetDAO = new AssetDAO();
        chartPanel = new CandlestickPanel();
        
        // 3. Setup Tabel Sidebar Kiri (Dark Theme)
        String[] columns = {"Symbol", "Type", "Live Price"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Tabel Read-Only
            }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(new Color(33, 37, 43)); 
        table.setForeground(Color.WHITE);
        table.setRowHeight(30);
        
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setPreferredSize(new Dimension(300, 0));

        // 4. SplitPane (Kiri Tabel, Kanan Chart)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScrollPane, chartPanel);
        splitPane.setDividerLocation(300);
        splitPane.setDividerSize(3);
        add(splitPane, BorderLayout.CENTER);

        // 5. Panel Kontrol Bawah (Timeframe & Tombol)
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(new Color(30, 33, 39));
        
        // --- Komponen Panel Bawah ---
        String[] tfOptions = {
            "TF: 5 Detik", "TF: 15 Detik", "TF: 30 Detik",
            "TF: 1 Menit", "TF: 5 Menit", "TF: 15 Menit",
            "TF: 1 Jam", "TF: 4 Jam", "TF: 1 Hari",
            "TF: 1 Minggu", "TF: 1 Bulan"
        };
        JComboBox<String> cbTimeframe = new JComboBox<>(tfOptions);
        
        JButton btnAdd = new JButton("➕ Tambah");
        JButton btnUpdate = new JButton("✏️ Edit");
        JButton btnDelete = new JButton("🗑️ Hapus");
        JToggleButton btnDraw = new JToggleButton("🖍️ Draw"); 
        JToggleButton btnAlert = new JToggleButton("🔔 Set Alert"); 
        
        bottomPanel.add(new JLabel("⏳ "));
        bottomPanel.add(cbTimeframe);
        bottomPanel.add(btnAdd);
        bottomPanel.add(btnUpdate);
        bottomPanel.add(btnDelete);
        bottomPanel.add(new JLabel("  |  🛠️ Tools: "));
        bottomPanel.add(btnDraw);
        bottomPanel.add(btnAlert);
        add(bottomPanel, BorderLayout.SOUTH);

        // 6. Load Data Awal dari Database MySQL
        loadDataFromDB();

        // 7. Event Listener Tabel (Ganti Chart pas diklik)
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    String symbol = model.getValueAt(selectedRow, 0).toString();
                    for (Asset a : activeAssets) {
                        if (a.getSymbol().equals(symbol)) {
                            chartPanel.setAsset(a);
                            break;
                        }
                    }
                }
            }
        });

        // 8. Start Mesin Engine Multithreading
        engine = new MarketThread(activeAssets, model, chartPanel);
        backgroundThread = new Thread(engine);
        backgroundThread.start();

        // Pilih baris pertama otomatis saat buka aplikasi
        if (table.getRowCount() > 0) table.setRowSelectionInterval(0, 0);

        // ==========================================
        //         EVENT LISTENERS
        // ==========================================

        // [EVENT] Ganti Timeframe
        cbTimeframe.addActionListener(e -> {
            int index = cbTimeframe.getSelectedIndex();
            long newTimeframeMs = 5000; 
            switch (index) {
                case 0: newTimeframeMs = 5000L; break;
                case 1: newTimeframeMs = 15000L; break;
                case 2: newTimeframeMs = 30000L; break;
                case 3: newTimeframeMs = 60000L; break;
                case 4: newTimeframeMs = 300000L; break;
                case 5: newTimeframeMs = 900000L; break;
                case 6: newTimeframeMs = 3600000L; break;
                case 7: newTimeframeMs = 14400000L; break;
                case 8: newTimeframeMs = 86400000L; break;
                case 9: newTimeframeMs = 604800000L; break;
                case 10: newTimeframeMs = 2592000000L; break;
            }
            if (activeAssets != null) {
                for (Asset a : activeAssets) {
                    a.changeTimeframe(newTimeframeMs);
                }
            }
            chartPanel.resetView(); // Balikin chart ke posisi awal
        });

        // [EVENT] Draw Trendline Mode
        btnDraw.addActionListener(e -> {
            if (btnDraw.isSelected()) {
                chartPanel.setDrawingMode(true);
                btnDraw.setText("❌ Stop Draw");
                btnDraw.setBackground(new Color(239, 83, 80)); // Merah
                btnDraw.setForeground(Color.WHITE);
            } else {
                chartPanel.setDrawingMode(false);
                btnDraw.setText("🖍️ Draw");
                btnDraw.setBackground(null);
                btnDraw.setForeground(null);
            }
        });

        // [EVENT] Set Alert Interaktif Mode
        btnAlert.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Pilih aset di tabel kiri dulu!");
                btnAlert.setSelected(false);
                return;
            }

            if (btnAlert.isSelected()) {
                btnAlert.setText("❌ Batal Alert");
                btnAlert.setBackground(new Color(239, 83, 80));
                btnAlert.setForeground(Color.WHITE);
                
                chartPanel.setAlertMode(true, () -> {
                    // Callback saat user selesai ngeklik di chart
                    btnAlert.setSelected(false);
                    btnAlert.setText("🔔 Set Alert");
                    btnAlert.setBackground(null);
                    btnAlert.setForeground(null);
                });
            } else {
                chartPanel.setAlertMode(false, null);
                btnAlert.setText("🔔 Set Alert");
                btnAlert.setBackground(null);
                btnAlert.setForeground(null);
            }
        });

        // [CRUD: CREATE] Tambah Data
        btnAdd.addActionListener(e -> {
            String symbol = JOptionPane.showInputDialog(this, "Masukkan Symbol (Contoh: GBP/USD):");
            if (symbol == null || symbol.trim().isEmpty()) return;
            
            String[] options = {"Forex", "Crypto"};
            int typeChoice = JOptionPane.showOptionDialog(this, "Pilih Tipe Aset:", "Tipe Asset",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (typeChoice == -1) return; 
            
            String priceStr = JOptionPane.showInputDialog(this, "Harga Awal:");
            if (priceStr == null || priceStr.trim().isEmpty()) return;

            try {
                double price = Double.parseDouble(priceStr);
                if (assetDAO.addAsset(symbol, options[typeChoice], price)) {
                    JOptionPane.showMessageDialog(this, "Aset berhasil ditambahkan!");
                    refreshSystem(); 
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error: Harga harus berupa angka!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // [CRUD: UPDATE] Edit Data
        btnUpdate.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                String symbol = model.getValueAt(selectedRow, 0).toString();
                String priceStr = JOptionPane.showInputDialog(this, "Masukkan Harga Baru untuk " + symbol + ":");
                if (priceStr != null && !priceStr.trim().isEmpty()) {
                    try {
                        double newPrice = Double.parseDouble(priceStr);
                        if (assetDAO.updateAsset(symbol, newPrice)) {
                            JOptionPane.showMessageDialog(this, "Harga berhasil di-update!");
                            refreshSystem(); 
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Error: Harga harus angka!", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Pilih baris di tabel dulu!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            }
        });

        // [CRUD: DELETE] Hapus Data
        btnDelete.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                String symbol = model.getValueAt(selectedRow, 0).toString();
                int confirm = JOptionPane.showConfirmDialog(this, "Yakin hapus " + symbol + "?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (assetDAO.deleteAsset(symbol)) {
                        JOptionPane.showMessageDialog(this, "Aset dihapus!");
                        refreshSystem();
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Pilih baris aset di tabel terlebih dahulu!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    // Method Helper buat Load Database
    private void loadDataFromDB() {
        model.setRowCount(0); 
        activeAssets = assetDAO.getAllAssets();
        for (Asset asset : activeAssets) {
            String type = asset.getClass().getSimpleName();
            model.addRow(new Object[]{asset.getSymbol(), type, String.format("%.2f", asset.getCurrentPrice())});
        }
    }

    // Method Helper buat Restart Engine saat ada perubahan DB
    private void refreshSystem() {
        if (engine != null) engine.stopEngine(); 
        
        loadDataFromDB(); 
        engine = new MarketThread(activeAssets, model, chartPanel);
        backgroundThread = new Thread(engine);
        backgroundThread.start();
        
        if (table.getRowCount() > 0) table.setRowSelectionInterval(0, 0);
    }

    public static void main(String[] args) {
        // Pasang tema (Look and Feel) bawaan OS biar gak kaku kayak aplikasi Java 90-an
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new ZenithDashboard().setVisible(true);
        });
    }
}