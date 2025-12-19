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
		// Validate script before saving
		if (!validateBeforeSave()) {
			return; // User cancelled or validation failed
		}

		parentFrame.save();
		parentFrame.updateStatus();
		JOptionPane.showMessageDialog(parentFrame, "Script saved.");
	}

	/**
	 * Validates the script before saving and asks user for confirmation if errors found.
	 *
	 * @return true if save should proceed, false if user cancelled
	 */
	private boolean validateBeforeSave() {
		try {
			// Run script validation
			String validationResult = parentFrame.getServer().checkScript(parentFrame.getScriptText());

			// Validation passed - show result and continue with save
			// Note: Some servers return "OK" or empty string on success
			return true;

		} catch (java.io.IOException | com.fluffypeople.managesieve.ParseException e) {
			// Validation failed - parse error and ask user
			return handleValidationError(e.getMessage());
		}
	}

	/**
	 * Handles validation errors by showing error message and asking user if they want to save anyway.
	 *
	 * @param errorMessage the validation error message
	 * @return true if user wants to save anyway, false otherwise
	 */
	private boolean handleValidationError(String errorMessage) {
		de.febrildur.sieveeditor.util.SieveErrorParser.ErrorInfo errorInfo =
			de.febrildur.sieveeditor.util.SieveErrorParser.parseError(errorMessage);

		String dialogMessage = "Script validation failed:\n\n" + errorInfo.getMessage() +
			"\n\nDo you want to save the script anyway?";

		Object[] options;
		if (errorInfo.hasLineNumber()) {
			int lineNumber = errorInfo.getLineNumber().get();
			options = new Object[]{"Jump to Line " + lineNumber, "Save Anyway", "Cancel"};
		} else {
			options = new Object[]{"Save Anyway", "Cancel"};
		}

		int choice = JOptionPane.showOptionDialog(
			parentFrame,
			dialogMessage,
			"Script Validation Error",
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE,
			null,
			options,
			options[options.length - 1] // Default to "Cancel"
		);

		if (errorInfo.hasLineNumber()) {
			// 3-button dialog: Jump / Save / Cancel
			if (choice == 0) {
				// Jump to Line
				parentFrame.jumpToLine(errorInfo.getLineNumber().get());
				return false; // Don't save
			} else if (choice == 1) {
				// Save Anyway
				return true;
			} else {
				// Cancel (or dialog closed)
				return false;
			}
		} else {
			// 2-button dialog: Save / Cancel
			return choice == 0; // Save Anyway = 0, Cancel = 1
		}
	}

}
