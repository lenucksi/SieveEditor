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
			JOptionPane.showMessageDialog(parentFrame, scriptInfo, "Script Check", JOptionPane.INFORMATION_MESSAGE);
		} catch (IOException | ParseException e1) {
			showErrorDialog(e1.getMessage());
		}
	}

	/**
	 * Shows an error dialog with optional "Jump to Line" button if line number is found.
	 *
	 * @param errorMessage the error message from the server
	 */
	private void showErrorDialog(String errorMessage) {
		de.febrildur.sieveeditor.util.SieveErrorParser.ErrorInfo errorInfo =
			de.febrildur.sieveeditor.util.SieveErrorParser.parseError(errorMessage);

		if (errorInfo.hasLineNumber()) {
			// Show dialog with "Jump to Error" button
			int lineNumber = errorInfo.getLineNumber().get();
			Object[] options = {"Jump to Line " + lineNumber, "Close"};
			int choice = JOptionPane.showOptionDialog(
				parentFrame,
				errorInfo.getMessage(),
				"Script Check Error",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.ERROR_MESSAGE,
				null,
				options,
				options[0]
			);

			if (choice == JOptionPane.YES_OPTION) {
				parentFrame.jumpToLine(lineNumber);
			}
		} else {
			// No line number found - show simple error dialog
			JOptionPane.showMessageDialog(
				parentFrame,
				errorMessage,
				"Script Check Error",
				JOptionPane.ERROR_MESSAGE
			);
		}
	}
}
