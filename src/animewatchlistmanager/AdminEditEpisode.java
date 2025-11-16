package animewatchlistmanager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.*;

public class AdminEditEpisode extends JFrame {

    private JTextField txtEpisodeNumber, txtTitle;
    private JLabel lblFile;
    private String localFilePath = null;
    private final int episodeId;

    public AdminEditEpisode(int episodeId) {
        this.episodeId = episodeId;

        setTitle("Edit Episode");
        setSize(500, 300);
        setLayout(null);
        setLocationRelativeTo(null);
        setResizable(false);

        // Episode Number
        JLabel lblE = new JLabel("Episode #: ");
        lblE.setBounds(30, 30, 120, 25);
        add(lblE);

        txtEpisodeNumber = new JTextField();
        txtEpisodeNumber.setBounds(150, 30, 250, 25);
        add(txtEpisodeNumber);

        // Episode Title
        JLabel lblT = new JLabel("Episode Title:");
        lblT.setBounds(30, 70, 120, 25);
        add(lblT);

        txtTitle = new JTextField();
        txtTitle.setBounds(150, 70, 250, 25);
        add(txtTitle);

        // Video File
        lblFile = new JLabel("No file selected");
        lblFile.setBounds(150, 110, 250, 25);
        add(lblFile);

        JButton btnChoose = new JButton("Choose Video File");
        btnChoose.setBounds(150, 140, 250, 25);
        btnChoose.addActionListener(e -> chooseFile());
        add(btnChoose);

        // Save Button
        JButton btnSave = new JButton("Save Changes");
        btnSave.setBounds(150, 180, 250, 30);
        btnSave.addActionListener(e -> saveChanges());
        add(btnSave);

        loadEpisodeData();
    }

    private void loadEpisodeData() {
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT episode_number, title, local_file_path FROM anime_episodes WHERE id = ?")) {
            ps.setInt(1, episodeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                txtEpisodeNumber.setText(String.valueOf(rs.getInt("episode_number")));
                txtTitle.setText(rs.getString("title"));
                localFilePath = rs.getString("local_file_path");
                lblFile.setText(localFilePath != null ? new File(localFilePath).getName() : "No file selected");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading episode: " + e.getMessage());
        }
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

    private void saveChanges() {
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE anime_episodes SET episode_number = ?, title = ?, local_file_path = ? WHERE id = ?")) {

            ps.setInt(1, Integer.parseInt(txtEpisodeNumber.getText()));
            ps.setString(2, txtTitle.getText());
            ps.setString(3, localFilePath);
            ps.setInt(4, episodeId);

            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "✅ Episode updated successfully!");
            dispose();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating episode: " + e.getMessage());
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "⚠️ Episode number must be a valid number!");
        }
    }
}
