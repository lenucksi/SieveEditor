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
 * Action to open a local Sieve script file for editing.
 *
 * This allows users to:
 * - Develop scripts offline without a server connection
 * - Back up scripts locally
 * - Use version control for their scripts
 */
public class ActionOpenLocalScript extends AbstractAction {

    private final Application parentFrame;
    private File lastDirectory;

    public ActionOpenLocalScript(Application parentFrame) {
        putValue("Name", "Open Local Script...");
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

        if (chooser.showOpenDialog(parentFrame) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            lastDirectory = selectedFile.getParentFile();

            try {
                String content = Files.readString(selectedFile.toPath());
                parentFrame.loadLocalScript(content, selectedFile.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parentFrame,
                        "Failed to load file: " + ex.getMessage(),
                        "Load Error", JOptionPane.ERROR_MESSAGE);
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
