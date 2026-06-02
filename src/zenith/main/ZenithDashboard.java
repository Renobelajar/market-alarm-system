package zenith.main;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.util.List;
import zenith.engine.BinanceFetcher;

public class ZenithDashboard extends JFrame {

    private Connection conn;

    private JTable marketTable;
    private DefaultTableModel tableModel;
    private JPanel chartPanel;
    private JComboBox<String> tfComboBox;
    
    private JButton btnTambah, btnEdit, btnHapus, btnStopDraw, btnSetAlert;
    private zenith.main.CandlestickPanel candlestickPanel;

    public ZenithDashboard(Connection conn) {
        this.conn = conn;
        
        setTitle("Zenith: High-Frequency Market Watcher (Binance Live Edition)");
        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    public ZenithDashboard() {
        setTitle("Zenith: High-Frequency Market Watcher (Binance Live Edition)");
        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
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
        leftScrollPane.setPreferredSize(new Dimension(300, 0));

        chartPanel = new JPanel();
        chartPanel.setBackground(Color.BLACK);
        chartPanel.setLayout(new BorderLayout());

        candlestickPanel = new zenith.main.CandlestickPanel();
        chartPanel.add(candlestickPanel, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScrollPane, chartPanel);
        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);

        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        JLabel tfLabel = new JLabel("TF:");
        tfLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        toolbarPanel.add(tfLabel);

        String[] timeframes = {"1 Menit", "5 Menit", "15 Menit", "1 Jam"};
        tfComboBox = new JComboBox<>(timeframes);
        toolbarPanel.add(tfComboBox);

        btnTambah = new JButton("Tambah Asset");
        btnEdit = new JButton("Edit");
        btnHapus = new JButton("Hapus");
        btnStopDraw = new JButton("Stop Draw");
        btnSetAlert = new JButton("Set Alert");

        toolbarPanel.add(btnTambah);
        toolbarPanel.add(btnEdit);
        toolbarPanel.add(btnHapus);
        toolbarPanel.add(btnStopDraw);
        toolbarPanel.add(btnSetAlert);

        add(toolbarPanel, BorderLayout.SOUTH);

        initActions();
    }

    private void initActions() {
        btnTambah.addActionListener(e -> {
            if (this.conn != null) {
                System.out.println("[DB Action] Membuka menu tambah asset...");
            }
        });

        marketTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && marketTable.getSelectedRow() != -1) {
                String symbol = tableModel.getValueAt(marketTable.getSelectedRow(), 0).toString();
                zenith.models.Asset selectedAsset = new zenith.models.Crypto(symbol, 0.0);
                candlestickPanel.setAsset(selectedAsset);
            }
        });
    }

    public void updateDashboardData() {
        new SwingWorker<Void, Object[]>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<String> pairs = BinanceFetcher.getAllUsdtPairs();
                int limit = Math.min(pairs.size(), 20);
                tableModel.setRowCount(0);

                for (int i = 0; i < limit; i++) {
                    String symbol = pairs.get(i);
                    double price = BinanceFetcher.getCurrentPrice(symbol);
                    String formattedPrice = (price == -1.0) ? "Loading..." : String.format("$%,.2f", price);
                    
                    publish(new Object[]{symbol, "SPOT", formattedPrice});
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