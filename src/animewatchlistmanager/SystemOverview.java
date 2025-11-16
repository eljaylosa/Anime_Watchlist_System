package animewatchlistmanager;

import java.sql.*;
import javax.swing.*;

public class SystemOverview extends javax.swing.JFrame {

    public SystemOverview() {
        initComponents();
        loadSystemOverview();
        AppIcon.setAppIcon(this);

        jButton1.addActionListener(e -> loadSystemOverview());
    }

    private void loadSystemOverview() {
        try (Connection con = DBConnection.connect()) {

            // Total Users
            PreparedStatement pstUsers = con.prepareStatement("SELECT COUNT(*) AS total FROM users");
            ResultSet rsUsers = pstUsers.executeQuery();
            if (rsUsers.next()) {
                lblUsersCount.setText("👤 Total Users: " + rsUsers.getInt("total"));
            }

            // Total Anime
            PreparedStatement pstAnime = con.prepareStatement("SELECT COUNT(*) AS total FROM anime");
            ResultSet rsAnime = pstAnime.executeQuery();
            if (rsAnime.next()) {
                lblAnimeCount.setText("🎬 Total Anime: " + rsAnime.getInt("total"));
            }

            // Total Episodes Uploaded
            PreparedStatement pstEpisodes = con.prepareStatement("SELECT COUNT(*) AS total FROM anime_episodes");
            ResultSet rsEpisodes = pstEpisodes.executeQuery();
            if (rsEpisodes.next()) {
                lblEpisodesWatched.setText("📺 Total Episodes Uploaded: " + rsEpisodes.getInt("total"));
            }

            // Total Messages
            PreparedStatement pstMsg = con.prepareStatement("SELECT COUNT(*) AS total FROM messages");
            ResultSet rsMsg = pstMsg.executeQuery();
            if (rsMsg.next()) {
                lblMessagesCount.setText("💬 Total Messages: " + rsMsg.getInt("total"));
            }

            // Total Activity Logs
            PreparedStatement pstLogs = con.prepareStatement("SELECT COUNT(*) AS total FROM activity_logs");
            ResultSet rsLogs = pstLogs.executeQuery();
            if (rsLogs.next()) {
                lblLogsCount.setText("📘 Total Logs: " + rsLogs.getInt("total"));
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "⚠️ Error loading overview: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lblUsersCount = new javax.swing.JLabel();
        lblAnimeCount = new javax.swing.JLabel();
        lblEpisodesWatched = new javax.swing.JLabel();
        lblMessagesCount = new javax.swing.JLabel();
        lblLogsCount = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("System Overview");
        setResizable(false); // Prevent resizing

        jPanel1.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        lblUsersCount.setFont(new java.awt.Font("Century Gothic", 0, 14));
        lblAnimeCount.setFont(new java.awt.Font("Century Gothic", 0, 14));
        lblEpisodesWatched.setFont(new java.awt.Font("Century Gothic", 0, 14));
        lblMessagesCount.setFont(new java.awt.Font("Century Gothic", 0, 14));
        lblLogsCount.setFont(new java.awt.Font("Century Gothic", 0, 14));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(lblUsersCount)
                        .addComponent(lblAnimeCount)
                        .addComponent(lblEpisodesWatched)
                        .addComponent(lblMessagesCount)
                        .addComponent(lblLogsCount))
                    .addContainerGap(50, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(15)
                    .addComponent(lblUsersCount)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(lblAnimeCount)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(lblEpisodesWatched)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(lblMessagesCount)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(lblLogsCount)
                    .addContainerGap(15, Short.MAX_VALUE))
        );

        jLabel1.setFont(new java.awt.Font("Century Gothic", 1, 16));
        jLabel1.setText("🧾 System Overview");

        jButton1.setFont(new java.awt.Font("Century Gothic", 0, 14));
        jButton1.setText("🔄 Refresh");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabel1)
                            .addGap(18)
                            .addComponent(jButton1)))
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(jButton1))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(15, Short.MAX_VALUE))
        );

        // Set window size and center it
        setSize(450, 300);
        setLocationRelativeTo(null);
    }

    // Variables declaration
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lblAnimeCount;
    private javax.swing.JLabel lblEpisodesWatched;
    private javax.swing.JLabel lblLogsCount;
    private javax.swing.JLabel lblMessagesCount;
    private javax.swing.JLabel lblUsersCount;
}
