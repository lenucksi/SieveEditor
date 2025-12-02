package de.febrildur.sieveeditor.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import com.fluffypeople.managesieve.ParseException;

import de.febrildur.sieveeditor.Application;

public class ActionCheckScript extends AbstractAction {

	private Application parentFrame;

	public ActionCheckScript(Application parentFrame) {
		putValue("Name", "Check Script");
		this.parentFrame = parentFrame;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String scriptInfo;
		try {
			scriptInfo = parentFrame.getServer().checkScript(parentFrame.getScriptText());
			JOptionPane.showMessageDialog(parentFrame, scriptInfo);
		} catch (IOException | ParseException e1) {
			JOptionPane.showMessageDialog(parentFrame, e1.getMessage());
		}
	}
}
