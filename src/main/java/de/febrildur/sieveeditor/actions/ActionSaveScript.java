package de.febrildur.sieveeditor.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import de.febrildur.sieveeditor.Application;

public class ActionSaveScript extends AbstractAction {

	private Application parentFrame;

	public ActionSaveScript(Application parentFrame) {
		putValue("Name", "Save");
		this.parentFrame = parentFrame;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		parentFrame.save();
		parentFrame.updateStatus();
		JOptionPane.showMessageDialog(parentFrame, "Script saved.");
	}

}
