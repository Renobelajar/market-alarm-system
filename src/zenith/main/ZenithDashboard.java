package zenith.main;

import zenith.database.AssetDAO;
import zenith.engine.MarketThread;
import zenith.engine.BinanceFetcher;
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
        setTitle("Zenith: High-Frequency Market Watcher (Binance Live Edition)");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        assetDAO = new AssetDAO();
        chartPanel = new CandlestickPanel();

        String[] columns = {"Symbol", "Type", "Live Price"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(new Color(33, 37, 43));
        table.setForeground(Color.WHITE);
        table.setRowHeight(30);

        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setPreferredSize(new Dimension(300, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScrollPane, chartPanel);
        splitPane.setDividerLocation(300);
        splitPane.setDividerSize(3);
        add(splitPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(new Color(30, 33, 39));

        String[] tfOptions = {
            "TF: 5 Detik", "TF: 15 Detik", "TF: 30 Detik",
            "TF: 1 Menit", "TF: 5 Menit", "TF: 15 Menit",
            "TF: 1 Jam", "TF: 4 Jam", "TF: 1 Hari",
            "TF: 1 Minggu", "TF: 1 Bulan"
        };
        JComboBox<String> cbTimeframe = new JComboBox<>(tfOptions);

        JButton btnAdd = new JButton("➕ Tambah Asset");
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

        loadDataFromDB();

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

        engine = new MarketThread(activeAssets, model, chartPanel);
        backgroundThread = new Thread(engine);
        backgroundThread.start();

        if (table.getRowCount() > 0) table.setRowSelectionInterval(0, 0);


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
            chartPanel.resetView();
        });

        btnDraw.addActionListener(e -> {
            if (btnDraw.isSelected()) {
                chartPanel.setDrawingMode(true);
                btnDraw.setText("❌ Stop Draw");
                btnDraw.setBackground(new Color(239, 83, 80));
                btnDraw.setForeground(Color.WHITE);
            } else {
                chartPanel.setDrawingMode(false);
                btnDraw.setText("🖍️ Draw");
                btnDraw.setBackground(null);
                btnDraw.setForeground(null);
            }
        });

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

        btnAdd.addActionListener(e -> {
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            List<String> livePairs = BinanceFetcher.getAllUsdtPairs();
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

            if (livePairs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Gagal mengambil data dari Binance!\nPeriksa koneksi internet Anda.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JComboBox<String> dropdownPairs = new JComboBox<>(livePairs.toArray(new String[0]));

            JPanel panelForm = new JPanel(new GridLayout(0, 1, 5, 5));
            panelForm.add(new JLabel("Pilih Pair Memecoin / Crypto dari Binance:"));
            panelForm.add(dropdownPairs);

            int result = JOptionPane.showConfirmDialog(this, panelForm,
                    "Tambah Asset Real-Time", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String selectedSymbol = (String) dropdownPairs.getSelectedItem();

                if (assetDAO.addAsset(selectedSymbol, "Crypto", 0.0)) {
                    JOptionPane.showMessageDialog(this, "Aset " + selectedSymbol + " berhasil dimasukkan ke Watchlist!");
                    refreshSystem();
                } else {
                    JOptionPane.showMessageDialog(this, "Gagal! Kemungkinan pair sudah ada di database.");
                }
            }
        });

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

    private void loadDataFromDB() {
        model.setRowCount(0);
        activeAssets = assetDAO.getAllAssets();
        for (Asset asset : activeAssets) {
            String type = asset.getClass().getSimpleName();
            model.addRow(new Object[]{asset.getSymbol(), type, String.format("%.2f", asset.getCurrentPrice())});
        }
    }

    private void refreshSystem() {
        if (engine != null) engine.stopEngine();
        loadDataFromDB();
        engine = new MarketThread(activeAssets, model, chartPanel);
        backgroundThread = new Thread(engine);
        backgroundThread.start();
        if (table.getRowCount() > 0) table.setRowSelectionInterval(0, 0);
    }

    public static void main(String[] args) {
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
