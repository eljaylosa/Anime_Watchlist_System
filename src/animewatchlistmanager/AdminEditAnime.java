package animewatchlistmanager;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Properties;
import javax.swing.*;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

public class AdminEditAnime extends JFrame {
    private JTextField txtTitle, txtGenre, txtEpisodes, txtSeasons;
    private JTextArea txtDescription;
    private JComboBox<String> cmbStatus;
    private JLabel lblImage;
    private String imagePath;
    private int animeId;

    private UtilDateModel dateModel;
    private JDatePickerImpl datePicker;

    public AdminEditAnime(int animeId) {
        this.animeId = animeId;
        initUI();
        loadAnimeDetails();
    }

    private void initUI() {
        setTitle("Edit Anime");
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

        // Inputs
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

        // Date Picker
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

        lblImage = new JLabel("No image selected", SwingConstants.CENTER);
        lblImage.setBounds(120, 380, 250, 150);
        lblImage.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        add(lblImage);

        JButton btnChooseImage = new JButton("Change Anime Poster");
        btnChooseImage.setBounds(120, 540, 250, 25);
        btnChooseImage.addActionListener(e -> chooseImage());
        add(btnChooseImage);

        JButton btnSave = new JButton("💾 Save Changes");
        btnSave.setBounds(120, 580, 250, 30);
        btnSave.addActionListener(e -> updateAnime());
        add(btnSave);
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

    private void loadAnimeDetails() {
        try (Connection conn = DBConnection.connect()) {
            String sql = "SELECT * FROM anime WHERE anime_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, animeId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                txtTitle.setText(rs.getString("title"));
                txtGenre.setText(rs.getString("genre"));
                txtEpisodes.setText(String.valueOf(rs.getInt("episodes")));
                cmbStatus.setSelectedItem(rs.getString("status"));
                txtSeasons.setText(String.valueOf(rs.getInt("seasons")));
                txtDescription.setText(rs.getString("description"));
                imagePath = rs.getString("image_url");

                // Load date_aired if exists
                Date dateAired = rs.getDate("date_aired");
                if (dateAired != null) {
                    dateModel.setValue(dateAired);
                }

                if (imagePath != null && !imagePath.isEmpty()) {
                    lblImage.setText("");
                    lblImage.setIcon(resizeImage(imagePath));
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading anime details: " + e.getMessage());
        }
    }

    private void updateAnime() {
        if (txtTitle.getText().isEmpty() || txtGenre.getText().isEmpty() ||
            txtEpisodes.getText().isEmpty() || txtSeasons.getText().isEmpty() ||
            txtDescription.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "⚠️ Please fill out all fields!");
            return;
        }

        String status = (String) cmbStatus.getSelectedItem();
        String dateAiredStr = null;

        if ("Completed".equals(status)) {
            Object selected = datePicker.getModel().getValue();
            if (selected != null) {
                java.util.Date selectedDate;
                if (selected instanceof java.util.Calendar) {
                    selectedDate = ((java.util.Calendar) selected).getTime();
                } else {
                    selectedDate = (java.util.Date) selected;
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                dateAiredStr = sdf.format(selectedDate);
            } else {
                JOptionPane.showMessageDialog(this, "⚠️ Date Aired is required for Completed anime!");
                return;
            }
        }

        try (Connection conn = DBConnection.connect()) {
            String sql = """
                UPDATE anime
                SET title = ?, genre = ?, episodes = ?, status = ?, seasons = ?, description = ?, image_url = ?, date_aired = ?
                WHERE anime_id = ?
            """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, txtTitle.getText());
            ps.setString(2, txtGenre.getText());
            ps.setInt(3, Integer.parseInt(txtEpisodes.getText()));
            ps.setString(4, status);
            ps.setInt(5, Integer.parseInt(txtSeasons.getText()));
            ps.setString(6, txtDescription.getText());
            ps.setString(7, imagePath);
            ps.setString(8, dateAiredStr);
            ps.setInt(9, animeId);

            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "✅ Anime updated successfully!");
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "❌ Database Error: " + ex.getMessage());
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "⚠️ Episodes and Seasons must be numbers!");
        }
    }

    private ImageIcon resizeImage(String path) {
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage().getScaledInstance(250, 150, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    // ---------------- DateLabelFormatter ----------------
    public class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {
        private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        @Override
        public Object stringToValue(String text) throws java.text.ParseException {
            return dateFormatter.parse(text);
        }

        @Override
        public String valueToString(Object value) throws java.text.ParseException {
            if (value != null) {
                if (value instanceof java.util.Calendar) {
                    return dateFormatter.format(((java.util.Calendar) value).getTime());
                } else if (value instanceof java.util.Date) {
                    return dateFormatter.format((java.util.Date) value);
                }
            }
            return "";
        }
    }
}
