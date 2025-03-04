package asia.lira.opaiplus.internal;

import com.allatori.annotations.StringEncryptionType;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@StringEncryptionType("strong")
public class Installer {
    private static final String INSTALL_PATH = System.getenv("APPDATA") + "\\Opai\\extensions";

    public static void start() {
        JFrame frame = new JFrame("OpaiPlus Installer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout());

        JLabel label = new JLabel("<html>Click 'Install' button to install OpaiPlus.<br><b>" + INSTALL_PATH + "</b></html>", SwingConstants.CENTER);
        frame.add(label, BorderLayout.CENTER);

        JButton installButton = new JButton("Install");
        installButton.setFont(new Font("Arial", Font.BOLD, 14));
        frame.add(installButton, BorderLayout.SOUTH);

        installButton.addActionListener(e -> installJar(frame));

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void installJar(JFrame frame) {
        try {
            String jarPath = new File(Installer.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
            File sourceFile = new File(jarPath);
            File targetDir = new File(INSTALL_PATH);
            File targetFile = new File(targetDir, sourceFile.getName());

            if (!targetDir.exists()) {
                throw new IOException("Failed to find target directory, check your opai install.");
            }

            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            JOptionPane.showMessageDialog(frame, "Installed successful!",
                    "OpaiPlus Installer", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Failed to install: " + ex.getMessage(),
                    "OpaiPlus Installer", JOptionPane.ERROR_MESSAGE);
        }
    }
}
