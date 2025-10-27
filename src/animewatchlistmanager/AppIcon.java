/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package animewatchlistmanager;

import java.awt.*;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

/**
 *
 * @author ljlosa
 */
public class AppIcon { 
        public static void setAppIcon(JFrame frame) {
        try {
            Image icon = Toolkit.getDefaultToolkit().getImage(
                AppIcon.class.getResource("/images/kitsulink_logo.png")
            );
            frame.setIconImage(icon);
        } catch (Exception e) {
            System.out.println("Failed to load app icon: " + e.getMessage());
        }
    }
}
