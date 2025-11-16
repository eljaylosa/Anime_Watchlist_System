package animewatchlistmanager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.sql.Date;

public class AnimeLibraryFrame extends JFrame {
    private JPanel panelLibrary;
    private JTextField txtSearch, txtSelectedGenres;
    private JComboBox<String> cmbFilter, cmbGenre;
    private JButton btnClearGenres, btnBack;
    private int userId;
    private boolean isAdmin;
    private WatchlistDashboard userDashboardInstance;

    private final Set<String> selectedGenres = new LinkedHashSet<>();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public AnimeLibraryFrame(int userId, WatchlistDashboard dashboardRef, boolean isAdmin) {
        this.userId = userId;
        this.userDashboardInstance = dashboardRef;
        this.isAdmin = isAdmin;

        setTitle(isAdmin ? "Manage Anime Library (Admin)" : "Anime Library");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- TOP PANEL ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        btnBack = new JButton("← Back");
        btnBack.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnBack.addActionListener(e -> dispose());

        txtSearch = new JTextField(20);
        txtSearch.setToolTipText("Search anime...");
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                loadLibrary();
            }
        });

        cmbFilter = new JComboBox<>(new String[]{"A-Z", "Latest Added"});
        cmbFilter.addActionListener(e -> loadLibrary());

        cmbGenre = new JComboBox<>();
        DefaultComboBoxModel<String> genreModel = new DefaultComboBoxModel<>();
        loadGenres(genreModel);
        cmbGenre.setModel(genreModel);
        cmbGenre.addActionListener(e -> handleGenreSelection());

        txtSelectedGenres = new JTextField(20);
        txtSelectedGenres.setEditable(false);

        btnClearGenres = new JButton("Clear Selection");
        btnClearGenres.addActionListener(e -> clearGenreSelection());

        topPanel.add(btnBack);
        topPanel.add(new JLabel("Search:"));
        topPanel.add(txtSearch);
        topPanel.add(new JLabel("Sort by:"));
        topPanel.add(cmbFilter);
        topPanel.add(new JLabel("Genre:"));
        topPanel.add(cmbGenre);
        topPanel.add(new JLabel("Selected:"));
        topPanel.add(txtSelectedGenres);
        topPanel.add(btnClearGenres);

        if (isAdmin) {
            JButton btnAddAnime = new JButton("➕ Add Anime");
            JButton btnAddEpisode = new JButton("🎬 Add Episode");

            btnAddAnime.addActionListener(e -> {
                AdminUploadAnime uploadAnime = new AdminUploadAnime();
                uploadAnime.setVisible(true);
                uploadAnime.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e1) {
                        loadLibrary();
                    }
                });
            });

            btnAddEpisode.addActionListener(e -> {
                AdminUploadEpisode uploadEpisode = new AdminUploadEpisode();
                uploadEpisode.setVisible(true);
                uploadEpisode.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e1) {
                        loadLibrary();
                    }
                });
            });

            topPanel.add(btnAddAnime);
            topPanel.add(btnAddEpisode);
        }

        add(topPanel, BorderLayout.NORTH);

        // --- LIBRARY PANEL ---
        panelLibrary = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        JScrollPane scroll = new JScrollPane(panelLibrary);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        loadLibrary();
    }

    private void loadGenres(DefaultComboBoxModel<String> model) {
        String[] genres = {"Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror",
                "Romance", "Sci-Fi", "Slice of Life", "Sports", "Supernatural", "Mystery",
                "Psychological", "Thriller", "Magic", "Mecha", "Music", "School", "Shounen",
                "Shoujo", "Isekai", "Historical", "Martial Arts", "Demons", "Parody", "Game",
                "Seinen", "Josei"};
        for (String g : genres) model.addElement(g);
    }

    private void handleGenreSelection() {
        String selected = (String) cmbGenre.getSelectedItem();
        if (selected == null) return;
        if (!selectedGenres.add(selected)) {
            JOptionPane.showMessageDialog(this, "Genre already selected: " + selected);
            return;
        }
        txtSelectedGenres.setText(String.join(", ", selectedGenres));
        loadLibrary();
    }

    private void clearGenreSelection() {
        selectedGenres.clear();
        txtSelectedGenres.setText("");
        JOptionPane.showMessageDialog(this, "🎯 Genre selection cleared!");
        loadLibrary();
    }

    private void loadLibrary() {
        panelLibrary.removeAll();

        try (Connection conn = DBConnection.connect()) {
            String search = "%" + txtSearch.getText().trim() + "%";
            String filter = (String) cmbFilter.getSelectedItem();

            StringBuilder sql = new StringBuilder("SELECT * FROM anime WHERE title LIKE ?");
            if (!selectedGenres.isEmpty()) {
                sql.append(" AND (");
                int i = 0;
                for (String ignored : selectedGenres) {
                    if (i++ > 0) sql.append(" OR ");
                    sql.append("genre LIKE ?");
                }
                sql.append(")");
            }

            if ("Latest Added".equals(filter)) {
                sql.append(" ORDER BY COALESCE(date_added, '1970-01-01') DESC, anime_id DESC");
            } else {
                sql.append(" ORDER BY title COLLATE utf8mb4_unicode_ci ASC");
            }

            PreparedStatement ps = conn.prepareStatement(sql.toString());
            int index = 1;
            ps.setString(index++, search);
            for (String genre : selectedGenres) {
                ps.setString(index++, "%" + genre + "%");
            }

            ResultSet rs = ps.executeQuery();
            boolean hasData = false;

            while (rs.next()) {
                hasData = true;
                int animeId = rs.getInt("anime_id");
                String title = rs.getString("title");
                String genre = rs.getString("genre");
                int episodes = rs.getInt("episodes");
                String status = rs.getString("status");
                String imagePath = rs.getString("image_url");
                String localPath = rs.getString("local_file_path");

                Date dateAired = rs.getDate("date_aired");

                JPanel box = createAnimeBox(animeId, title, genre, episodes, status, imagePath, localPath, dateAired);
                panelLibrary.add(box);
            }

            if (!hasData) {
                JLabel noData = new JLabel("No anime found.");
                noData.setFont(new Font("SansSerif", Font.ITALIC, 16));
                panelLibrary.add(noData);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading library: " + e.getMessage());
        }

        panelLibrary.revalidate();
        panelLibrary.repaint();
    }

    private JPanel createAnimeBox(int animeId, String title, String genre, int totalEpisodes,
                              String status, String imagePath, String localPath, Date dateAired) {

            JPanel box = new JPanel();
            box.setPreferredSize(new Dimension(220, isAdmin ? 460 : 400));
            box.setLayout(new BorderLayout());
            box.setBackground(Color.WHITE);
            box.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));

            // --- IMAGE ---
            JLabel lblImage;
            try {
                ImageIcon icon = new ImageIcon(imagePath);
                lblImage = new JLabel(new ImageIcon(icon.getImage().getScaledInstance(200, 240, Image.SCALE_SMOOTH)));
            } catch (Exception e) {
                lblImage = new JLabel("No Image", SwingConstants.CENTER);
                lblImage.setPreferredSize(new Dimension(200, 240));
            }
            lblImage.setOpaque(true);
            lblImage.setBackground(Color.LIGHT_GRAY);
            lblImage.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            lblImage.setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(lblImage, BorderLayout.NORTH);

            // --- INFO PANEL ---
            int uploadedEpisodes = getUploadedEpisodeCount(animeId);
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);
            infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            JLabel lblTitle = new JLabel("<html><center>" + title + "</center></html>");
            lblTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
            lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            lblTitle.setHorizontalAlignment(SwingConstants.CENTER);

            JLabel lblGenre = new JLabel("🎭 " + genre, SwingConstants.CENTER);
            lblGenre.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lblGenre.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblEpisodes = new JLabel("🎬 " + uploadedEpisodes + "/" + totalEpisodes + " episodes", SwingConstants.CENTER);
            lblEpisodes.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lblEpisodes.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Separate label for status
            JLabel lblStatus = new JLabel("Status: " + status, SwingConstants.CENTER);
            lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Separate label for date aired
            String dateText = "";
            if (dateAired != null) {
                LocalDate localDate = dateAired.toLocalDate();
                dateText = "Aired: " + localDate.format(dateFormatter);
            }
            JLabel lblDate = new JLabel(dateText, SwingConstants.CENTER);
            lblDate.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lblDate.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Add spacing for neat layout
            infoPanel.add(lblTitle);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            infoPanel.add(lblGenre);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 3)));
            infoPanel.add(lblEpisodes);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 2)));
            infoPanel.add(lblStatus);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 2)));
            infoPanel.add(lblDate);

            box.add(infoPanel, BorderLayout.CENTER);

            // --- BUTTON PANEL ---
            JPanel btnPanel = new JPanel();
            btnPanel.setLayout(new GridLayout(isAdmin ? 3 : 1, 1, 5, 5));
            btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            btnPanel.setOpaque(false);

            if (isAdmin) {
                JButton btnEdit = new JButton("✏️ Edit");
                JButton btnDelete = new JButton("🗑️ Delete");
                JButton btnEditEpisode = new JButton("🎬 Edit Episodes");

                Font btnFont = new Font("SansSerif", Font.PLAIN, 12);
                btnEdit.setFont(btnFont);
                btnDelete.setFont(btnFont);
                btnEditEpisode.setFont(btnFont);

                btnPanel.add(btnEdit);
                btnPanel.add(btnDelete);
                btnPanel.add(btnEditEpisode);

                btnEdit.addActionListener(e -> editAnime(animeId));
                btnDelete.addActionListener(e -> deleteAnime(animeId));
                btnEditEpisode.addActionListener(e -> editAnimeEpisodes(animeId));

            } else {
                JButton btnAdd = new JButton("➕ Add");
                btnAdd.setFont(new Font("SansSerif", Font.PLAIN, 12));
                btnPanel.add(btnAdd);
                btnAdd.addActionListener(e -> addToWatchlist(animeId));
            }

            box.add(btnPanel, BorderLayout.SOUTH);

            // --- HOVER EFFECT ---
            box.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    box.setBackground(new Color(240, 248, 255));
                    box.setBorder(BorderFactory.createLineBorder(new Color(100, 160, 255), 2));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    box.setBackground(Color.WHITE);
                    box.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    showAnimeDetail(animeId, title, genre, totalEpisodes, status, imagePath, localPath, dateAired);
                }
            });

            return box;
        }



    private String fetchAnimeDescription(int animeId) {
        String desc = "No description available.";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT description FROM anime WHERE anime_id = ?")) {
            ps.setInt(1, animeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) desc = rs.getString("description");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return desc;
    }

    private void showAnimeDetail(int animeId, String title, String genre, int totalEpisodes,
                                 String status, String imagePath, String localPath, Date dateAired) {

        JDialog detailDialog = new JDialog(this, title, true);
        detailDialog.setSize(600, 400);
        detailDialog.setLocationRelativeTo(this);
        detailDialog.setLayout(new BorderLayout());

        JLabel lblImage;
        try {
            ImageIcon icon = new ImageIcon(imagePath);
            lblImage = new JLabel(new ImageIcon(icon.getImage().getScaledInstance(200, 300, Image.SCALE_SMOOTH)));
        } catch (Exception e) {
            lblImage = new JLabel("No Image", SwingConstants.CENTER);
            lblImage.setPreferredSize(new Dimension(200, 300));
        }
        JPanel leftPanel = new JPanel();
        leftPanel.add(lblImage);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JLabel lblTitle = new JLabel("<html><h2>" + title + "</h2></html>");
        JLabel lblGenre = new JLabel("Genre: " + genre);
        JLabel lblStatus = new JLabel("Status: " + status);

        String dateText = "";
        if (dateAired != null) {
            LocalDate localDate = dateAired.toLocalDate();
            dateText = " | Aired: " + localDate.format(dateFormatter);
        }

        JLabel lblEpisodes = new JLabel("Episodes: " + getUploadedEpisodeCount(animeId) + "/" + totalEpisodes + dateText);

        JTextArea txtDescription = new JTextArea();
        txtDescription.setText(fetchAnimeDescription(animeId));
        txtDescription.setWrapStyleWord(true);
        txtDescription.setLineWrap(true);
        txtDescription.setEditable(false);
        txtDescription.setBackground(rightPanel.getBackground());

        rightPanel.add(lblTitle);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        rightPanel.add(lblGenre);
        rightPanel.add(lblStatus);
        rightPanel.add(lblEpisodes);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        rightPanel.add(new JScrollPane(txtDescription));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAdd = new JButton(isAdmin ? "✏️ Edit" : "➕ Add to My List");
        btnAdd.addActionListener(e -> {
            if(isAdmin){
                editAnime(animeId);
            } else {
                addToWatchlist(animeId);
            }
            detailDialog.dispose();
        });
        btnPanel.add(btnAdd);

        detailDialog.add(leftPanel, BorderLayout.WEST);
        detailDialog.add(rightPanel, BorderLayout.CENTER);
        detailDialog.add(btnPanel, BorderLayout.SOUTH);

        detailDialog.setVisible(true);
    }

    private int getUploadedEpisodeCount(int animeId) {
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM anime_episodes WHERE anime_id = ?")) {
            ps.setInt(1, animeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void addToWatchlist(int animeId) {
        try (Connection conn = DBConnection.connect()) {
            PreparedStatement check = conn.prepareStatement(
                    "SELECT * FROM watchlist WHERE user_id = ? AND anime_id = ?"
            );
            check.setInt(1, userId);
            check.setInt(2, animeId);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "✅ Anime is already in your list!");
                return;
            }

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO watchlist (user_id, anime_id, title, genre, episodes, status, image_url, local_file_path, date_added) " +
                            "SELECT ?, anime_id, title, genre, episodes, 'Plan to Watch', image_url, local_file_path, NOW() " +
                            "FROM anime WHERE anime_id = ?"
            );
            ps.setInt(1, userId);
            ps.setInt(2, animeId);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Anime added to your list!");

            if (userDashboardInstance != null) {
                userDashboardInstance.loadAnimeList();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding anime: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteAnime(int animeId) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this anime?\n(This will also delete its episodes and remove it from all user watchlists.)",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.connect()) {
                conn.setAutoCommit(false);
                conn.prepareStatement("DELETE FROM anime_episodes WHERE anime_id = " + animeId).executeUpdate();
                conn.prepareStatement("DELETE FROM watchlist WHERE anime_id = " + animeId).executeUpdate();
                conn.prepareStatement("DELETE FROM anime WHERE anime_id = " + animeId).executeUpdate();
                conn.commit();

                JOptionPane.showMessageDialog(this, "🗑️ Anime deleted successfully!");
                loadLibrary();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting anime: " + e.getMessage());
            }
        }
    }

    private void editAnime(int animeId) {
        AdminEditAnime editWindow = new AdminEditAnime(animeId);
        editWindow.setVisible(true);
        editWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                loadLibrary();
            }
        });
    }

    private void editAnimeEpisodes(int animeId) {
        JFrame frame = new JFrame("Edit Episodes");
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(this);
        frame.setLayout(new BorderLayout());

        DefaultListModel<String> episodeListModel = new DefaultListModel<>();
        JList<String> episodeList = new JList<>(episodeListModel);
        Map<String, Integer> episodeMap = new HashMap<>();

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, episode_number, title FROM anime_episodes WHERE anime_id = ? ORDER BY episode_number ASC")) {
            ps.setInt(1, animeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int epId = rs.getInt("id");
                int epNum = rs.getInt("episode_number");
                String epTitle = rs.getString("title");
                String display = "Episode " + epNum + ": " + epTitle;
                episodeListModel.addElement(display);
                episodeMap.put(display, epId);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading episodes: " + e.getMessage());
            return;
        }

        frame.add(new JScrollPane(episodeList), BorderLayout.CENTER);

        JButton btnEdit = new JButton("Edit Selected Episode");
        btnEdit.addActionListener(e -> {
            String selected = episodeList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(frame, "Please select an episode.");
                return;
            }
            int epId = episodeMap.get(selected);
            new AdminEditEpisode(epId).setVisible(true);
            frame.dispose();
        });

        frame.add(btnEdit, BorderLayout.SOUTH);
        frame.setVisible(true);
    }
}
