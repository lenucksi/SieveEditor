package de.febrildur.sieveeditor.system;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;

public class SieveCompletionHandler {

	private final JTextComponent textComponent;
	private final CompletionProvider provider;
	private final JPopupMenu popupMenu;
	private final DefaultListModel<Completion> listModel;
	private final JList<Completion> completionList;
	private final Timer autoActivationTimer;

	public SieveCompletionHandler(JTextComponent textComponent) {
		this.textComponent = textComponent;

		provider = new SieveCompletionProvider();

		listModel = new DefaultListModel<>();
		completionList = new JList<>(listModel);
		completionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		popupMenu = new JPopupMenu();
		popupMenu.add(new JScrollPane(completionList));
		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				// Restore focus to text component when popup is dismissed
				textComponent.requestFocusInWindow();
			}
		});

		// Ctrl+Space trigger — use WHEN_IN_FOCUSED_WINDOW so it works
		// even when the text area is inside an RTextScrollPane.
		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK);
		textComponent.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, "sieve-completion");
		textComponent.getActionMap().put("sieve-completion", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showCompletionPopup();
			}
		});

		// Enter in the popup list inserts the selection
		completionList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "insert");
		completionList.getActionMap().put("insert", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				insertSelectedCompletion();
			}
		});

		// Double-click inserts
		completionList.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (e.getClickCount() == 2) {
					insertSelectedCompletion();
				}
			}
		});

		// Auto-activation: when user types, after a delay show completions
		autoActivationTimer = new Timer(300, e -> showCompletionPopup());
		autoActivationTimer.setRepeats(false);

		textComponent.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				if (e.getLength() == 1) {
					autoActivationTimer.restart();
				}
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				autoActivationTimer.stop();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {}
		});
	}

	private void showCompletionPopup() {
		String enteredText = provider.getAlreadyEnteredText(textComponent);
		if (enteredText == null) {
			popupMenu.setVisible(false);
			return;
		}

		// If nothing entered and popup is showing, hide it
		if (enteredText.isEmpty()) {
			popupMenu.setVisible(false);
			return;
		}

		List<Completion> completions = provider.getCompletions(textComponent);
		if (completions == null || completions.isEmpty()) {
			popupMenu.setVisible(false);
			return;
		}

		listModel.clear();
		for (Completion c : completions) {
			listModel.addElement(c);
		}

		// Position at caret using component-relative coordinates
		// (JPopupMenu.show() uses component-relative, not screen coords,
		//  which avoids Wayland JWindow positioning issues)
		try {
			java.awt.Rectangle caretRect = textComponent.modelToView(
				textComponent.getCaretPosition());
			if (caretRect != null) {
				popupMenu.show(textComponent, caretRect.x,
					caretRect.y + caretRect.height);
				completionList.setSelectedIndex(0);
				completionList.requestFocusInWindow();
			}
		} catch (BadLocationException ex) {
			// ignore
		}
	}

	private void insertSelectedCompletion() {
		Completion c = completionList.getSelectedValue();
		if (c != null) {
			String enteredText = provider.getAlreadyEnteredText(textComponent);
			int dot = textComponent.getCaretPosition();
			int start = dot - enteredText.length();
			textComponent.select(start, dot);
			textComponent.replaceSelection(c.getReplacementText());
		}
		popupMenu.setVisible(false);
		textComponent.requestFocusInWindow();
	}
}
