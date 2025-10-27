/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package animewatchlistmanager;

/**
 *
 * @author ljlosa
 */
import java.sql.*;
public class ActivityLogger {
    
     public static void log(int userId, String username, String action) {
        try (Connection con = DBConnection.connect()) {
            String sql = "INSERT INTO activity_logs (user_id, username, action) VALUES (?, ?, ?)";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, userId);
            pst.setString(2, username);
            pst.setString(3, action);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error logging activity: " + e.getMessage());
        }
    }
    
}
