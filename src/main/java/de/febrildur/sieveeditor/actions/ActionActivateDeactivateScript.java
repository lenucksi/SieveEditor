package de.febrildur.sieveeditor.actions;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
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
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import com.fluffypeople.managesieve.ParseException;
import com.fluffypeople.managesieve.SieveScript;

import de.febrildur.sieveeditor.Application;

public class ActionActivateDeactivateScript extends AbstractAction {

	private Application parentFrame;

	public ActionActivateDeactivateScript(Application parentFrame) {
		putValue("Name", "Manage Scripts...");
		this.parentFrame = parentFrame;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			final JDialog dialog = new JDialog(parentFrame, "Manage Scripts", true);
			dialog.setLayout(new BorderLayout(5, 5));

			// Create table model (non-editable)
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

			// Load scripts into table
			refreshTable(tableModel);

			JScrollPane scrollPane = new JScrollPane(table);
			dialog.add(scrollPane, BorderLayout.CENTER);

			// Button panel
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
			activateBtn.addActionListener(event -> {
				int row = table.getSelectedRow();
				if (row < 0) {
					JOptionPane.showMessageDialog(dialog, "Please select a script first.");
					return;
				}
				String scriptName = (String) tableModel.getValueAt(row, 0);
				try {
					parentFrame.getServer().activateScript(scriptName);
					refreshTable(tableModel);
				} catch (IOException | ParseException ex) {
					JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
				}
			});
			buttonPanel.add(activateBtn);

			JButton deactivateBtn = new JButton("Deactivate All");
			deactivateBtn.setToolTipText("Deactivate all scripts");
			deactivateBtn.addActionListener(event -> {
				try {
					parentFrame.getServer().deactivateScript();
					refreshTable(tableModel);
				} catch (IOException | ParseException ex) {
					JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
				}
			});
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
				if (newName != null && !newName.isEmpty() && !newName.equals(oldName)) {
					try {
						parentFrame.getServer().rename(oldName, newName);
						refreshTable(tableModel);
					} catch (IOException | ParseException ex) {
						JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
					}
				}
			});
			buttonPanel.add(renameBtn);

			JButton deleteBtn = new JButton("Delete");
			deleteBtn.setToolTipText("Delete selected script from server");
			deleteBtn.addActionListener(event -> {
				int row = table.getSelectedRow();
				if (row < 0) {
					JOptionPane.showMessageDialog(dialog, "Please select a script first.");
					return;
				}
				String scriptName = (String) tableModel.getValueAt(row, 0);
				int confirm = JOptionPane.showConfirmDialog(dialog,
					"Delete script \"" + scriptName + "\"?\n\nThis cannot be undone.",
					"Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (confirm == JOptionPane.YES_OPTION) {
					try {
						parentFrame.getServer().deleteScript(scriptName);
						refreshTable(tableModel);
					} catch (IOException | ParseException ex) {
						JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
					}
				}
			});
			buttonPanel.add(deleteBtn);

			dialog.add(buttonPanel, BorderLayout.SOUTH);

			// Double-click to load script
			table.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					// Filter out horizontal scroll buttons (4/5) to prevent IllegalArgumentException
					// Only process standard mouse buttons (1=left, 2=middle, 3=right)
					if (e.getButton() > 3) {
						return; // Ignore horizontal scroll wheel events
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

			// Size and show dialog
			dialog.pack();
			// Ensure reasonable minimum size
			if (dialog.getWidth() < 400) {
				dialog.setSize(400, dialog.getHeight());
			}
			if (dialog.getHeight() < 300) {
				dialog.setSize(dialog.getWidth(), 300);
			}
			dialog.setLocationRelativeTo(parentFrame);
			dialog.setVisible(true);

		} catch (IOException | ParseException e1) {
			JOptionPane.showMessageDialog(parentFrame, e1.getClass().getName() + ": " + e1.getMessage());
		}
	}

	private void refreshTable(DefaultTableModel model) throws IOException, ParseException {
		model.setRowCount(0);
		List<SieveScript> scripts = parentFrame.getServer().getListScripts();
		for (SieveScript script : scripts) {
			model.addRow(new Object[]{script.getName(), script.isActive() ? "active" : ""});
		}
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
