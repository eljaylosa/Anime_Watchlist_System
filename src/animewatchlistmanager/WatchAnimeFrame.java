package animewatchlistmanager;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

public class WatchAnimeFrame extends JFrame {

    private JLabel lblPoster, lblTitle, lblGenre, lblSeasons, lblEpisodes;
    private JTextArea txtDescription;
    private JTable tblEpisodes;
    private int animeId;
    private int userId;

    private static Stage currentStage;
    private static MediaPlayer currentPlayer;
    private static MediaView mediaView;

    public WatchAnimeFrame(int animeId, int userId) {
        this.animeId = animeId;
        this.userId = userId;

        new JFXPanel();
        Platform.setImplicitExit(false);

        initComponents();
        loadAnimeDetails(animeId);
        loadEpisodes(animeId);
    }

    private void initComponents() {
        setTitle("Watch Anime");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        lblPoster = new JLabel("No Poster", SwingConstants.CENTER);
        lblPoster.setPreferredSize(new Dimension(250, 350));
        lblPoster.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        add(lblPoster, BorderLayout.WEST);

        JPanel infoPanel = new JPanel(new BorderLayout(5, 5));
        JPanel topInfo = new JPanel(new GridLayout(4, 1));
        lblTitle = new JLabel("Title: ");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblGenre = new JLabel("Genre: ");
        lblSeasons = new JLabel("Seasons: ");
        lblEpisodes = new JLabel("Episodes: ");
        topInfo.add(lblTitle);
        topInfo.add(lblGenre);
        topInfo.add(lblSeasons);
        topInfo.add(lblEpisodes);
        infoPanel.add(topInfo, BorderLayout.NORTH);

        txtDescription = new JTextArea();
        txtDescription.setEditable(false);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtDescription.setBorder(BorderFactory.createTitledBorder("Description"));
        infoPanel.add(new JScrollPane(txtDescription), BorderLayout.CENTER);
        add(infoPanel, BorderLayout.CENTER);

        // --- Episodes Table (No File Path Displayed)
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Episode ID", "Episode #", "Title"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblEpisodes = new JTable(model);
        tblEpisodes.removeColumn(tblEpisodes.getColumnModel().getColumn(0)); // hide Episode ID
        tblEpisodes.setRowHeight(25);

        JScrollPane scrollEpisodes = new JScrollPane(tblEpisodes);
        scrollEpisodes.setBorder(BorderFactory.createTitledBorder("Episodes"));
        add(scrollEpisodes, BorderLayout.SOUTH);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnBack = new JButton("← Back");
        JButton btnPlayAll = new JButton("▶ Play All");
        topPanel.add(btnBack);
        topPanel.add(btnPlayAll);
        add(topPanel, BorderLayout.NORTH);

        btnBack.addActionListener(e -> dispose());
        btnPlayAll.addActionListener(e -> playAllEpisodes());

        tblEpisodes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedRow = tblEpisodes.convertRowIndexToModel(tblEpisodes.getSelectedRow());
                    if (selectedRow != -1) {
                        DefaultTableModel m = (DefaultTableModel) tblEpisodes.getModel();
                        int episodeId = (int) m.getValueAt(selectedRow, 0);
                        int epNumber = (int) m.getValueAt(selectedRow, 1);
                        String epTitle = m.getValueAt(selectedRow, 2).toString();
                        String filePath = getEpisodeFilePath(episodeId);
                        playEpisode(episodeId, filePath, epNumber, epTitle);
                    }
                }
            }
        });
    }

    // Fetch file path internally
    private String getEpisodeFilePath(int episodeId) {
        try (Connection conn = DBConnection.connect()) {
            PreparedStatement ps = conn.prepareStatement("SELECT local_file_path FROM anime_episodes WHERE id = ?");
            ps.setInt(1, episodeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("local_file_path");
        } catch (Exception ex) {
            System.err.println("⚠️ Error fetching file path: " + ex.getMessage());
        }
        return null;
    }

    private void recordEpisodeWatch(int userId, int episodeId) {
    String checkSql = "SELECT COUNT(*) FROM anime_episodes_watched WHERE user_id = ? AND episode_id = ?";
    String insertSql = "INSERT INTO anime_episodes_watched (episode_id, user_id) VALUES (?, ?)";

    try (Connection conn = DBConnection.connect()) {
        // Check if this user already watched this episode
        PreparedStatement checkPs = conn.prepareStatement(checkSql);
        checkPs.setInt(1, userId);
        checkPs.setInt(2, episodeId);
        ResultSet rs = checkPs.executeQuery();

        if (rs.next() && rs.getInt(1) == 0) {
            // Insert only if not yet watched
            PreparedStatement insertPs = conn.prepareStatement(insertSql);
            insertPs.setInt(1, episodeId);
            insertPs.setInt(2, userId);
            insertPs.executeUpdate();
            insertPs.close();
            //System.out.println("✅ Watch recorded for user " + userId + ", episode " + episodeId);
        } else {
            //System.out.println("⚠️ User " + userId + " already watched episode " + episodeId);
        }

        checkPs.close();
    } catch (SQLException ex) {
        System.err.println("⚠️ Failed to record episode watch: " + ex.getMessage());
    }
}

    private void loadAnimeDetails(int animeId) {
        try (Connection conn = DBConnection.connect()) {
            String sql = "SELECT * FROM anime WHERE anime_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, animeId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                lblTitle.setText("Title: " + rs.getString("title"));
                lblGenre.setText("Genre: " + rs.getString("genre"));
                lblSeasons.setText("Seasons: " + rs.getInt("seasons"));
                lblEpisodes.setText("Episodes: " + rs.getInt("episodes"));
                txtDescription.setText(rs.getString("description"));

                String posterPath = rs.getString("image_url");
                if (posterPath != null && !posterPath.isEmpty()) {
                    ImageIcon icon = new ImageIcon(new ImageIcon(posterPath).getImage()
                            .getScaledInstance(250, 350, Image.SCALE_SMOOTH));
                    lblPoster.setIcon(icon);
                    lblPoster.setText("");
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading anime: " + ex.getMessage());
        }
    }

    private void loadEpisodes(int animeId) {
        DefaultTableModel model = (DefaultTableModel) tblEpisodes.getModel();
        model.setRowCount(0);
        try (Connection conn = DBConnection.connect()) {
            String sql = "SELECT id, episode_number, title FROM anime_episodes WHERE anime_id = ? ORDER BY episode_number ASC";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, animeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getInt("episode_number"),
                        rs.getString("title")
                });
            }
            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "⚠️ No episodes uploaded for this anime yet!");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading episodes: " + ex.getMessage());
        }
    }

    private void playEpisode(int episodeId, String localFilePath, int episodeNumber, String episodeTitle) {
        if (localFilePath == null || localFilePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No video file found!");
            return;
        }

        recordEpisodeWatch(userId, episodeId);

        Platform.runLater(() -> {
            try {
                if (currentPlayer != null) {
                    currentPlayer.stop();
                    currentPlayer.dispose();
                }

                Media media = new Media(new File(localFilePath).toURI().toString());
                currentPlayer = new MediaPlayer(media);

                if (mediaView == null) {
                    mediaView = new MediaView(currentPlayer);
                    mediaView.setPreserveRatio(true);
                } else {
                    mediaView.setMediaPlayer(currentPlayer);
                }

                StackPane stack = new StackPane(mediaView);
                mediaView.fitWidthProperty().bind(stack.widthProperty().multiply(0.9));
                mediaView.fitHeightProperty().bind(stack.heightProperty().multiply(0.9));

                Slider slider = new Slider(0, 100, 0);
                Label timeLabel = new Label("00:00 / 00:00");
                timeLabel.setStyle("-fx-text-fill: white;");

                currentPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                    if (currentPlayer.getTotalDuration() != null) {
                        double progress = newTime.toMillis() / currentPlayer.getTotalDuration().toMillis() * 100;
                        slider.setValue(progress);
                        timeLabel.setText(formatTime(newTime, currentPlayer.getTotalDuration()));
                    }
                });

                slider.setOnMouseReleased(e -> {
                    if (currentPlayer.getTotalDuration() != null) {
                        double seekTime = slider.getValue() / 100.0 * currentPlayer.getTotalDuration().toMillis();
                        currentPlayer.seek(javafx.util.Duration.millis(seekTime));
                    }
                });

                Button btnPlay = new Button("▶ Play");
                Button btnPause = new Button("⏸ Pause");
                Button btnStop = new Button("⏹ Stop");
                btnPlay.setOnAction(e -> currentPlayer.play());
                btnPause.setOnAction(e -> currentPlayer.pause());
                btnStop.setOnAction(e -> currentPlayer.stop());

                HBox controls = new HBox(20, btnPlay, btnPause, btnStop);
                controls.setStyle("-fx-padding: 15; -fx-alignment: center;");
                VBox root = new VBox(stack, slider, timeLabel, controls);
                VBox.setVgrow(stack, Priority.ALWAYS);
                root.setStyle("-fx-background-color: black;");

                if (currentStage == null) {
                    currentStage = new Stage();
                }
                currentStage.setTitle("Now Playing - Ep " + episodeNumber + ": " + episodeTitle);
                currentStage.setScene(new Scene(root, 900, 600));
                currentStage.show();

                currentStage.setOnCloseRequest(e -> currentPlayer.stop());

                currentPlayer.play();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error playing video: " + ex.getMessage());
            }
        });
    }

    private void playAllEpisodes() {
        if (tblEpisodes.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No episodes to play!");
            return;
        }

        DefaultTableModel m = (DefaultTableModel) tblEpisodes.getModel();
        int episodeId = (int) m.getValueAt(0, 0);
        int epNumber = (int) m.getValueAt(0, 1);
        String epTitle = m.getValueAt(0, 2).toString();
        String filePath = getEpisodeFilePath(episodeId);

        playEpisode(episodeId, filePath, epNumber, epTitle);
    }

    private String formatTime(javafx.util.Duration elapsed, javafx.util.Duration total) {
        int e = (int) elapsed.toSeconds(), t = (int) total.toSeconds();
        return String.format("%02d:%02d / %02d:%02d", e / 60, e % 60, t / 60, t % 60);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WatchAnimeFrame(1, 1).setVisible(true));
    }
}
