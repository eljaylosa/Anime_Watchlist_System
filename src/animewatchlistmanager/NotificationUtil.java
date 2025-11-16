/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package animewatchlistmanager;

/**
 *
 * @author ljlosa
 */
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.sql.*;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.Timer;
public class NotificationUtil {
    public static void sendNotification(int userId, String message) {
        try (Connection conn = DBConnection.connect()) {
            String sql = "INSERT INTO notifications (user_id, message, created_at) VALUES (?, ?, NOW())";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving notification: " + e.getMessage());
        }
    }
    
    public static void showToast(String message) {
    // 🪄 Create a simple toast popup
    JWindow window = new JWindow();
    JLabel label = new JLabel(message);
    label.setOpaque(true);
    label.setBackground(new Color(0, 0, 0, 180));
    label.setForeground(Color.WHITE);
    label.setFont(new Font("Segoe UI", Font.BOLD, 14));
    label.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

    window.add(label);
    window.pack();

    // 📍 Position bottom-right
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width - window.getWidth() - 20;
    int y = screenSize.height - window.getHeight() - 60;
    window.setLocation(x, y);

    // 👁️ Show for 3 seconds
    window.setVisible(true);
    new Timer(3000, e -> window.dispose()).start();
}

}
