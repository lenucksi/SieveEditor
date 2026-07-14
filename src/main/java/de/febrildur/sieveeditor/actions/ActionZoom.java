package de.febrildur.sieveeditor.actions;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import com.formdev.flatlaf.util.UIScale;

import org.fife.ui.rtextarea.RTextScrollPane;

import de.febrildur.sieveeditor.Application;

public class ActionZoom extends AbstractAction {

	public enum ZoomOperation {
		IN, OUT, RESET
	}

	private final Application app;
	private final RTextScrollPane scrollPane;
	private final ZoomOperation operation;

	public ActionZoom(Application app, RTextScrollPane scrollPane, ZoomOperation operation,
			String name, KeyStroke accelerator) {
		putValue(NAME, name);
		putValue(ACCELERATOR_KEY, accelerator);
		this.app = app;
		this.scrollPane = scrollPane;
		this.operation = operation;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int currentSize = app.getProp().getFontSize();

		switch (operation) {
			case IN:
				currentSize++;
				break;
			case OUT:
				currentSize--;
				break;
			case RESET:
				currentSize = 13;
				break;
		}

		app.getProp().setFontSize(currentSize);
		app.getProp().write();

		int scaledEditorSize = UIScale.scale(currentSize);
		app.getScriptArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, scaledEditorSize));

		int scaledGutterSize = UIScale.scale(currentSize + 2);
		scrollPane.getGutter().setLineNumberFont(new Font(Font.MONOSPACED, Font.PLAIN, scaledGutterSize));
	}
}
