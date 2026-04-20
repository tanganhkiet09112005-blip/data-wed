import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuanLyKhachHang extends JFrame {

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtPhone, txtTenKH, txtNhuCau, txtTimKiem;
    private JButton btnTaiDuLieu, btnThem, btnXoa, btnLamMoi;
    private JLabel lblTongSo;
    private TableRowSorter<DefaultTableModel> sorter;

    // Biến lưu trữ "Chìa khóa" (Key) của Firebase để biết đường Xóa đúng người
    private String selectedFirebaseKey = "";

    // QUAN TRỌNG: Link Firebase kết nối với Website
    private final String FIREBASE_URL = "https://devstudio-crm-default-rtdb.asia-southeast1.firebasedatabase.app/khachhang.json";

    public QuanLyKhachHang() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } 
        catch (Exception e) { e.printStackTrace(); }

        setTitle("NTC Workspace (Dev: Anh Kiệt) - Hệ Thống CRM Quản Lý Khách Hàng");
        setSize(1150, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(241, 245, 249));

        Font mainFont = new Font("Segoe UI", Font.PLAIN, 14);
        Font boldFont = new Font("Segoe UI", Font.BOLD, 14);

        // --- PHẦN TRÊN: TIÊU ĐỀ & TÌM KIẾM ---
        JPanel panelTop = new JPanel(new BorderLayout(20, 0));
        panelTop.setBackground(new Color(15, 23, 42)); 
        panelTop.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblTitle = new JLabel("DANH SÁCH KHÁCH HÀNG TIỀM NĂNG (LEADS)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);

        // Thanh tìm kiếm siêu tốc
        JPanel panelSearch = new JPanel(new BorderLayout(10, 0));
        panelSearch.setOpaque(false);
        JLabel lblSearch = new JLabel("🔍 Lọc danh sách (Tên, SĐT...): ");
        lblSearch.setForeground(Color.WHITE);
        lblSearch.setFont(boldFont);
        txtTimKiem = new JTextField(25);
        txtTimKiem.setFont(mainFont);
        panelSearch.add(lblSearch, BorderLayout.WEST);
        panelSearch.add(txtTimKiem, BorderLayout.CENTER);

        panelTop.add(lblTitle, BorderLayout.WEST);
        panelTop.add(panelSearch, BorderLayout.EAST);
        add(panelTop, BorderLayout.NORTH);

        // --- PANEL TRÁI: FORM THÊM & XÓA ---
        JPanel panelLeft = new JPanel(new BorderLayout(10, 10));
        panelLeft.setPreferredSize(new Dimension(320, 0));
        panelLeft.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panelLeft.setBackground(Color.WHITE);

        JPanel panelInput = new JPanel(new GridLayout(6, 1, 5, 5));
        panelInput.setBackground(Color.WHITE);
        
        panelInput.add(new JLabel("Số Điện Thoại / Zalo:"));
        txtPhone = new JTextField(); txtPhone.setFont(mainFont); panelInput.add(txtPhone);
        
        panelInput.add(new JLabel("Tên Khách Hàng:"));
        txtTenKH = new JTextField(); txtTenKH.setFont(mainFont); panelInput.add(txtTenKH);
        
        panelInput.add(new JLabel("Nhu Cầu / Ghi Chú (Email):"));
        txtNhuCau = new JTextField(); txtNhuCau.setFont(mainFont); panelInput.add(txtNhuCau);
        
        panelLeft.add(panelInput, BorderLayout.NORTH);

        JPanel panelButtons = new JPanel(new GridLayout(4, 1, 10, 10));
        panelButtons.setBackground(Color.WHITE);
        
        btnThem = new JButton("Thêm Khách Hàng Mới"); 
        btnThem.setBackground(new Color(16, 185, 129)); btnThem.setForeground(Color.WHITE); btnThem.setFont(boldFont);
        
        btnXoa = new JButton("Xóa Khách Hàng Chọn"); 
        btnXoa.setBackground(new Color(239, 68, 68)); btnXoa.setForeground(Color.WHITE); btnXoa.setFont(boldFont);
        btnXoa.setEnabled(false); 
        
        btnLamMoi = new JButton("Làm Mới Form"); btnLamMoi.setFont(boldFont);
        
        btnTaiDuLieu = new JButton("Đồng Bộ Website (Kéo Data)"); 
        btnTaiDuLieu.setBackground(new Color(37, 99, 235)); btnTaiDuLieu.setForeground(Color.WHITE); btnTaiDuLieu.setFont(boldFont);

        panelButtons.add(btnThem);
        panelButtons.add(btnXoa);
        panelButtons.add(btnLamMoi);
        panelButtons.add(btnTaiDuLieu);
        panelLeft.add(panelButtons, BorderLayout.SOUTH);

        add(panelLeft, BorderLayout.WEST);

        // --- PHẦN GIỮA: BẢNG DỮ LIỆU ---
        String[] columns = {"STT", "Số Điện Thoại", "Tên Khách Hàng", "Nhu Cầu & Thông Tin", "Thời Gian", "FirebaseKey"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(tableModel);
        table.setFont(mainFont); table.setRowHeight(35);
        table.getTableHeader().setFont(boldFont);
        table.getTableHeader().setBackground(new Color(226, 232, 240));

        // Tùy chỉnh độ rộng cột cho đẹp mắt
        table.getColumnModel().getColumn(0).setPreferredWidth(40);  // STT
        table.getColumnModel().getColumn(1).setPreferredWidth(120); // SĐT
        table.getColumnModel().getColumn(2).setPreferredWidth(150); // Tên
        table.getColumnModel().getColumn(3).setPreferredWidth(350); // Nhu cầu (Rộng nhất)
        table.getColumnModel().getColumn(4).setPreferredWidth(150); // Thời gian

        // Ẩn cột FirebaseKey
        table.getColumnModel().getColumn(5).setMinWidth(0);
        table.getColumnModel().getColumn(5).setMaxWidth(0);
        table.getColumnModel().getColumn(5).setWidth(0);

        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // --- PHẦN DƯỚI: THANH TRẠNG THÁI ---
        JPanel panelBottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelBottom.setBackground(new Color(241, 245, 249));
        lblTongSo = new JLabel("Tổng số khách hàng liên hệ: 0");
        lblTongSo.setFont(boldFont);
        panelBottom.add(lblTongSo);
        add(panelBottom, BorderLayout.SOUTH);

        addEvents();
        taiDuLieuNen();
    }

    private void addEvents() {
        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { sorter.setRowFilter(RowFilter.regexFilter("(?i)" + txtTimKiem.getText())); }
            @Override public void removeUpdate(DocumentEvent e) { sorter.setRowFilter(RowFilter.regexFilter("(?i)" + txtTimKiem.getText())); }
            @Override public void changedUpdate(DocumentEvent e) { sorter.setRowFilter(RowFilter.regexFilter("(?i)" + txtTimKiem.getText())); }
        });

        btnLamMoi.addActionListener(e -> {
            txtPhone.setText(""); txtTenKH.setText(""); txtNhuCau.setText("");
            selectedFirebaseKey = "";
            btnXoa.setEnabled(false);
            table.clearSelection();
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = table.getSelectedRow();
                if (viewRow >= 0) {
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    txtPhone.setText(tableModel.getValueAt(modelRow, 1).toString());
                    txtTenKH.setText(tableModel.getValueAt(modelRow, 2).toString());
                    txtNhuCau.setText(tableModel.getValueAt(modelRow, 3).toString());
                    
                    selectedFirebaseKey = tableModel.getValueAt(modelRow, 5).toString();
                    btnXoa.setEnabled(true);
                }
            }
        });

        btnTaiDuLieu.addActionListener(e -> taiDuLieuNen());

        btnThem.addActionListener(e -> {
            String phone = txtPhone.getText().trim();
            String name = txtTenKH.getText().trim();
            String requirement = txtNhuCau.getText().trim();

            if (phone.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ SĐT và Tên Khách!"); return;
            }

            btnThem.setText("Đang đẩy lên mây..."); btnThem.setEnabled(false);
            
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    // Vẫn dùng biến maSV, hoTen, lopHoc để khớp với cấu trúc Database cũ
                    themLenFirebase(phone, name, requirement);
                    return null;
                }
                @Override
                protected void done() {
                    btnThem.setText("Thêm Khách Hàng Mới"); btnThem.setEnabled(true);
                    taiDuLieuNen(); 
                    btnLamMoi.doClick(); 
                    JOptionPane.showMessageDialog(QuanLyKhachHang.this, "Đã lưu khách hàng mới!");
                }
            }.execute();
        });

        btnXoa.addActionListener(e -> {
            if(selectedFirebaseKey.isEmpty()) return;

            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa vĩnh viễn khách hàng này?", "Xác nhận Xóa", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                btnXoa.setText("Đang xóa..."); btnXoa.setEnabled(false);
                
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        xoaTrenFirebase(selectedFirebaseKey);
                        return null;
                    }
                    @Override
                    protected void done() {
                        btnXoa.setText("Xóa Khách Hàng Chọn");
                        taiDuLieuNen(); 
                        btnLamMoi.doClick(); 
                        JOptionPane.showMessageDialog(QuanLyKhachHang.this, "Đã xóa dữ liệu thành công!");
                    }
                }.execute();
            }
        });
    }

    private void taiDuLieuNen() {
        btnTaiDuLieu.setText("Đang kéo Data..."); btnTaiDuLieu.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { layDuLieuTuFirebase(); return null; }
            @Override protected void done() { btnTaiDuLieu.setText("Đồng Bộ Website (Kéo Data)"); btnTaiDuLieu.setEnabled(true); }
        }.execute();
    }

    private void layDuLieuTuFirebase() {
        try {
            URL url = new URL(FIREBASE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) response.append(inputLine);
                in.close();

                String json = response.toString();
                if(json.equals("null")) {
                    SwingUtilities.invokeLater(() -> { tableModel.setRowCount(0); lblTongSo.setText("Tổng số khách hàng liên hệ: 0"); });
                    return;
                }

                SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));

                int stt = 1;
                Matcher mObj = Pattern.compile("\"([a-zA-Z0-9_-]+)\":\\{([^{}]+)\\}").matcher(json);
                
                while (mObj.find()) {
                    String firebaseKey = mObj.group(1); 
                    String rowData = mObj.group(2);    
                    
                    // Vẫn đọc các Key gốc từ Database
                    String phone = layGiaTri(rowData, "maSV");
                    String name = layGiaTri(rowData, "hoTen");
                    String requirement = layGiaTri(rowData, "lopHoc");
                    String time = layGiaTri(rowData, "timestamp");
                    if(time.length() > 10) time = time.substring(0, 10) + " " + time.substring(11, 19);

                    Object[] row = {stt++, phone, name, requirement, time, firebaseKey};
                    SwingUtilities.invokeLater(() -> tableModel.addRow(row));
                }

                int finalTongSo = stt - 1;
                SwingUtilities.invokeLater(() -> lblTongSo.setText("Tổng số khách hàng liên hệ: " + finalTongSo));
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void themLenFirebase(String phone, String name, String requirement) {
        try {
            URL url = new URL(FIREBASE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST"); 
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Gắn dữ liệu vào các Key tương ứng
            String jsonInputString = String.format(
                "{\"maSV\": \"%s\", \"hoTen\": \"%s\", \"lopHoc\": \"%s\", \"timestamp\": \"%s\"}", 
                phone, name, requirement, java.time.Instant.now().toString()
            );

            try(OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            conn.getResponseCode(); 
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void xoaTrenFirebase(String key) {
        try {
            String deleteUrl = FIREBASE_URL.replace(".json", "/" + key + ".json");
            URL url = new URL(deleteUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE"); 
            conn.getResponseCode(); 
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String layGiaTri(String source, String key) {
        Matcher m = Pattern.compile("\"" + key + "\":\"?([^\",}]+)\"?").matcher(source);
        if (m.find()) return m.group(1);
        return "";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuanLyKhachHang().setVisible(true));
    }
}