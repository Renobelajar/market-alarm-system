package zenith.main;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection; 
import java.util.List;
import java.util.ArrayList;
import zenith.engine.BinanceFetcher;
import zenith.database.AssetDAO;

public class ZenithDashboard extends JFrame {
    
    private Connection conn; 
    private AssetDAO assetDAO;

    private JTable marketTable;
    private DefaultTableModel tableModel;
    private JPanel chartPanel;
    private JComboBox<String> tfComboBox;
    
//    private JButton btnTambah, btnEdit, btnHapus, btnStopDraw, btnSetAlert;
    private JButton btnEdit, btnHapus, btnStopDraw, btnSetAlert;

    private zenith.main.CandlestickPanel candlestickPanel;
    
    private JTextField txtCari;
    private JPopupMenu autoPopup;
    private DefaultListModel<String> listModel;
    private JList<String> suggestList;
    private List<String> allBinancePairs = new ArrayList<>();
    
    private Thread tickerThread;

    public ZenithDashboard(Connection conn) {
        this.conn = conn; 
        this.assetDAO = new AssetDAO();
        setTitle("Zenith: High-Frequency Market Watcher (Binance Live Edition)");
        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
        loadBinancePairsAsync();
        startRealtimeTicker();
        updateDashboardData();
    }

    public ZenithDashboard() {
        this.assetDAO = new AssetDAO();
        setTitle("Zenith: High-Frequency Market Watcher (Binance Live Edition)");
        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
        loadBinancePairsAsync();
        startRealtimeTicker();
        updateDashboardData();
    }

    private void loadBinancePairsAsync() {
        new Thread(() -> {
            try {
                allBinancePairs = BinanceFetcher.getAllUsdtPairs();
            } catch (Exception e) {}
        }).start();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        String[] columns = {"Symbol", "Type", "Live Price"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        marketTable = new JTable(tableModel);
        marketTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        marketTable.setRowHeight(25);

        JScrollPane leftScrollPane = new JScrollPane(marketTable);

        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        txtCari = new JTextField();
        searchPanel.add(new JLabel("Cari:"), BorderLayout.WEST);
        searchPanel.add(txtCari, BorderLayout.CENTER);
        
        leftPanel.add(searchPanel, BorderLayout.NORTH);
        leftPanel.add(leftScrollPane, BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(300, 0));

        autoPopup = new JPopupMenu();
        listModel = new DefaultListModel<>();
        suggestList = new JList<>(listModel);
        JScrollPane popScroll = new JScrollPane(suggestList);
        popScroll.setPreferredSize(new Dimension(230, 150));
        autoPopup.add(popScroll);
        autoPopup.setFocusable(false);

        chartPanel = new JPanel();
        chartPanel.setBackground(Color.BLACK); 
        chartPanel.setLayout(new BorderLayout());

        candlestickPanel = new zenith.main.CandlestickPanel();
        chartPanel.add(candlestickPanel, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, chartPanel);
        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);

        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        JLabel tfLabel = new JLabel("TF:");
        tfLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        toolbarPanel.add(tfLabel);

        String[] timeframes = {"1 Menit", "5 Menit", "15 Menit", "1 Jam"};
        tfComboBox = new JComboBox<>(timeframes);
        toolbarPanel.add(tfComboBox);

//        btnTambah = new JButton("Tambah Asset");
        btnEdit = new JButton("Edit");
        btnHapus = new JButton("Hapus");
        btnStopDraw = new JButton("Stop Draw");
        btnSetAlert = new JButton("Set Alert");

//        toolbarPanel.add(btnTambah);
        toolbarPanel.add(btnEdit);
        toolbarPanel.add(btnHapus);
        toolbarPanel.add(btnStopDraw);
        toolbarPanel.add(btnSetAlert);

        add(toolbarPanel, BorderLayout.SOUTH);

        initActions();
    }

    private void initActions() {
        txtCari.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSuggestions(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSuggestions(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSuggestions(); }
            
            private void updateSuggestions() {
                SwingUtilities.invokeLater(() -> {
                    String text = txtCari.getText().toUpperCase().trim();
                    if (text.isEmpty() || allBinancePairs.isEmpty()) {
                        autoPopup.setVisible(false);
                        return;
                    }
                    listModel.clear();
                    int count = 0;
                    for (String pair : allBinancePairs) {
                        if (pair.contains(text)) {
                            listModel.addElement(pair);
                            count++;
                            if (count >= 30) break;
                        }
                    }
                    if (listModel.isEmpty()) {
                        autoPopup.setVisible(false);
                    } else {
                        autoPopup.show(txtCari, 0, txtCari.getHeight());
                        txtCari.requestFocusInWindow();
                    }
                });
            }
        });

        suggestList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String selected = suggestList.getSelectedValue();
                if (selected != null) {
                    txtCari.setText(selected);
                    autoPopup.setVisible(false);
                }
            }
        });

        txtCari.addActionListener(e -> {
            String symbol = txtCari.getText().toUpperCase().trim();
            if (!symbol.isEmpty()) {
                double livePrice = BinanceFetcher.getCurrentPrice(symbol);
                if (livePrice > 0) {
                    if (assetDAO.addAsset(symbol, "Crypto", livePrice)) {
                        JOptionPane.showMessageDialog(this, "Berhasil menambahkan " + symbol);
                        txtCari.setText("");
                        autoPopup.setVisible(false);
                        updateDashboardData();
                    } else {
                        JOptionPane.showMessageDialog(this, "Koin sudah ada di database.");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Symbol tidak ditemukan di Binance.");
                }
            }
        });

        tfComboBox.addActionListener(e -> {
            if (candlestickPanel.getAsset() != null) {
                String selected = (String) tfComboBox.getSelectedItem();
                long ms = 60000; 
                if ("5 Menit".equals(selected)) ms = 300000;
                else if ("15 Menit".equals(selected)) ms = 900000;
                else if ("1 Jam".equals(selected)) ms = 3600000;
                
                final long targetMs = ms;
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        candlestickPanel.getAsset().changeTimeframe(targetMs);
                        return null;
                    }
                    @Override
                    protected void done() {
                        candlestickPanel.resetView();
                        candlestickPanel.repaint();
                    }
                }.execute();
            }
        });

        marketTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && marketTable.getSelectedRow() != -1) {
                String symbol = tableModel.getValueAt(marketTable.getSelectedRow(), 0).toString();
                new SwingWorker<zenith.models.Asset, Void>() {
                    @Override
                    protected zenith.models.Asset doInBackground() {
                        return new zenith.models.Crypto(symbol, 0.0);
                    }
                    @Override
                    protected void done() {
                        try {
                            candlestickPanel.setAsset(get());
                            candlestickPanel.repaint();
                        } catch (Exception ex) {}
                    }
                }.execute();
            }
        });
//
//        btnTambah.addActionListener(e -> {
//            String symbol = JOptionPane.showInputDialog(this, "Masukkan Pair Koin (Contoh: BTCUSDT):");
//            if (symbol != null && !symbol.trim().isEmpty()) {
//                symbol = symbol.toUpperCase().trim();
//                double livePrice = BinanceFetcher.getCurrentPrice(symbol);
//                if (livePrice > 0) {
//                    if (assetDAO.addAsset(symbol, "Crypto", livePrice)) {
//                        JOptionPane.showMessageDialog(this, "Berhasil menambahkan " + symbol);
//                        updateDashboardData();
//                    } else {
//                        JOptionPane.showMessageDialog(this, "Gagal menambahkan ke Database.");
//                    }
//                } else {
//                    JOptionPane.showMessageDialog(this, "Symbol koin tidak ditemukan di Binance.");
//                }
//            }
//        });

        btnEdit.addActionListener(e -> {
            int row = marketTable.getSelectedRow();
            if (row != -1) {
                String symbol = tableModel.getValueAt(row, 0).toString();
                String newPriceStr = JOptionPane.showInputDialog(this, "Set base price baru untuk " + symbol + ":");
                if (newPriceStr != null) {
                    try {
                        double newPrice = Double.parseDouble(newPriceStr.replace(",", "."));
                        if (assetDAO.updateAsset(symbol, newPrice)) {
                            JOptionPane.showMessageDialog(this, "Berhasil update harga.");
                            updateDashboardData();
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Format harga salah.");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Pilih pair dari tabel sebelah kiri terlebih dahulu.");
            }
        });

        btnHapus.addActionListener(e -> {
            int row = marketTable.getSelectedRow();
            if (row != -1) {
                String symbol = tableModel.getValueAt(row, 0).toString();
                int confirm = JOptionPane.showConfirmDialog(this, "Hapus " + symbol + " dari watchlist?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (assetDAO.deleteAsset(symbol)) {
                        JOptionPane.showMessageDialog(this, symbol + " telah dihapus.");
                        updateDashboardData();
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Pilih pair dari tabel sebelah kiri terlebih dahulu.");
            }
        });

        btnSetAlert.addActionListener(e -> {
            candlestickPanel.setAlertMode(true, () -> {});
        });

        btnStopDraw.addActionListener(e -> {
            candlestickPanel.setAlertMode(false, null);
            candlestickPanel.setDrawingMode(false);
        });
    }

    private void startRealtimeTicker() {
        tickerThread = new Thread(() -> {
            while (true) {
                try {
                    if (candlestickPanel != null && candlestickPanel.getAsset() != null) {
                        candlestickPanel.getAsset().simulateTick();
                        SwingUtilities.invokeLater(() -> candlestickPanel.repaint());
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    break;
                } catch (Exception ex) {}
            }
        });
        tickerThread.setDaemon(true);
        tickerThread.start();
    }

    public void updateDashboardData() {
        tableModel.setRowCount(0);
        new SwingWorker<Void, Object[]>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Memanggil data HANYA dari database yang tersimpan
                List<zenith.models.Asset> savedAssets = assetDAO.getAllAssets();
                for (zenith.models.Asset a : savedAssets) {
                    String symbol = a.getSymbol();
                    String type = a instanceof zenith.models.Crypto ? "Crypto" : "Forex";
                    double price = BinanceFetcher.getCurrentPrice(symbol);
                    String formattedPrice = (price == -1.0) ? "Loading..." : String.format("$%,.2f", price);
                    publish(new Object[]{symbol, type, formattedPrice});
                }
                return null;
            }

            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] row : chunks) {
                    tableModel.addRow(row);
                }
            }
        }.execute();
    }
}