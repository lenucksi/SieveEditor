package de.febrildur.sieveeditor.actions;

import de.febrildur.sieveeditor.Application;
import de.febrildur.sieveeditor.templates.SieveTemplate;
import de.febrildur.sieveeditor.templates.TemplateService;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Builds the Insert menu with template options.
 *
 * The menu includes:
 * - Built-in templates for common Sieve patterns
 * - User-defined templates from the templates directory
 */
public class InsertMenuBuilder {

    private final Application application;
    private final TemplateService templateService;

    public InsertMenuBuilder(Application application) {
        this.application = application;
        this.templateService = new TemplateService();
    }

    /**
     * Creates the Insert menu with all templates.
     */
    public JMenu createInsertMenu() {
        JMenu insertMenu = new JMenu("Insert");

        // Built-in templates
        JMenu builtinMenu = new JMenu("Built-in Templates");
        for (SieveTemplate template : templateService.getBuiltinTemplates()) {
            builtinMenu.add(createTemplateMenuItem(template));
        }
        insertMenu.add(builtinMenu);

        // User templates
        List<SieveTemplate> userTemplates = templateService.getUserTemplates();
        if (!userTemplates.isEmpty()) {
            insertMenu.addSeparator();
            JMenu userMenu = new JMenu("User Templates");
            for (SieveTemplate template : userTemplates) {
                userMenu.add(createTemplateMenuItem(template));
            }
            insertMenu.add(userMenu);
        }

        // Help item to show templates directory
        insertMenu.addSeparator();
        insertMenu.add(createOpenTemplatesDirMenuItem());

        return insertMenu;
    }

    /**
     * Creates a menu item for a template.
     */
    private JMenuItem createTemplateMenuItem(SieveTemplate template) {
        AbstractAction action = new AbstractAction(template.getName()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertTemplate(template);
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, template.getDescription());

        return new JMenuItem(action);
    }

    /**
     * Creates menu item to open templates directory.
     */
    private JMenuItem createOpenTemplatesDirMenuItem() {
        return new JMenuItem(new AbstractAction("Open Templates Folder...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                templateService.ensureTemplatesDirectoryExists();
                try {
                    java.awt.Desktop.getDesktop().open(
                            templateService.getTemplatesDirectory().toFile());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(application,
                            "Templates folder:\n" + templateService.getTemplatesDirectory() +
                                    "\n\nPlace .sieve files here for custom templates.",
                            "Templates Location",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
    }

    /**
     * Inserts template content at the current cursor position.
     */
    private void insertTemplate(SieveTemplate template) {
        var textArea = application.getScriptArea();
        int caretPos = textArea.getCaretPosition();
        textArea.insert(template.getContent(), caretPos);
        textArea.setCaretPosition(caretPos + template.getContent().length());
        textArea.requestFocusInWindow();
    }

    /**
     * Gets the template service for testing.
     */
    public TemplateService getTemplateService() {
        return templateService;
    }
}
