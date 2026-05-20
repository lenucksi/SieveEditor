package de.febrildur.sieveeditor.actions;

public interface ScriptManagementView {
    int getSelectedRow();
    int getRowCount();
    String getScriptNameAt(int row);
    boolean isActiveAt(int row);
    void refreshTable(String[][] data);
    void showError(String message);
    void showInfo(String message);
    boolean showConfirm(String message, String title);
    void close();
}
