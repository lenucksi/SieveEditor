package de.febrildur.sieveeditor.actions;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.fluffypeople.managesieve.ParseException;
import com.fluffypeople.managesieve.SieveScript;

import de.febrildur.sieveeditor.Application;
import de.febrildur.sieveeditor.system.ToString;
import de.febrildur.sieveeditor.system.ToStringListCellRenderer;

public class ActionLoadScript extends AbstractAction {

	private Application parentFrame;

	public ActionLoadScript(Application parentFrame) {
		putValue("Name", "Load...");
		this.parentFrame = parentFrame;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final JDialog frame = new JDialog(parentFrame, "Select Script", true);
		try {
			JPanel panel = new JPanel();
			panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
			GridLayout layout = new GridLayout(2, 2, 6, 6);
			panel.setLayout(layout);

			frame.getContentPane().add(panel);
			frame.setSize(400, 120);
			frame.setLocationRelativeTo(parentFrame);
			
			JLabel labelScript = new JLabel("Script");
			panel.add(labelScript);
			SieveScript[] liste = parentFrame.getServer().getListScripts().toArray(new SieveScript[0]);
			JComboBox<SieveScript> tfScript = new JComboBox<SieveScript>(liste);
			final ToString toString = new ToString() {
				public String toString(final Object object) {
					final SieveScript value = (SieveScript) object;
					return value.getName();
			    }
			};
			tfScript.setRenderer(new ToStringListCellRenderer(
					tfScript.getRenderer(), toString));
			panel.add(tfScript);
			
			JButton buttonOK = new JButton("OK");
			buttonOK.addActionListener((event) -> {
				try {
					parentFrame.setScript((SieveScript) tfScript.getSelectedItem());
					parentFrame.updateStatus();
					frame.setVisible(false);
				} catch (NumberFormatException | IOException | ParseException e1) {
					JOptionPane.showMessageDialog(frame, e1.getClass().getName() + ": " + e1.getMessage());
				}
			});
			panel.add(buttonOK);
			
			frame.setVisible(true);
		} catch (IOException | ParseException e1) {
			JOptionPane.showMessageDialog(frame, e1.getClass().getName() + ": " + e1.getMessage());
		}
	}

}
