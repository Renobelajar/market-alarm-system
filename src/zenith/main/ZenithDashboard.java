package zenith.main;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection; 
import java.util.List;
import zenith.engine.BinanceFetcher;

public class ZenithDashboard extends JFrame {
    
    private Connection conn; 

    // Komponen UI Utama (Sesuai Struktur screenshot WhatsApp Anda)
    private JTable marketTable;
    private DefaultTableModel tableModel;
    private JPanel chartPanel;
    private JComboBox<String> tfComboBox;
    
    // Tombol-Tombol Aksi Asli (Tanpa Icon Gambar)
    private JButton btnTambah, btnEdit, btnHapus, btnStopDraw, btnSetAlert;

    // Konstruktor utama menerima koneksi DB Anda agar fitur lain tidak putus
    public ZenithDashboard(Connection conn) {
        this.conn = conn; // Menyimpan koneksi database ke lokal class
        
        setTitle("Zenith: High-Frequency Market Watcher (Binance Live Edition)");
        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    // Konstruktor cadangan tanpa parameter (jika dipanggil dari Main biasa)
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
        // Menggunakan font standar sistem agar tulisan hitamnya jelas terbaca
        marketTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        marketTable.setRowHeight(25);

        JScrollPane leftScrollPane = new JScrollPane(marketTable);
        leftScrollPane.setPreferredSize(new Dimension(300, 0));

        chartPanel = new JPanel();
        chartPanel.setBackground(Color.BLACK); // Area chart tetap hitam pekat sesuai aslinya
        chartPanel.setLayout(new BorderLayout());

        // Satukan panel tabel kiri dan panel chart kanan menggunakan SplitPane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScrollPane, chartPanel);
        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);

        
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        // Label & ComboBox Timeframe (TF)
        JLabel tfLabel = new JLabel("TF:");
        tfLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        toolbarPanel.add(tfLabel);

        String[] timeframes = {"5 Detik", "1 Menit", "5 Menit", "15 Menit", "1 Jam"};
        tfComboBox = new JComboBox<>(timeframes);
        toolbarPanel.add(tfComboBox);

        // Tombol aksi murni teks standar bawaan OS (Tulisan Hitam Jelas & Tanpa Gambar)
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

        // Hubungkan event 
        initActions();
    }

    private void initActions() {
        // Tombol Tambah Asset memakai koneksi database 'this.conn' 
        btnTambah.addActionListener(e -> {
            if (this.conn != null) {
                // Jalankan logika/dialog 
                System.out.println("[DB Action] Membuka menu tambah asset...");
            }
        });

    }

    // Fungsi otomatis untuk sinkronisasi harga live dari Binance ke Tabel
    public void updateDashboardData() {
        new SwingWorker<Void, Object[]>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<String> pairs = BinanceFetcher.getAllUsdtPairs();
                int limit = Math.min(pairs.size(), 20); // Ambil 20 koin teratas
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