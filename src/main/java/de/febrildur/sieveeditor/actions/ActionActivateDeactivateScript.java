package de.febrildur.sieveeditor.actions;
// SPDX-FileCopyrightText: 2020 Zwixx
// SPDX-FileCopyrightText: 2025 Claude
// SPDX-FileCopyrightText: 2025, 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import com.fluffypeople.managesieve.ParseException;
import com.fluffypeople.managesieve.SieveScript;

import de.febrildur.sieveeditor.Application;

public class ActionActivateDeactivateScript extends AbstractAction {

    private Application parentFrame;

    public ActionActivateDeactivateScript(Application parentFrame) {
        putValue(NAME, "Manage Scripts...");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
            KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK));
        this.parentFrame = parentFrame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JDialog dialog = new JDialog(parentFrame, "Manage Scripts", true);
        dialog.setLayout(new BorderLayout(5, 5));

        String[] columnNames = {"Name", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(250);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(table);
        dialog.add(scrollPane, BorderLayout.CENTER);

        ScriptManagementView view = new ScriptManagementView() {
            @Override
            public int getSelectedRow() {
                return table.getSelectedRow();
            }

            @Override
            public int getRowCount() {
                return tableModel.getRowCount();
            }

            @Override
            public String getScriptNameAt(int row) {
                return (String) tableModel.getValueAt(row, 0);
            }

            @Override
            public boolean isActiveAt(int row) {
                return "active".equals(tableModel.getValueAt(row, 1));
            }

            @Override
            public void refreshTable(String[][] data) {
                tableModel.setRowCount(0);
                for (String[] row : data) {
                    tableModel.addRow(row);
                }
            }

            @Override
            public void showError(String message) {
                JOptionPane.showMessageDialog(dialog, message);
            }

            @Override
            public void showInfo(String message) {
                JOptionPane.showMessageDialog(dialog, message);
            }

            @Override
            public boolean showConfirm(String message, String title) {
                return JOptionPane.showConfirmDialog(dialog, message, title,
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
            }

            @Override
            public void close() {
                dialog.dispose();
            }
        };

        ScriptManagementPresenter presenter = new ScriptManagementPresenter(view, parentFrame.getServer());
        presenter.refreshScriptList();

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton loadBtn = new JButton("Load");
        loadBtn.setToolTipText("Load selected script into editor");
        loadBtn.addActionListener(event -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dialog, "Please select a script first.");
                return;
            }
            String scriptName = (String) tableModel.getValueAt(row, 0);
            loadScript(scriptName, dialog);
        });
        buttonPanel.add(loadBtn);

        JButton activateBtn = new JButton("Activate");
        activateBtn.setToolTipText("Set selected script as active");
        activateBtn.addActionListener(event -> presenter.handleActivate());
        buttonPanel.add(activateBtn);

        JButton deactivateBtn = new JButton("Deactivate All");
        deactivateBtn.setToolTipText("Deactivate all scripts");
        deactivateBtn.addActionListener(event -> presenter.handleDeactivate());
        buttonPanel.add(deactivateBtn);

        JButton renameBtn = new JButton("Rename...");
        renameBtn.setToolTipText("Rename selected script");
        renameBtn.addActionListener(event -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dialog, "Please select a script first.");
                return;
            }
            String oldName = (String) tableModel.getValueAt(row, 0);
            String newName = JOptionPane.showInputDialog(dialog, "New name:", oldName);
            presenter.handleRename(newName);
        });
        buttonPanel.add(renameBtn);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setToolTipText("Delete selected script from server");
        deleteBtn.addActionListener(event -> presenter.handleDelete());
        buttonPanel.add(deleteBtn);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() > 3) {
                    return;
                }
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        String scriptName = (String) tableModel.getValueAt(row, 0);
                        loadScript(scriptName, dialog);
                    }
                }
            }
        });

        dialog.pack();
        if (dialog.getWidth() < 400) {
            dialog.setSize(400, dialog.getHeight());
        }
        if (dialog.getHeight() < 300) {
            dialog.setSize(dialog.getWidth(), 300);
        }
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }

    private void loadScript(String scriptName, JDialog dialog) {
        try {
            List<SieveScript> scripts = parentFrame.getServer().getListScripts();
            for (SieveScript script : scripts) {
                if (script.getName().equals(scriptName)) {
                    parentFrame.setScript(script);
                    parentFrame.updateStatus();
                    dialog.dispose();
                    return;
                }
            }
        } catch (IOException | ParseException ex) {
            JOptionPane.showMessageDialog(dialog, "Error loading script: " + ex.getMessage());
        }
    }
}
