package de.febrildur.sieveeditor.actions;

import java.io.IOException;
import java.util.List;

import com.fluffypeople.managesieve.ParseException;
import com.fluffypeople.managesieve.SieveScript;

import de.febrildur.sieveeditor.system.ConnectAndListScripts;

public class ScriptManagementPresenter {
    private final ScriptManagementView view;
    private final ConnectAndListScripts server;

    public ScriptManagementPresenter(ScriptManagementView view, ConnectAndListScripts server) {
        this.view = view;
        this.server = server;
    }

    public void handleActivate() {
        int selectedRow = view.getSelectedRow();
        if (selectedRow < 0) {
            view.showError("Please select a script first.");
            return;
        }
        String scriptName = view.getScriptNameAt(selectedRow);
        try {
            server.activateScript(scriptName);
            refreshScriptList();
        } catch (IOException | ParseException ex) {
            view.showError("Error: " + ex.getMessage());
        }
    }

    public void handleDeactivate() {
        try {
            server.deactivateScript();
            refreshScriptList();
        } catch (IOException | ParseException ex) {
            view.showError("Error: " + ex.getMessage());
        }
    }

    public void handleRename(String newName) {
        int selectedRow = view.getSelectedRow();
        if (selectedRow < 0) {
            view.showError("Please select a script first.");
            return;
        }
        String oldName = view.getScriptNameAt(selectedRow);
        if (newName == null || newName.trim().isEmpty()) {
            view.showError("Script name cannot be empty.");
            return;
        }
        if (newName.equals(oldName)) {
            return;
        }
        try {
            server.rename(oldName, newName);
            refreshScriptList();
        } catch (IOException | ParseException ex) {
            view.showError("Error: " + ex.getMessage());
        }
    }

    public void handleDelete() {
        int selectedRow = view.getSelectedRow();
        if (selectedRow < 0) {
            view.showError("Please select a script first.");
            return;
        }
        String scriptName = view.getScriptNameAt(selectedRow);
        boolean confirmed = view.showConfirm(
            "Delete script \"" + scriptName + "\"?\n\nThis cannot be undone.",
            "Confirm Delete");
        if (!confirmed) {
            return;
        }
        try {
            server.deleteScript(scriptName);
            refreshScriptList();
        } catch (IOException | ParseException ex) {
            view.showError("Error: " + ex.getMessage());
        }
    }

    public void refreshScriptList() {
        try {
            List<SieveScript> scripts = server.getListScripts();
            String[][] data = new String[scripts.size()][2];
            for (int i = 0; i < scripts.size(); i++) {
                SieveScript script = scripts.get(i);
                data[i][0] = script.getName();
                data[i][1] = script.isActive() ? "active" : "";
            }
            view.refreshTable(data);
        } catch (IOException | ParseException ex) {
            view.showError("Error: " + ex.getMessage());
        }
    }
}
