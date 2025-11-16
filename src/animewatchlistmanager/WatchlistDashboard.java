/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package animewatchlistmanager;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import javax.swing.JOptionPane;
import java.sql.*;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import org.json.JSONArray;
import org.json.JSONObject;
import animewatchlistmanager.AnimeAPI;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.util.List;
/**
 *
 * @author ljlosa
 */
public class WatchlistDashboard extends javax.swing.JFrame {

    /**
     * Creates new form WatchlistDashboard
     */
    
   
    private int userId;
    String title = "SELECT INTO watchlist WHERE title";
    String username;
    public WatchlistDashboard(int userId, String username) {
        initComponents();
        this.userId = userId;
        this.username = username;
        lblUsername.setText(username + "!");
        displayProfilePic(username);
        loadAnimeList();
        
        JLabel logo = LogoHelper.getAppLogo(120);
        lblLogo.setIcon(((ImageIcon) ((JLabel) logo).getIcon()));
        lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
        AppIcon.setAppIcon(this);
        
        tblAnime.addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            if (evt.getClickCount() == 2 && tblAnime.getSelectedRow() != -1) {
                int selectedRow = tblAnime.getSelectedRow();

                // 👇 Paste your existing logic here
                int animeId = (int) tblAnime.getValueAt(selectedRow, 0);

                try (Connection conn = DBConnection.connect()) {
                    String sql = "SELECT local_file_path FROM anime_episodes WHERE anime_id = ? ORDER BY episode_number ASC LIMIT 1";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setInt(1, animeId);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        String path = rs.getString("local_file_path");
                        if (path == null || path.isEmpty()) {
                            JOptionPane.showMessageDialog(null, "⚠️ No video file found for this anime!");
                        } else {
                            WatchAnimeFrame frame = new WatchAnimeFrame(animeId, userId);
                            frame.setVisible(true);
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "⚠️ No episodes found for this anime!");
                    }

                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
                }
            }
        }
    });
   

    }
    public void updateUsernameLabel(String newUsername) {
    lblUsername.setText(newUsername);
    }
  private void sortWatchlist(String criteria) {
    String sql = "";

    switch (criteria) {
        case "Date":
            sql = "SELECT anime_id, title, genre, episodes, status, " +
                  "CASE WHEN status = 'Completed' THEN rating ELSE 0.0 END AS rating, " +
                  "image_url, link, date_added FROM watchlist WHERE user_id = ? ORDER BY date_added DESC";
            break;
        case "A-Z":
            sql = "SELECT anime_id, title, genre, episodes, status, " +
                  "CASE WHEN status = 'Completed' THEN rating ELSE 0.0 END AS rating, " +
                  "image_url, link, date_added FROM watchlist WHERE user_id = ? ORDER BY title ASC";
            break;
        case "Most Episode":
            sql = "SELECT anime_id, title, genre, episodes, status, " +
                  "CASE WHEN status = 'Completed' THEN rating ELSE 0.0 END AS rating, " +
                  "image_url, link, date_added FROM watchlist WHERE user_id = ? ORDER BY episodes DESC";
            break;
        case "Most Rating":
            sql = "SELECT anime_id, title, genre, episodes, status, " +
                  "CASE WHEN status = 'Completed' THEN rating ELSE 0.0 END AS rating, " +
                  "image_url, link, date_added FROM watchlist WHERE user_id = ? ORDER BY rating DESC";
            break;
        case "By Status":
            sql = "SELECT anime_id, title, genre, episodes, status, " +
                  "CASE WHEN status = 'Completed' THEN rating ELSE 0.0 END AS rating, " +
                  "image_url, link, date_added FROM watchlist WHERE user_id = ? " +
                  "ORDER BY FIELD(status, 'Watching', 'On Hold', 'Plan to Watch', 'Dropped', 'Completed')";
            break;
        default:
            sql = "SELECT anime_id, title, genre, episodes, status, " +
                  "CASE WHEN status = 'Completed' THEN rating ELSE 0.0 END AS rating, " +
                  "image_url, link, date_added FROM watchlist WHERE user_id = ?";
            break;
    }

    try (Connection con = DBConnection.connect();
         PreparedStatement pst = con.prepareStatement(sql)) {

        pst.setInt(1, userSession.currentUserId);
        ResultSet rs = pst.executeQuery();

        // ✅ Rebuild table model with image icons
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 1) return ImageIcon.class; // column 1 = image
                return Object.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // ✅ Table columns
        model.addColumn("Anime ID");
        model.addColumn("Poster");
        model.addColumn("Title");
        model.addColumn("Genre");
        model.addColumn("Episodes");
        model.addColumn("Status");
        model.addColumn("Rating");
        model.addColumn("Link"); // hidden later

        // ✅ Fill table
        while (rs.next()) {
            String imagePath = rs.getString("image_url");
            ImageIcon icon = null;
            if (imagePath != null && !imagePath.isEmpty()) {
                ImageIcon originalIcon = new ImageIcon(imagePath);
                Image img = originalIcon.getImage().getScaledInstance(80, 100, Image.SCALE_SMOOTH);
                icon = new ImageIcon(img);
            }

            model.addRow(new Object[]{
                rs.getInt("anime_id"),
                icon,
                rs.getString("title"),
                rs.getString("genre"),
                rs.getInt("episodes"),
                rs.getString("status"),
                rs.getDouble("rating"),
                rs.getString("link") // hidden later
            });
        }

        tblAnime.setModel(model);

        // ✅ Hide link column
        tblAnime.getColumnModel().getColumn(7).setMinWidth(0);
        tblAnime.getColumnModel().getColumn(7).setMaxWidth(0);
        tblAnime.getColumnModel().getColumn(7).setWidth(0);

        // ✅ Adjust image column size
        tblAnime.setRowHeight(100);
        tblAnime.getColumnModel().getColumn(1).setPreferredWidth(80);

    } catch (Exception ex) {
        JOptionPane.showMessageDialog(null, "Error Sorting: " + ex.getMessage());
    }
}



    /*
Watching
Completed
On Hold
Dropped
Plan to Watch
    */
   private void searchAnime(String keyword) {
    try (Connection conn = DBConnection.connect()) {
        String sql = "SELECT anime_id, image_url, title, genre, episodes, status, rating " +
                     "FROM watchlist WHERE user_id = ? AND " +
                     "(CAST(anime_id AS CHAR) LIKE ? OR title LIKE ? OR genre LIKE ? OR " +
                     "CAST(episodes AS CHAR) LIKE ? OR status LIKE ? OR CAST(rating AS CHAR) LIKE ?)";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, userSession.currentUserId);
        for (int i = 2; i <= 7; i++) {
            pst.setString(i, "%" + keyword + "%");
        }

        ResultSet rs = pst.executeQuery();

        DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Anime ID", "Poster", "Title", "Genre", "Episodes", "Status", "Rating"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return (columnIndex == 1) ? ImageIcon.class : Object.class;
            }
        };

        while (rs.next()) {
            String imageUrl = rs.getString("image_url");
            String status = rs.getString("status");
            double rating = rs.getDouble("rating");

            if (!"Completed".equalsIgnoreCase(status)) {
                rating = 0.0;
            }

            ImageIcon icon = null;
            try {
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Image img;

                    // ✅ Support for both URL and local file paths
                    if (imageUrl.startsWith("http")) {
                        img = new ImageIcon(new java.net.URL(imageUrl)).getImage();
                    } else {
                        img = new ImageIcon(imageUrl).getImage(); // local path
                    }

                    img = img.getScaledInstance(70, 100, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(img);
                }
            } catch (Exception e) {
                // fallback image if URL or path is invalid
                BufferedImage placeholder = new BufferedImage(70, 100, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = placeholder.createGraphics();
                g2.setColor(Color.LIGHT_GRAY);
                g2.fillRect(0, 0, 70, 100);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("No Img", 10, 50);
                g2.dispose();
                icon = new ImageIcon(placeholder);
            }

            model.addRow(new Object[]{
                rs.getInt("anime_id"),
                icon,
                rs.getString("title"),
                rs.getString("genre"),
                rs.getInt("episodes"),
                status,
                rating
            });
        }

        tblAnime.setRowHeight(100);
        tblAnime.setModel(model);

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, e.getMessage());
    }
}





   
   private void deleteSelectedAnime() {
    int selectedRow = tblAnime.getSelectedRow(); // your table variable
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(null, "Please select an anime to delete.");
        return;
    }

    // Safely get anime ID and title
    Object idObj = tblAnime.getValueAt(selectedRow, 0);
    Object titleObj = tblAnime.getValueAt(selectedRow, 1);

    int animeId;
    try {
        animeId = Integer.parseInt(String.valueOf(idObj));
    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(null, "Invalid anime ID.");
        return;
    }

    String title = (titleObj != null) ? titleObj.toString() : "Unknown Anime";

    // ✅ Only a plain string message
    int confirm = JOptionPane.showConfirmDialog(
            null,
            "Are you sure you want to delete this anime?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
    );

    if (confirm != JOptionPane.YES_OPTION) {
        return; // user cancelled
    }

    try (Connection conn = DBConnection.connect()) {
        // Only delete for the current logged-in user
        String sql = "DELETE FROM watchlist WHERE anime_id = ? AND user_id = ?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, animeId);
        pst.setInt(2, userId);

        int rowsDeleted = pst.executeUpdate();

        if (rowsDeleted > 0) {
            JOptionPane.showMessageDialog(null, "✅ Anime deleted successfully!");
            loadAnimeList(); // refresh table

            // Optional: log the deletion activity
            ActivityLogger.log(userId, username, "Deleted anime from watchlist: " + title);
        } else {
            JOptionPane.showMessageDialog(null, "⚠️ Failed to delete anime. It may not exist in your watchlist.");
        }

    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(null, "Error deleting anime: " + ex.getMessage());
    }
}



    
   public void loadAnimeList() {
    try (Connection conn = DBConnection.connect()) {
        String sql = "SELECT anime_id, image_url, title, genre, episodes, status, " +
                     "CASE WHEN status = 'Completed' THEN rating ELSE 0.0 END AS rating, " +
                     "image_url, link " +
                     "FROM watchlist WHERE user_id = ?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, userId);
        ResultSet rs = pst.executeQuery();

        // Custom table model that allows image icons
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 1) return ImageIcon.class; // column 1 = image
                return Object.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Add columns
        model.addColumn("Anime ID");
        model.addColumn("Poster");
        model.addColumn("Title");
        model.addColumn("Genre");
        model.addColumn("Episodes");
        model.addColumn("Status");
        model.addColumn("Rating");
        model.addColumn("Link"); // hidden later

        // Populate rows
        while (rs.next()) {
            String imagePath = rs.getString("image_url");
            ImageIcon icon = null;
            if (imagePath != null && !imagePath.isEmpty()) {
                ImageIcon originalIcon = new ImageIcon(imagePath);
                Image img = originalIcon.getImage().getScaledInstance(80, 100, Image.SCALE_SMOOTH);
                icon = new ImageIcon(img);
            }

            model.addRow(new Object[]{
                rs.getInt("anime_id"),
                icon,
                rs.getString("title"),
                rs.getString("genre"),
                rs.getInt("episodes"),
                rs.getString("status"),
                rs.getDouble("rating"),
                rs.getString("link") // keep this hidden
            });
        }

        tblAnime.setModel(model);

        // Hide the link column
        tblAnime.getColumnModel().getColumn(7).setMinWidth(0);
        tblAnime.getColumnModel().getColumn(7).setMaxWidth(0);
        tblAnime.getColumnModel().getColumn(7).setWidth(0);

        // Adjust image column width
        tblAnime.setRowHeight(100);
        tblAnime.getColumnModel().getColumn(1).setPreferredWidth(80);

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error loading anime list: " + e.getMessage());
    }
}



    
    
    private void displayProfilePic(String username) {
    try (Connection conn = DBConnection.connect()) {
        String sql = "SELECT profile_pic FROM users WHERE username = ?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, username);
        ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            byte[] imgBytes = rs.getBytes("profile_pic");
            if (imgBytes != null) {
                ImageIcon image = new ImageIcon(imgBytes);
                Image scaledImage = image.getImage().getScaledInstance(
                        lblProfilePic.getWidth(), lblProfilePic.getHeight(), Image.SCALE_SMOOTH);
                lblProfilePic.setIcon(new ImageIcon(scaledImage));
            } else {
                lblProfilePic.setIcon(null);
            }
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage());
    }
}

    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSlider1 = new javax.swing.JSlider();
        jPanel1 = new javax.swing.JPanel();
        btnEdit = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblAnime = new javax.swing.JTable();
        btnAdd = new javax.swing.JButton();
        btnWatch = new javax.swing.JButton();
        txtSearch = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        sortCmb = new javax.swing.JComboBox<>();
        btnDelete = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        lblProfilePic = new javax.swing.JLabel();
        btnUpload = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        lblUsername = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        lblLogo = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtAdmin = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Anime Dashboard");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jPanel1.setBackground(new java.awt.Color(35, 76, 106));
        jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel1.setForeground(new java.awt.Color(204, 204, 255));

        btnEdit.setFont(new java.awt.Font("Century Gothic", 0, 14)); // NOI18N
        btnEdit.setText("Edit list");
        btnEdit.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnEdit.setFocusable(false);
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditActionPerformed(evt);
            }
        });

        tblAnime.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        tblAnime.setFont(new java.awt.Font("Century Gothic", 0, 12)); // NOI18N
        tblAnime.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Anime ID", "Title", "Genre", "Episodes", "Status", "Rating"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblAnime.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        tblAnime.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        tblAnime.setRowHeight(40);
        jScrollPane1.setViewportView(tblAnime);

        btnAdd.setFont(new java.awt.Font("Century Gothic", 0, 14)); // NOI18N
        btnAdd.setText("Anime library");
        btnAdd.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnAdd.setFocusable(false);
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });

        btnWatch.setFont(new java.awt.Font("Century Gothic", 0, 14)); // NOI18N
        btnWatch.setText("Watch Now");
        btnWatch.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnWatch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnWatchActionPerformed(evt);
            }
        });

        txtSearch.setFont(new java.awt.Font("Century Gothic", 0, 14)); // NOI18N
        txtSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearchActionPerformed(evt);
            }
        });
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Century Gothic", 0, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Your anime watchlist ");

        jLabel2.setFont(new java.awt.Font("Century Gothic", 0, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Search");

        sortCmb.setFont(new java.awt.Font("Century Gothic", 0, 14)); // NOI18N
        sortCmb.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Sort by", "Date", "A-Z", "Most Episode", "Most Rating", "By Status" }));
        sortCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sortCmbActionPerformed(evt);
            }
        });

        btnDelete.setFont(new java.awt.Font("Century Gothic", 0, 14)); // NOI18N
        btnDelete.setText("Delete");
        btnDelete.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnDelete.setFocusable(false);
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });

        jButton1.setText("Request Anime");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton3.setText("Back to Login");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1482, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(363, 363, 363)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sortCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jButton3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnAdd)
                                .addGap(18, 18, 18)
                                .addComponent(btnEdit)
                                .addGap(18, 18, 18)
                                .addComponent(btnDelete)
                                .addGap(518, 518, 518)))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnWatch, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(48, 48, 48)
                                .addComponent(jButton1)))))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(sortCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addGap(10, 10, 10)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnWatch)
                    .addComponent(btnAdd)
                    .addComponent(btnEdit)
                    .addComponent(btnDelete)
                    .addComponent(jButton3))
                .addContainerGap())
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        lblProfilePic.setMaximumSize(new java.awt.Dimension(100, 16));
        lblProfilePic.setPreferredSize(new java.awt.Dimension(120, 120));

        btnUpload.setFont(new java.awt.Font("Century Gothic", 0, 12)); // NOI18N
        btnUpload.setText("Change Profile");
        btnUpload.setFocusable(false);
        btnUpload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUploadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(21, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblProfilePic, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnUpload, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(17, 17, 17))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(37, 37, 37)
                .addComponent(lblProfilePic, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnUpload)
                .addContainerGap())
        );

        jPanel3.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jLabel1.setFont(new java.awt.Font("Century Gothic", 0, 24)); // NOI18N
        jLabel1.setText("Hello,");

        lblUsername.setFont(new java.awt.Font("Century Gothic", 1, 36)); // NOI18N
        lblUsername.setText("username");
        lblUsername.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblUsername.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblUsernameMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                lblUsernameMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                lblUsernameMouseExited(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(lblUsername))
                .addContainerGap(201, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblUsername)
                .addContainerGap())
        );

        jLabel4.setFont(new java.awt.Font("Century Gothic", 1, 48)); // NOI18N
        jLabel4.setText("Kitsu");

        jLabel5.setFont(new java.awt.Font("Century Gothic", 2, 48)); // NOI18N
        jLabel5.setText("Link");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(lblLogo, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addGap(42, 42, 42))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(0, 50, Short.MAX_VALUE)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)))
                    .addComponent(lblLogo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        txtAdmin.setFont(new java.awt.Font("Century Gothic", 3, 14)); // NOI18N
        txtAdmin.setText("Contact Admin");
        txtAdmin.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        txtAdmin.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                txtAdminMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                txtAdminMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                txtAdminMouseExited(evt);
            }
        });

        jButton2.setText("Notifications");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(16, 16, 16)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(196, 196, 196)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(25, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(txtAdmin)
                .addGap(54, 54, 54))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(txtAdmin)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        // TODO add your handling code here:
        deleteSelectedAnime();
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        // TODO add your handling code here:
        //AddNew addFrame = new AddNew(this, userId);
        //addFrame.setVisible(true);
        new AnimeLibraryFrame(userId, this, false).setVisible(true);


    }//GEN-LAST:event_btnAddActionPerformed

    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditActionPerformed
        // TODO add your handling code here:
        int selectedRow = tblAnime.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row to edit.");
            return;
        }

        int animeId = (int) tblAnime.getValueAt(selectedRow, 0);  // ✅ still ID
        // skip the image column
        String title = (String) tblAnime.getValueAt(selectedRow, 2);
        String genre = (String) tblAnime.getValueAt(selectedRow, 3);
        int episodes = Integer.parseInt(tblAnime.getValueAt(selectedRow, 4).toString());
        String status = (String) tblAnime.getValueAt(selectedRow, 5);

        // handle rating safely
        Object ratingObj = tblAnime.getValueAt(selectedRow, 6);
        double rating = 0.0;
        if (ratingObj != null && !ratingObj.toString().trim().isEmpty()) {
            try {
                rating = Double.parseDouble(ratingObj.toString());
            } catch (NumberFormatException e) {
                rating = 0.0;
            }
        }

        Edit editFrame = new Edit(this, animeId, title, genre, episodes, status, rating);
        editFrame.setVisible(true);
    }//GEN-LAST:event_btnEditActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        new LoginForm().setVisible(true);
    }//GEN-LAST:event_formWindowClosing

    private void btnUploadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUploadActionPerformed
        // TODO add your handling code here:
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose Profile Picture");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    
    int result = fileChooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        try {
            FileInputStream fis = new FileInputStream(file);
            Connection conn = DBConnection.connect();
            
            String sql = "UPDATE users SET profile_pic = ? WHERE username = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setBinaryStream(1, fis, (int) file.length());
            pst.setString(2, username); // replace with your logged-in username
            pst.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Profile picture updated!");
            displayProfilePic(username);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }
    }//GEN-LAST:event_btnUploadActionPerformed

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
        // TODO add your handling code here:
        String keyword = txtSearch.getText().trim();
        searchAnime(keyword);
    }//GEN-LAST:event_txtSearchKeyReleased

    private void sortCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sortCmbActionPerformed
        // TODO add your handling code here:
        String selected = sortCmb.getSelectedItem().toString();
        sortWatchlist(selected);
    }//GEN-LAST:event_sortCmbActionPerformed

    private void txtAdminMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txtAdminMouseClicked
        // TODO add your handling code here:
        new ContactAdmin(username).setVisible(true);
        
    }//GEN-LAST:event_txtAdminMouseClicked

    private void txtAdminMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txtAdminMouseEntered
        // TODO add your handling code here:
        txtAdmin.setForeground(Color.BLUE);
        
    }//GEN-LAST:event_txtAdminMouseEntered

    private void txtAdminMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txtAdminMouseExited
        // TODO add your handling code here:
        txtAdmin.setForeground(Color.BLACK);
        
    }//GEN-LAST:event_txtAdminMouseExited

    private void btnWatchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnWatchActionPerformed
        int selectedRow = tblAnime.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an anime to watch!");
            return;
        }

        // Get the anime ID from the table (watchlist or library table)
        int animeId = (int) tblAnime.getValueAt(selectedRow, 0);

        try (Connection conn = DBConnection.connect()) {
            // ✅ Get the first episode path from anime_episodes
            String sql = "SELECT local_file_path FROM anime_episodes WHERE anime_id = ? ORDER BY episode_number ASC LIMIT 1";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, animeId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String path = rs.getString("local_file_path");
                if (path == null || path.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "⚠️ No video file found for this anime!");
                } else {
                    // ✅ Open WatchAnimeFrame using animeId
                    WatchAnimeFrame frame = new WatchAnimeFrame(animeId, userId);
                    frame.setVisible(true);
                }
            } else {
                JOptionPane.showMessageDialog(this, "⚠️ No episodes found for this anime!");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }



    }//GEN-LAST:event_btnWatchActionPerformed

    private void lblUsernameMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblUsernameMouseClicked
        // TODO add your handling code here:
        UsernameChange changeFrame = new UsernameChange(this);
        changeFrame.setVisible(true);
    }//GEN-LAST:event_lblUsernameMouseClicked

    private void lblUsernameMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblUsernameMouseEntered
        // TODO add your handling code here:
        lblUsername.setForeground(Color.LIGHT_GRAY);
    }//GEN-LAST:event_lblUsernameMouseEntered

    private void lblUsernameMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblUsernameMouseExited
        // TODO add your handling code here:
        lblUsername.setForeground(Color.BLACK);
    }//GEN-LAST:event_lblUsernameMouseExited

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        new AnimeRequestForm().setVisible(true);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
        int currentUserId = userSession.currentUserId;
        new UserNotificationsFrame(currentUserId).setVisible(true);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSearchActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtSearchActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
        new LoginForm().setVisible(true);
        this.dispose();
    }//GEN-LAST:event_jButton3ActionPerformed

    /**
     * @param args the command line arguments
     */
  
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    public javax.swing.JButton btnDelete;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnUpload;
    private javax.swing.JButton btnWatch;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSlider jSlider1;
    private javax.swing.JLabel lblLogo;
    private javax.swing.JLabel lblProfilePic;
    public javax.swing.JLabel lblUsername;
    private javax.swing.JComboBox<String> sortCmb;
    public javax.swing.JTable tblAnime;
    private javax.swing.JLabel txtAdmin;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}
