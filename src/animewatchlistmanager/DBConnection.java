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
import javax.swing.JOptionPane;
public class DBConnection {
    public static Connection connect(){
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost/anime_watchlist_db", "root", "");
            return con;
        }
        catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database Connection Failed");
            return null;
        }
    }
}
