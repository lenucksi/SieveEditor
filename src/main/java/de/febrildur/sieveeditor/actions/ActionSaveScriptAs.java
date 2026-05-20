package de.febrildur.sieveeditor.actions;
// SPDX-FileCopyrightText: 2019, 2020 Zwixx
// SPDX-FileCopyrightText: 2025 Claude
// SPDX-FileCopyrightText: 2025 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import de.febrildur.sieveeditor.Application;

public class ActionSaveScriptAs extends AbstractAction {

	private Application parentFrame;

	public ActionSaveScriptAs(Application parentFrame) {
	putValue(NAME, "Save as...");
	putValue(ACCELERATOR_KEY, javax.swing.KeyStroke.getKeyStroke(
		java.awt.event.KeyEvent.VK_S,
		java.awt.event.KeyEvent.CTRL_DOWN_MASK | java.awt.event.KeyEvent.ALT_DOWN_MASK));
	this.parentFrame = parentFrame;
}

	@Override
	public void actionPerformed(ActionEvent e) {
		String newName = JOptionPane.showInputDialog("Rename to:", parentFrame.getScriptName());

		try {
			parentFrame.save(newName);
			parentFrame.updateStatus();
		} catch (NumberFormatException e1) {
			JOptionPane.showMessageDialog(parentFrame, e1.getClass().getName() + ": " + e1.getMessage());
		}
	}
}
