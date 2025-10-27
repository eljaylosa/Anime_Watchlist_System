/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package animewatchlistmanager;

/**
 *
 * @author ljlosa
 */
import javax.swing.*;
import java.awt.*;
public class LogoHelper {
    public static JLabel getAppLogo(int size) {
        try {
            // Load your app logo (put the image in: src/images/kitsulink_logo.png)
            ImageIcon icon = new ImageIcon(LogoHelper.class.getResource("/images/kitsulink_logo.png"));

            // Resize it smoothly
            Image scaled = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            icon = new ImageIcon(scaled);

            JLabel logoLabel = new JLabel(icon);
            logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
            logoLabel.setVerticalAlignment(SwingConstants.CENTER);

            return logoLabel;

        } catch (Exception e) {
            System.err.println("Error loading logo: " + e.getMessage());
            return new JLabel("Logo not found");
        }
    }
}
