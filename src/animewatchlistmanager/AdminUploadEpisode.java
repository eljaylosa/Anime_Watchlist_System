package animewatchlistmanager;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class AdminUploadEpisode extends JFrame {

    private JComboBox<String> cmbAnime;
    private JTextField txtEpisodeNumber, txtTitle;
    private JLabel lblFile, lblPoster;
    private String localFilePath = null;

    private final Map<String, Integer> animeTitleToId = new HashMap<>();

    public AdminUploadEpisode() {
        setTitle("Upload Anime Episode");
        setSize(600, 400);
        setLayout(null);
        setLocationRelativeTo(null);
        setResizable(false);

        JLabel lblA = new JLabel("Anime Title:");
        lblA.setBounds(30, 30, 120, 25);
        add(lblA);

        cmbAnime = new JComboBox<>();
        cmbAnime.setEditable(true);
        cmbAnime.setBounds(150, 30, 250, 25);
        loadAnimeTitles();
        enableAutocompleteFilter();
        add(cmbAnime);

        JLabel lblE = new JLabel("Episode #: ");
        lblE.setBounds(30, 70, 120, 25);
        add(lblE);

        txtEpisodeNumber = new JTextField();
        txtEpisodeNumber.setBounds(150, 70, 250, 25);
        add(txtEpisodeNumber);

        JLabel lblT = new JLabel("Episode Title:");
        lblT.setBounds(30, 110, 120, 25);
        add(lblT);

        txtTitle = new JTextField();
        txtTitle.setBounds(150, 110, 250, 25);
        add(txtTitle);

        lblFile = new JLabel("No file selected");
        lblFile.setBounds(150, 150, 250, 25);
        add(lblFile);

        JButton btnChoose = new JButton("Choose Video File");
        btnChoose.setBounds(150, 180, 250, 25);
        btnChoose.addActionListener(e -> chooseFile());
        add(btnChoose);

        JButton btnUpload = new JButton("Upload Episode");
        btnUpload.setBounds(150, 220, 250, 30);
        btnUpload.addActionListener(e -> uploadEpisode());
        add(btnUpload);

        // 🖼 Poster Preview
        lblPoster = new JLabel("No Poster", SwingConstants.CENTER);
        lblPoster.setBounds(420, 30, 140, 180);
        lblPoster.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        add(lblPoster);

        // 🎯 Load poster automatically when anime is selected
        cmbAnime.addActionListener(e -> loadPosterImage());
    }

    private void loadAnimeTitles() {
    try (Connection conn = DBConnection.connect();
         PreparedStatement ps = conn.prepareStatement("SELECT anime_id, title FROM anime");
         ResultSet rs = ps.executeQuery()) {

        boolean firstItemSet = false;

        while (rs.next()) {
            int id = rs.getInt("anime_id");
            String title = rs.getString("title");
            animeTitleToId.put(title, id);
            cmbAnime.addItem(title);

            // ✅ Automatically select the first anime
            if (!firstItemSet) {
                cmbAnime.setSelectedItem(title);
                firstItemSet = true;
            }
        }

        // ✅ Show poster immediately for the first anime
        if (cmbAnime.getItemCount() > 0) {
            SwingUtilities.invokeLater(this::loadPosterImage);
        }

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error loading anime list: " + e.getMessage());
    }
}


    private void enableAutocompleteFilter() {
        JTextComponent editor = (JTextComponent) cmbAnime.getEditor().getEditorComponent();

        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                SwingUtilities.invokeLater(() -> {
                    String input = editor.getText().toLowerCase();
                    cmbAnime.hidePopup();
                    cmbAnime.removeAllItems();

                    if (input.isEmpty()) {
                        for (String title : animeTitleToId.keySet()) {
                            cmbAnime.addItem(title);
                        }
                    } else {
                        for (String title : animeTitleToId.keySet()) {
                            if (title.toLowerCase().contains(input)) {
                                cmbAnime.addItem(title);
                            }
                        }
                    }

                    editor.setText(input);
                    cmbAnime.getEditor().setItem(editor.getText());
                    cmbAnime.showPopup();
                });
            }
        });
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        int option = chooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            localFilePath = file.getAbsolutePath();
            lblFile.setText(file.getName());
        }
    }

    private void loadPosterImage() {
        String selectedTitle = (String) cmbAnime.getSelectedItem();
        if (selectedTitle == null || !animeTitleToId.containsKey(selectedTitle)) return;

        int animeId = animeTitleToId.get(selectedTitle);

        try (Connection conn = DBConnection.connect()) {
            String sql = "SELECT image_url FROM anime WHERE anime_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, animeId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String imagePath = rs.getString("image_url");
                if (imagePath != null && !imagePath.isEmpty()) {
                    ImageIcon icon = new ImageIcon(new ImageIcon(imagePath)
                            .getImage().getScaledInstance(140, 180, Image.SCALE_SMOOTH));
                    lblPoster.setText("");
                    lblPoster.setIcon(icon);
                } else {
                    lblPoster.setText("No Poster");
                    lblPoster.setIcon(null);
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading poster: " + e.getMessage());
        }
    }

    private void uploadEpisode() {
        if (localFilePath == null) {
            JOptionPane.showMessageDialog(this, "⚠️ Please choose a video file first!");
            return;
        }

        try (Connection conn = DBConnection.connect()) {
            String animeTitle = (String) cmbAnime.getSelectedItem();
            if (animeTitle == null || !animeTitleToId.containsKey(animeTitle)) {
                JOptionPane.showMessageDialog(this, "⚠️ Please select a valid anime title!");
                return;
            }

            int animeId = animeTitleToId.get(animeTitle);

            String sql = """
                INSERT INTO anime_episodes (anime_id, episode_number, title, local_file_path)
                VALUES (?, ?, ?, ?)
            """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, animeId);
            ps.setInt(2, Integer.parseInt(txtEpisodeNumber.getText()));
            ps.setString(3, txtTitle.getText());
            ps.setString(4, localFilePath);
            ps.executeUpdate();

            try (PreparedStatement notifPs = conn.prepareStatement(
                    "INSERT INTO notifications (user_id, message, created_at) VALUES (?, ?, NOW())")) {

                ResultSet rs = conn.createStatement().executeQuery("SELECT user_id FROM users");
                while (rs.next()) {
                    notifPs.setInt(1, rs.getInt("user_id"));
                    notifPs.setString(2, "📺 New episode uploaded for " + animeTitle +" — Episode " + txtEpisodeNumber.getText() + "!");
                    notifPs.addBatch();
                }
                notifPs.executeBatch();
            }

            NotificationUtil.showToast("📢 Notification sent to all users!");
            JOptionPane.showMessageDialog(this, "✅ Episode uploaded successfully!");

            txtEpisodeNumber.setText("");
            txtTitle.setText("");
            lblFile.setText("No file selected");
            localFilePath = null;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "❌ Database Error: " + e.getMessage());
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "⚠️ Episode number must be a valid number!");
        }
    }
}
