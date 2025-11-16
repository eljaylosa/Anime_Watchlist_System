package animewatchlistmanager;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.Random;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

public class ReportsFrame extends JFrame {

    public ReportsFrame() {
        setTitle("Anime Reports Dashboard");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- TOP PANEL BUTTONS ---
        JPanel buttonPanel = new JPanel(new GridLayout(2, 6, 5, 5)); // added new button slot

        JButton btnTotalAnime = new JButton("Total Anime");
        JButton btnTotalEpisodes = new JButton("Total Episodes");
        JButton btnMostWatched = new JButton("Most Watched Anime");
        JButton btnTopUsers = new JButton("Top Users");
        JButton btnGenreStats = new JButton("Genre Statistics");
        JButton btnEpisodesWatched = new JButton("Episodes Watched"); // new

        JButton btnByDay = new JButton("By Day");
        JButton btnByWeek = new JButton("By Week");
        JButton btnByMonth = new JButton("By Month");
        JButton btnByYear = new JButton("By Year");
        JButton btnClearFilter = new JButton("Clear Time Filter");

        buttonPanel.add(btnTotalAnime);
        buttonPanel.add(btnTotalEpisodes);
        buttonPanel.add(btnMostWatched);
        buttonPanel.add(btnTopUsers);
        buttonPanel.add(btnGenreStats);
        buttonPanel.add(btnEpisodesWatched);

        buttonPanel.add(btnByDay);
        buttonPanel.add(btnByWeek);
        buttonPanel.add(btnByMonth);
        buttonPanel.add(btnByYear);
        buttonPanel.add(btnClearFilter);

        add(buttonPanel, BorderLayout.NORTH);

        JPanel chartPanel = new JPanel(new BorderLayout());
        add(chartPanel, BorderLayout.CENTER);

        // --- ORIGINAL CHART ACTIONS ---
        btnTotalAnime.addActionListener(e -> showTotalAnime(chartPanel));
        btnTotalEpisodes.addActionListener(e -> showTotalEpisodes(chartPanel));
        btnMostWatched.addActionListener(e -> showMostWatched(chartPanel));
        btnTopUsers.addActionListener(e -> showTopUsers(chartPanel));
        btnGenreStats.addActionListener(e -> showGenreStats(chartPanel));
        btnEpisodesWatched.addActionListener(e -> showEpisodesWatched(chartPanel));

        // --- TIME-BASED CHART ACTIONS ---
        btnByDay.addActionListener(e -> showAddedByTime(chartPanel, "DAY"));
        btnByWeek.addActionListener(e -> showAddedByTime(chartPanel, "WEEK"));
        btnByMonth.addActionListener(e -> showAddedByTime(chartPanel, "MONTH"));
        btnByYear.addActionListener(e -> showAddedByTime(chartPanel, "YEAR"));
        btnClearFilter.addActionListener(e -> btnGenreStats.doClick()); // default chart

        btnGenreStats.doClick(); // show default
        setVisible(true);
    }

    // -------------------- CHART METHODS --------------------

        private void showAddedByTime(JPanel chartPanel, String interval) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // ✅ Use anime.date_added instead of watchlist.date_added
        String sql = switch (interval) {
            case "DAY" -> """
                SELECT DATE(date_added) AS label, COUNT(*) AS total
                FROM anime
                GROUP BY DATE(date_added)
                ORDER BY DATE(date_added)
            """;
            case "WEEK" -> """
                SELECT YEAR(date_added) AS yr, WEEK(date_added) AS wk, COUNT(*) AS total
                FROM anime
                GROUP BY YEAR(date_added), WEEK(date_added)
                ORDER BY YEAR(date_added), WEEK(date_added)
            """;
            case "MONTH" -> """
                SELECT YEAR(date_added) AS yr, MONTH(date_added) AS mon, COUNT(*) AS total
                FROM anime
                GROUP BY YEAR(date_added), MONTH(date_added)
                ORDER BY YEAR(date_added), MONTH(date_added)
            """;
            case "YEAR" -> """
                SELECT YEAR(date_added) AS yr, COUNT(*) AS total
                FROM anime
                GROUP BY YEAR(date_added)
                ORDER BY YEAR(date_added)
            """;
            default -> "";
        };

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String label = switch (interval) {
                    case "DAY" -> rs.getString("label");
                    case "WEEK" -> "Week " + rs.getInt("wk") + " (" + rs.getInt("yr") + ")";
                    case "MONTH" -> String.format("%02d/%d", rs.getInt("mon"), rs.getInt("yr"));
                    case "YEAR" -> String.valueOf(rs.getInt("yr"));
                    default -> "";
                };
                int total = rs.getInt("total");
                dataset.addValue(total, "Added Anime", label);
            }

            // ✅ Dynamic chart title
            String chartTitle = switch (interval) {
                case "DAY" -> "Anime Added per Day";
                case "WEEK" -> "Anime Added per Week";
                case "MONTH" -> "Anime Added per Month";
                case "YEAR" -> "Anime Added per Year";
                default -> "Anime Added Over Time";
            };

            JFreeChart chart = ChartFactory.createBarChart(
                    chartTitle,
                    interval,
                    "Number of Anime Added",
                    dataset
            );

            customizeBarChart(chart);
            updateChart(chartPanel, chart);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "⚠️ Failed to load " + interval + " data: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }



    private void showTotalAnime(JPanel chartPanel) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS total FROM anime");
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int total = rs.getInt("total");
                dataset.addValue(total, "Anime", "Total Anime");
            }

            JFreeChart chart = ChartFactory.createBarChart(
                    "Total Anime",
                    "Category",
                    "Count",
                    dataset
            );
            customizeBarChart(chart);
            updateChart(chartPanel, chart);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void showTotalEpisodes(JPanel chartPanel) {
     DefaultCategoryDataset dataset = new DefaultCategoryDataset();

     String sql = "SELECT title, episodes FROM anime ORDER BY episodes DESC LIMIT 10";

     try (Connection conn = DBConnection.connect();
          PreparedStatement ps = conn.prepareStatement(sql);
          ResultSet rs = ps.executeQuery()) {

         while (rs.next()) {
             String title = rs.getString("title");
             int episodes = rs.getInt("episodes");
             dataset.addValue(episodes, "Episodes", title);
         }

         // Create the bar chart
         JFreeChart chart = ChartFactory.createBarChart(
                 "Top 10 Anime by Total Episodes",  // Chart title
                 "Anime",                          // X-axis label
                 "Total Episodes",                 // Y-axis label
                 dataset
         );

         // Customize chart
         customizeBarChart(chart);

         // Update the chart display
         updateChart(chartPanel, chart);

     } catch (SQLException ex) {
         ex.printStackTrace();
         JOptionPane.showMessageDialog(null, "⚠️ Failed to load Top 10 Episodes Chart: " + ex.getMessage());
     }
 }


    private void showMostWatched(JPanel chartPanel) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String sql = "SELECT a.title, COUNT(w.watchlist_id) AS watch_count " +
                "FROM watchlist w JOIN anime a ON w.anime_id = a.anime_id " +
                "GROUP BY w.anime_id ORDER BY watch_count DESC LIMIT 10";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String title = rs.getString("title");
                int count = rs.getInt("watch_count");
                dataset.addValue(count, "Watch Count", title);
            }

            JFreeChart chart = ChartFactory.createBarChart(
                    "Most Watched Anime (Top 10)",
                    "Anime",
                    "Watch Count",
                    dataset
            );
            customizeBarChart(chart);
            updateChart(chartPanel, chart);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void showTopUsers(JPanel chartPanel) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String sql = "SELECT u.username, COUNT(w.watchlist_id) AS total_added " +
                "FROM users u JOIN watchlist w ON u.user_id = w.user_id " +
                "GROUP BY u.user_id ORDER BY total_added DESC LIMIT 10";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String user = rs.getString("username");
                int total = rs.getInt("total_added");
                dataset.addValue(total, "Added Anime", user);
            }

            JFreeChart chart = ChartFactory.createBarChart(
                    "Users with Most Added Anime (Top 10)",
                    "User",
                    "Anime Added",
                    dataset
            );
            customizeBarChart(chart);
            updateChart(chartPanel, chart);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void showGenreStats(JPanel chartPanel) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        String sql = "SELECT genre, COUNT(*) AS total FROM anime GROUP BY genre";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String genre = rs.getString("genre");
                int total = rs.getInt("total");
                dataset.setValue(genre, total);
            }

            JFreeChart chart = ChartFactory.createPieChart(
                    "Anime Distribution by Genre",
                    dataset,
                    true,
                    true,
                    false
            );
            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setLabelBackgroundPaint(Color.LIGHT_GRAY);

            updateChart(chartPanel, chart);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // --- NEW METHOD: Episodes Watched ---
        private void showEpisodesWatched(JPanel chartPanel) {
        chartPanel.removeAll();
        chartPanel.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblSelectAnime = new JLabel("Select Anime:");
        JComboBox<String> cmbAnime = new JComboBox<>();

        topPanel.add(lblSelectAnime);
        topPanel.add(cmbAnime);
        chartPanel.add(topPanel, BorderLayout.NORTH);

        JPanel innerChartPanel = new JPanel(new BorderLayout());
        chartPanel.add(innerChartPanel, BorderLayout.CENTER);

        // Load anime titles into combo box
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT title FROM anime ORDER BY title");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cmbAnime.addItem(rs.getString("title"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        // Listener for combo box selection
        cmbAnime.addActionListener(e -> {
            String selectedAnime = (String) cmbAnime.getSelectedItem();
            if (selectedAnime != null) {
                showEpisodeWatchChart(innerChartPanel, selectedAnime);
            }
        });

        // Show default chart for first anime
        if (cmbAnime.getItemCount() > 0) {
            cmbAnime.setSelectedIndex(0);
            showEpisodeWatchChart(innerChartPanel, cmbAnime.getItemAt(0));
        }

        chartPanel.revalidate();
        chartPanel.repaint();
    }



    // -------------------- HELPER METHODS --------------------

        private void showEpisodeWatchChart(JPanel chartPanel, String animeTitle) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        String sql = """
            SELECT 
                CONCAT('Ep ', e.episode_number, 
                       IF(e.title IS NOT NULL AND e.title != '', 
                          CONCAT(': ', e.title), '')) AS episode_label,
                COUNT(w.id) AS watch_count
            FROM anime a
            JOIN anime_episodes e ON a.anime_id = e.anime_id
            LEFT JOIN anime_episodes_watched w ON e.id = w.episode_id
            WHERE a.title = ?
            GROUP BY e.id, e.episode_number, e.title
            ORDER BY e.episode_number
            """;

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, animeTitle);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String episodeLabel = rs.getString("episode_label");
                    int count = rs.getInt("watch_count");
                    dataset.addValue(count, "Watch Count", episodeLabel);
                }
            }

            JFreeChart chart = ChartFactory.createBarChart(
                    "Episode Watch Count - " + animeTitle,
                    "Episodes",
                    "Watch Count",
                    dataset
            );

            customizeBarChart(chart);
            updateChart(chartPanel, chart);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }


        
        private void customizeBarChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(230, 230, 250));
        plot.setDomainGridlinePaint(Color.GRAY);
        plot.setRangeGridlinePaint(Color.GRAY);

        // Rotate X-axis labels
        plot.getDomainAxis().setCategoryLabelPositions(
                org.jfree.chart.axis.CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0)
        );

        // Random color per bar
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        Random rand = new Random();
        for (int i = 0; i < plot.getDataset().getColumnCount(); i++) {
            renderer.setSeriesPaint(i, new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
        }
    }

    private void updateChart(JPanel chartPanel, JFreeChart chart) {
        chartPanel.removeAll();
        ChartPanel cp = new ChartPanel(chart);
        chartPanel.add(cp, BorderLayout.CENTER);
        chartPanel.revalidate();
        chartPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ReportsFrame::new);
    }
}
