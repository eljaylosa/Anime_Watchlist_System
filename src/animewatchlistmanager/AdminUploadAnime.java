package animewatchlistmanager;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.sql.*;
import javax.swing.*;



import java.util.Properties;
import java.text.SimpleDateFormat;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

public class AdminUploadAnime extends JFrame {
    private JTextField txtTitle, txtGenre, txtEpisodes, txtSeasons;
    private JTextArea txtDescription;
    private JComboBox<String> cmbStatus;
    private JLabel lblImage;
    private String imagePath;
    private Integer requestId = null;

    private UtilDateModel dateModel;
    private JDatePickerImpl datePicker;

    public AdminUploadAnime() {
        initUI();
    }

    public AdminUploadAnime(int requestId, String title) {
        this.requestId = requestId;
        initUI();
        txtTitle.setText(title);
        loadRequestDetails(requestId);
    }

    private void initUI() {
        setTitle("Upload Anime");
        setSize(450, 750);
        setLocationRelativeTo(null);
        setLayout(null);
        setResizable(false);

        // Labels
        JLabel lblT = new JLabel("Title:");
        lblT.setBounds(20, 20, 100, 25);
        add(lblT);

        JLabel lblG = new JLabel("Genre:");
        lblG.setBounds(20, 60, 100, 25);
        add(lblG);

        JLabel lblE = new JLabel("Episodes:");
        lblE.setBounds(20, 100, 100, 25);
        add(lblE);

        JLabel lblS = new JLabel("Status:");
        lblS.setBounds(20, 140, 100, 25);
        add(lblS);

        JLabel lblDate = new JLabel("Date Aired:");
        lblDate.setBounds(20, 180, 100, 25);
        add(lblDate);

        JLabel lblSeasonsLabel = new JLabel("Seasons:");
        lblSeasonsLabel.setBounds(20, 220, 100, 25);
        add(lblSeasonsLabel);

        JLabel lblDesc = new JLabel("Description:");
        lblDesc.setBounds(20, 260, 100, 25);
        add(lblDesc);

        // Input fields
        txtTitle = new JTextField();
        txtTitle.setBounds(120, 20, 250, 25);
        add(txtTitle);

        txtGenre = new JTextField();
        txtGenre.setBounds(120, 60, 250, 25);
        add(txtGenre);

        txtEpisodes = new JTextField();
        txtEpisodes.setBounds(120, 100, 250, 25);
        add(txtEpisodes);

        cmbStatus = new JComboBox<>(new String[]{"Ongoing", "Completed"});
        cmbStatus.setBounds(120, 140, 250, 25);
        add(cmbStatus);

        // Calendar date picker
        dateModel = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl datePanel = new JDatePanelImpl(dateModel, p);
        datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
        datePicker.setBounds(120, 180, 250, 30);
        add(datePicker);

        txtSeasons = new JTextField();
        txtSeasons.setBounds(120, 220, 250, 25);
        add(txtSeasons);

        txtDescription = new JTextArea();
        txtDescription.setBounds(120, 260, 250, 100);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtDescription.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        add(txtDescription);

        // Poster preview
        lblImage = new JLabel("No image selected", SwingConstants.CENTER);
        lblImage.setBounds(120, 380, 250, 150);
        lblImage.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        add(lblImage);

        JButton btnChooseImage = new JButton("Choose Anime Poster");
        btnChooseImage.setBounds(120, 540, 250, 25);
        btnChooseImage.addActionListener(e -> chooseImage());
        add(btnChooseImage);

        JButton btnUpload = new JButton("Upload Anime");
        btnUpload.setBounds(120, 580, 250, 30);
        btnUpload.addActionListener(e -> uploadAnime());
        add(btnUpload);
    }

    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Anime Image");
        int option = chooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            imagePath = selected.getAbsolutePath();
            lblImage.setText("");
            lblImage.setIcon(resizeImage(imagePath));
        }
    }

    private ImageIcon resizeImage(String path) {
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage().getScaledInstance(250, 150, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    private void uploadAnime() {
        if (txtTitle.getText().isEmpty() || txtGenre.getText().isEmpty() ||
            txtEpisodes.getText().isEmpty() || txtSeasons.getText().isEmpty() ||
            txtDescription.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "⚠️ Please fill out all fields!");
            return;
        }

        String status = (String) cmbStatus.getSelectedItem();
        String dateAired = null;

        if ("Completed".equals(status)) {
            Object selected = datePicker.getModel().getValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "⚠️ Date Aired is required for completed anime!");
                return;
            }
            java.util.Date selectedDate;
            if (selected instanceof java.util.Calendar) {
                selectedDate = ((java.util.Calendar) selected).getTime();
            } else {
                selectedDate = (java.util.Date) selected;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            dateAired = sdf.format(selectedDate);
        }

        if (imagePath == null) {
            JOptionPane.showMessageDialog(this, "⚠️ Please select an image before uploading!");
            return;
        }

        try (Connection conn = DBConnection.connect()) {
            String sql = """
                INSERT INTO anime (title, genre, episodes, status, seasons, description, image_url, date_added, date_aired)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?)
            """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, txtTitle.getText());
            ps.setString(2, txtGenre.getText());
            ps.setInt(3, Integer.parseInt(txtEpisodes.getText()));
            ps.setString(4, status);
            ps.setInt(5, Integer.parseInt(txtSeasons.getText()));
            ps.setString(6, txtDescription.getText());
            ps.setString(7, imagePath);
            ps.setString(8, dateAired);
            ps.executeUpdate();
            
             try (PreparedStatement notifPs = conn.prepareStatement("INSERT INTO notifications (user_id, message, created_at) VALUES (?, ?, NOW())")) {
                 ResultSet rs = conn.createStatement().executeQuery("SELECT user_id FROM users");
                 
                 while(rs.next()) {
                    notifPs.setInt(1, rs.getInt("user_id"));
                    notifPs.setString(2, "🎬️ A new anime has been added to the library! Check it out!");
                    notifPs.addBatch();
                 }
                 notifPs.executeBatch();
             }
             
            NotificationUtil.showToast("📢 Notification sent to all users!");
            JOptionPane.showMessageDialog(this, "✅ Anime uploaded successfully!");
            dispose();
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "❌ Database Error: " + ex.getMessage());
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "⚠️ Episodes and Seasons must be numbers!");
        }
    }

    private void loadRequestDetails(int requestId) {
        try (Connection conn = DBConnection.connect()) {
            String sql = "SELECT title, comment, status FROM anime_requests WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, requestId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                txtTitle.setText(rs.getString("title"));
                cmbStatus.setSelectedItem(rs.getString("status"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading request details: " + e.getMessage());
        }
    }

    // Formatter for JDatePicker
    public class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {
        private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        @Override
        public Object stringToValue(String text) throws java.text.ParseException {
            return dateFormatter.parseObject(text);
        }
        @Override
        public String valueToString(Object value) throws java.text.ParseException {
            if (value != null) {
                if (value instanceof java.util.Calendar) {
                    return dateFormatter.format(((java.util.Calendar) value).getTime());
                }
            }
            return "";
        }
    }
}
