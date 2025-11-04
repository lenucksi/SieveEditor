package de.febrildur.sieveeditor.actions;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.fluffypeople.managesieve.ParseException;
import com.fluffypeople.managesieve.SieveScript;

import de.febrildur.sieveeditor.Application;

public class ActionActivateDeactivateScript extends AbstractAction {

	private Application parentFrame;

	public ActionActivateDeactivateScript(Application parentFrame) {
		putValue("Name", "List of scripts...");
		this.parentFrame = parentFrame;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			List<SieveScript> liste = parentFrame.getServer().getListScripts();

			JPanel panel = new JPanel();

			final JDialog frame = new JDialog(parentFrame, "Scripts", true);
			frame.getContentPane().add(panel);
			frame.setSize(300, 200);
			frame.setLocationRelativeTo(parentFrame);

			String columnNames[] = { "Name", "Aktiv" };
			String rowData[][] = new String[liste.size()][2];
			for (int i = 0; i < liste.size(); i++) {
				rowData[i][0] = liste.get(i).getName();
				rowData[i][1] = liste.get(i).isActive() ? "active" : "";
			}

			JTable table = new JTable(rowData, columnNames);
			frame.add(new JScrollPane(table));

			JPopupMenu popmen = new JPopupMenu();
			JMenuItem activate = new JMenuItem("activate");
			activate.addActionListener((event) -> {
				String script = rowData[table.getSelectedRow()][0];
				try {
					parentFrame.getServer().activateScript(script);
				} catch (IOException | ParseException e1) {
					JOptionPane.showMessageDialog(parentFrame, e1.getMessage());
					return;
				}
				JOptionPane.showMessageDialog(parentFrame, "activate " + script);
			});
			popmen.add(activate);
			
			JMenuItem deactivate = new JMenuItem("deactivate all");
			deactivate.addActionListener((event) -> {
				try {
					parentFrame.getServer().deactivateScript();
				} catch (IOException | ParseException e1) {
					JOptionPane.showMessageDialog(parentFrame, e1.getMessage());
					return;
				}
				JOptionPane.showMessageDialog(parentFrame, "deactivate all scripts");
			});
			popmen.add(deactivate);
			
			JMenuItem rename = new JMenuItem("rename...");
			rename.addActionListener((event) -> {
				try {
					String script = rowData[table.getSelectedRow()][0];
					String newName = JOptionPane.showInputDialog("Rename to:", script);
					
					parentFrame.getServer().rename(script, newName);
				} catch (IOException | ParseException e1) {
					JOptionPane.showMessageDialog(parentFrame, e1.getMessage());
					return;
				}
				JOptionPane.showMessageDialog(parentFrame, "deactivate all scripts");
			});
			popmen.add(rename);
			
			table.addMouseListener(new MouseAdapter() {
				public void mouseReleased(MouseEvent me) {
					if (me.isPopupTrigger())
						popmen.show(me.getComponent(), me.getX(), me.getY());
				}
			});

			frame.setVisible(true);
		} catch (IOException | ParseException e1) {
			JOptionPane.showMessageDialog(parentFrame, e1.getClass().getName() + ": " + e1.getMessage());
		}

	}

}
