package de.febrildur.sieveeditor.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import de.febrildur.sieveeditor.Application;

/**
 * Action to save the current script to a local file.
 *
 * This allows users to:
 * - Back up scripts locally
 * - Export scripts for version control
 * - Save work without a server connection
 */
public class ActionSaveLocalScript extends AbstractAction {

    private final Application parentFrame;
    private File lastDirectory;

    public ActionSaveLocalScript(Application parentFrame) {
        putValue("Name", "Save Local Script...");
        this.parentFrame = parentFrame;
        this.lastDirectory = new File(System.getProperty("user.home"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Sieve Scripts (*.sieve)", "sieve");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(lastDirectory);

        if (chooser.showSaveDialog(parentFrame) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            lastDirectory = file.getParentFile();

            // Add .sieve extension if not present
            if (!file.getName().toLowerCase().endsWith(".sieve")) {
                file = new File(file.getAbsolutePath() + ".sieve");
            }

            try {
                Files.writeString(file.toPath(), parentFrame.getScriptText());
                JOptionPane.showMessageDialog(parentFrame,
                        "File saved successfully to:\n" + file.getName(),
                        "Save Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parentFrame,
                        "Failed to save file: " + ex.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Gets the last accessed directory for persistence.
     */
    public File getLastDirectory() {
        return lastDirectory;
    }

    /**
     * Sets the last accessed directory.
     */
    public void setLastDirectory(File directory) {
        this.lastDirectory = directory;
    }
}
