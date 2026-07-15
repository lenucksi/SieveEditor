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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;

public class SieveCompletionHandler {

	private static final Logger LOGGER = Logger.getLogger(SieveCompletionHandler.class.getName());

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
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				LOGGER.fine("[SieveCompletionHandler] popupMenuWillBecomeVisible");
			}
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				LOGGER.fine("[SieveCompletionHandler] popupMenuWillBecomeInvisible");
			}
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				LOGGER.fine("[SieveCompletionHandler] popupMenuCanceled");
				textComponent.requestFocusInWindow();
			}
		});

		// Ctrl+Space trigger
		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK);
		textComponent.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, "sieve-completion");
		textComponent.getActionMap().put("sieve-completion", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LOGGER.fine("[SieveCompletionHandler] Ctrl+Space triggered");
				showCompletionPopup();
			}
		});
		LOGGER.fine("[SieveCompletionHandler] Ctrl+Space binding installed");

		// Enter in the popup list inserts the selection
		completionList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "insert");
		completionList.getActionMap().put("insert", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LOGGER.fine("[SieveCompletionHandler] Enter pressed in popup list");
				insertSelectedCompletion();
			}
		});

		// Double-click inserts
		completionList.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (e.getClickCount() == 2) {
					LOGGER.fine("[SieveCompletionHandler] Double-click on completion");
					insertSelectedCompletion();
				}
			}
		});

		// Auto-activation: when user types, after a delay show completions
		autoActivationTimer = new Timer(300, e -> {
			LOGGER.fine("[SieveCompletionHandler] Auto-activation timer fired");
			showCompletionPopup();
		});
		autoActivationTimer.setRepeats(false);

		textComponent.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				LOGGER.fine("[SieveCompletionHandler] insertUpdate: len=" + e.getLength()
					+ ", offset=" + e.getOffset());
				if (e.getLength() == 1) {
					autoActivationTimer.restart();
				}
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				LOGGER.fine("[SieveCompletionHandler] removeUpdate: len=" + e.getLength());
				autoActivationTimer.stop();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				LOGGER.fine("[SieveCompletionHandler] changedUpdate");
			}
		});
		LOGGER.fine("[SieveCompletionHandler] DocumentListener installed on: "
			+ textComponent.getDocument().getClass().getName());
	}

	private void showCompletionPopup() {
		LOGGER.fine("=== SieveCompletionHandler.showCompletionPopup() ===");

		String enteredText = provider.getAlreadyEnteredText(textComponent);
		LOGGER.fine("  enteredText: '" + enteredText + "'");

		if (enteredText == null) {
			LOGGER.fine("  enteredText is null, hiding popup");
			popupMenu.setVisible(false);
			return;
		}

		if (enteredText.isEmpty()) {
			LOGGER.fine("  enteredText is empty, hiding popup");
			popupMenu.setVisible(false);
			return;
		}

		List<Completion> completions = provider.getCompletions(textComponent);
		LOGGER.fine("  completions from provider: "
			+ (completions != null ? completions.size() : "null"));

		if (completions == null || completions.isEmpty()) {
			LOGGER.fine("  no completions returned, hiding popup");
			popupMenu.setVisible(false);
			return;
		}

		for (int i = 0; i < Math.min(completions.size(), 5); i++) {
			Completion c = completions.get(i);
			LOGGER.fine("    [" + i + "] inputText='" + c.getInputText()
				+ "' replaceText='" + c.getReplacementText() + "'");
		}
		if (completions.size() > 5) {
			LOGGER.fine("    ... and " + (completions.size() - 5) + " more");
		}

		listModel.clear();
		for (Completion c : completions) {
			listModel.addElement(c);
		}
		LOGGER.fine("  listModel size after fill: " + listModel.size());

		// Position at caret using component-relative coordinates
		try {
			java.awt.Rectangle caretRect = textComponent.modelToView(
				textComponent.getCaretPosition());
			LOGGER.fine("  caretRect: " + caretRect);
			if (caretRect != null) {
				LOGGER.fine("  popupMenu.show(" + caretRect.x + ", "
					+ (caretRect.y + caretRect.height) + ")");
				popupMenu.show(textComponent, caretRect.x,
					caretRect.y + caretRect.height);
				LOGGER.fine("  popupMenu visible: " + popupMenu.isVisible()
					+ ", showing: " + popupMenu.isShowing());
				completionList.setSelectedIndex(0);
			} else {
				LOGGER.fine("  caretRect is null, cannot show popup");
			}
		} catch (BadLocationException ex) {
			LOGGER.fine("  BadLocationException: " + ex.getMessage());
		}
	}

	private void insertSelectedCompletion() {
		Completion c = completionList.getSelectedValue();
		LOGGER.fine("[SieveCompletionHandler] insertSelectedCompletion: " + c);
		if (c != null) {
			String enteredText = provider.getAlreadyEnteredText(textComponent);
			int dot = textComponent.getCaretPosition();
			int start = dot - enteredText.length();
			LOGGER.fine("  replacing '" + enteredText + "' with '"
				+ c.getReplacementText() + "' at " + start + "-" + dot);
			textComponent.select(start, dot);
			textComponent.replaceSelection(c.getReplacementText());
		}
		popupMenu.setVisible(false);
		textComponent.requestFocusInWindow();
	}
}
