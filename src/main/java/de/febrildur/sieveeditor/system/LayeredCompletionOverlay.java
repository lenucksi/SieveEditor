package de.febrildur.sieveeditor.system;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;

public class LayeredCompletionOverlay {

    private static final Logger LOGGER = Logger.getLogger(LayeredCompletionOverlay.class.getName());

    private final JTextComponent textComponent;
    private final CompletionProvider provider;
    private final JFrame frame;

    private final JPanel popupPanel;
    private final DefaultListModel<Completion> listModel;
    private final JList<Completion> completionList;
    private final Timer autoActivationTimer;
    private final CaretListener caretListener;

    private boolean visible;
    private AWTEventListener dismissListener;
    private final Map<KeyStroke, SavedBinding> savedBindings = new HashMap<>();

    // Key bindings installed while overlay is visible
    private static final String ACTION_PREFIX = "lo-";
    private static final KeyStroke KS_UP = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
    private static final KeyStroke KS_DOWN = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
    private static final KeyStroke KS_ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    private static final KeyStroke KS_TAB = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
    private static final KeyStroke KS_PAGE_UP = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);
    private static final KeyStroke KS_PAGE_DOWN = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);
    private static final KeyStroke KS_HOME = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0);
    private static final KeyStroke KS_END = KeyStroke.getKeyStroke(KeyEvent.VK_END, 0);
    private static final KeyStroke KS_ESCAPE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

    private static class SavedBinding {
        Object key;
        Action action;
    }

    public LayeredCompletionOverlay(JTextComponent textComponent, CompletionProvider provider, JFrame frame) {
        this.textComponent = textComponent;
        this.provider = provider;
        this.frame = frame;

        listModel = new DefaultListModel<>();
        completionList = new JList<>(listModel);
        completionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        completionList.setCellRenderer(new CompletionCellRenderer());

        popupPanel = new JPanel(new BorderLayout());
        popupPanel.setBorder(javax.swing.BorderFactory.createLineBorder(
            new java.awt.Color(180, 180, 180)));
        popupPanel.add(new JScrollPane(completionList));

        // Double-click on list item inserts
        completionList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    insertSelectedCompletion();
                }
            }
        });

        // Auto-activation timer (300ms after typing)
        autoActivationTimer = new Timer(300, e -> showFromTimer());
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
                hide();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {}
        });

        // CaretListener: refreshes completions when cursor moves (left/right arrows)
        caretListener = e -> {
            if (visible) {
                refreshCompletions();
            }
        };

        // Ctrl+Space trigger (WHEN_FOCUSED scope to match overlay navigation bindings)
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, java.awt.event.InputEvent.CTRL_DOWN_MASK);
        textComponent.getInputMap().put(ks, "layered-completion");
        textComponent.getActionMap().put("layered-completion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFromTrigger();
            }
        });
    }

    public void showFromTimer() {
        show(false);
    }

    public void showFromTrigger() {
        show(true);
    }

    private void show(boolean force) {
        String entered = provider.getAlreadyEnteredText(textComponent);
        if (entered == null || (entered.isEmpty() && !force)) {
            hide();
            return;
        }
        List<Completion> completions = provider.getCompletions(textComponent);
        if (completions == null || completions.isEmpty()) {
            hide();
            return;
        }

        listModel.clear();
        int maxWidth = 0;
        Font font = completionList.getFont();
        FontMetrics fm = completionList.getFontMetrics(font);
        Font boldFont = font.deriveFont(Font.BOLD);
        FontMetrics bfm = completionList.getFontMetrics(boldFont);
        int approxDescWidth = fm.stringWidth(" - RFC 0000"); // approximate description width

        for (Completion c : completions) {
            listModel.addElement(c);
            int cmdW = bfm.stringWidth(c.getInputText());
            int descW = 0;
            if (c instanceof BasicCompletion) {
                String desc = ((BasicCompletion)c).getShortDescription();
                if (desc != null) {
                    descW = fm.stringWidth(" - " + desc);
                }
            }
            maxWidth = Math.max(maxWidth, cmdW + descW + 30);
        }

        popupPanel.setPreferredSize(new Dimension(
            Math.max(280, Math.min(600, maxWidth)),
            Math.min(400, Math.max(80, completions.size() * 22 + 5))));
        popupPanel.setSize(popupPanel.getPreferredSize());
        popupPanel.validate();

        JLayeredPane layeredPane = frame.getRootPane().getLayeredPane();
        positionPopup(layeredPane);

        layeredPane.add(popupPanel, JLayeredPane.POPUP_LAYER);
        popupPanel.setVisible(true);
        visible = true;

        if (listModel.size() > 0) {
            completionList.setSelectedIndex(0);
            completionList.ensureIndexIsVisible(0);
        }

        installDismissListener();
        textComponent.addCaretListener(caretListener);
        installOverlayKeyBindings();
    }

    private void refreshCompletions() {
        if (!visible) { return; }
        String entered = provider.getAlreadyEnteredText(textComponent);
        if (entered == null || entered.isEmpty()) {
            hide();
            return;
        }
        List<Completion> completions = provider.getCompletions(textComponent);
        if (completions == null || completions.isEmpty()) {
            hide();
            return;
        }
        listModel.clear();
        for (Completion c : completions) {
            listModel.addElement(c);
        }
        if (listModel.size() > 0) {
            completionList.setSelectedIndex(0);
            completionList.ensureIndexIsVisible(0);
        }
        popupPanel.repaint();
    }

    public void hide() {
        if (!visible) { return; }
        visible = false;
        textComponent.removeCaretListener(caretListener);
        uninstallOverlayKeyBindings();
        uninstallDismissListener();
        JLayeredPane layeredPane = frame.getRootPane().getLayeredPane();
        layeredPane.remove(popupPanel);
        layeredPane.repaint();
    }

    private void positionPopup(JLayeredPane layeredPane) {
        try {
            Rectangle caretRect = textComponent.modelToView(
                textComponent.getCaretPosition());
            if (caretRect == null) { return; }

            Point popupPoint = SwingUtilities.convertPoint(
                textComponent,
                caretRect.x,
                caretRect.y + caretRect.height + 1,
                layeredPane);

            Rectangle lpBounds = layeredPane.getVisibleRect();
            if (popupPoint.y + popupPanel.getHeight() > lpBounds.height) {
                Point above = SwingUtilities.convertPoint(
                    textComponent, caretRect.x,
                    caretRect.y - popupPanel.getHeight() - 1, layeredPane);
                if (above.y >= 0) { popupPoint = above; }
                else { popupPoint.y = Math.max(0, lpBounds.height - popupPanel.getHeight()); }
            }
            if (popupPoint.x + popupPanel.getWidth() > lpBounds.width) {
                popupPoint.x = Math.max(0, lpBounds.width - popupPanel.getWidth());
            }
            if (popupPoint.x < 0) { popupPoint.x = 0; }
            if (popupPoint.y < 0) { popupPoint.y = 0; }

            popupPanel.setLocation(popupPoint.x, popupPoint.y);
        } catch (BadLocationException e) {
            LOGGER.fine("Could not position completion popup: " + e.getMessage());
        }
    }

    private void installDismissListener() {
        dismissListener = event -> {
            if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                MouseEvent me = (MouseEvent) event;
                Component source = me.getComponent();
                Point p = SwingUtilities.convertPoint(source, me.getPoint(),
                    frame.getRootPane().getLayeredPane());
                if (!popupPanel.getBounds().contains(p)) {
                    SwingUtilities.invokeLater(this::hide);
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(dismissListener,
            java.awt.AWTEvent.MOUSE_EVENT_MASK);
    }

    private void uninstallDismissListener() {
        if (dismissListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(dismissListener);
            dismissListener = null;
        }
    }

    private void installOverlayKeyBindings() {
        saveAndReplace(KS_UP, "lo-up", new OverlayAction("up"));
        saveAndReplace(KS_DOWN, "lo-down", new OverlayAction("down"));
        saveAndReplace(KS_ENTER, "lo-enter", new OverlayAction("enter"));
        saveAndReplace(KS_TAB, "lo-tab", new OverlayAction("enter")); // Tab = Enter
        saveAndReplace(KS_PAGE_UP, "lo-pageUp", new OverlayAction("pageUp"));
        saveAndReplace(KS_PAGE_DOWN, "lo-pageDown", new OverlayAction("pageDown"));
        saveAndReplace(KS_HOME, "lo-home", new OverlayAction("home"));
        saveAndReplace(KS_END, "lo-end", new OverlayAction("end"));
        saveAndReplace(KS_ESCAPE, "lo-escape", new OverlayAction("escape"));
    }

    private void saveAndReplace(KeyStroke ks, String actionKey, Action action) {
        InputMap im = textComponent.getInputMap(); // WHEN_FOCUSED
        ActionMap am = textComponent.getActionMap();
        SavedBinding saved = new SavedBinding();
        saved.key = im.get(ks);
        im.put(ks, actionKey);
        saved.action = am.get(actionKey);
        am.put(actionKey, action);
        savedBindings.put(ks, saved);
    }

    private void uninstallOverlayKeyBindings() {
        InputMap im = textComponent.getInputMap(); // WHEN_FOCUSED
        ActionMap am = textComponent.getActionMap();
        for (Map.Entry<KeyStroke, SavedBinding> entry : savedBindings.entrySet()) {
            KeyStroke ks = entry.getKey();
            SavedBinding saved = entry.getValue();
            Object currentKey = im.get(ks);
            if (saved.action != null && currentKey != null) {
                am.put(currentKey, saved.action);
            }
            if (saved.key != null) {
                im.put(ks, saved.key);
            } else {
                im.remove(ks);
            }
        }
        savedBindings.clear();
    }

    private class OverlayAction extends AbstractAction {
        private final String action;

        OverlayAction(String action) { this.action = action; }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!visible) { return; }
            int size = listModel.getSize();
            if (size == 0) { return; }
            int idx = completionList.getSelectedIndex();
            if (idx < 0) { idx = 0; }
            switch (action) {
                case "up":
                    idx = (idx <= 0) ? size - 1 : idx - 1;
                    break;
                case "down":
                    idx = (idx >= size - 1) ? 0 : idx + 1;
                    break;
                case "enter":
                    insertSelectedCompletion();
                    return;
                case "pageUp": {
                    int vis = completionList.getVisibleRowCount();
                    idx = Math.max(0, idx - vis);
                    break;
                }
                case "pageDown": {
                    int vis = completionList.getVisibleRowCount();
                    idx = Math.min(size - 1, idx + vis);
                    break;
                }
                case "home":
                    idx = 0;
                    break;
                case "end":
                    idx = size - 1;
                    break;
                case "escape":
                    hide();
                    return;
            }
            completionList.setSelectedIndex(idx);
            completionList.ensureIndexIsVisible(idx);
        }
    }

	private void insertSelectedCompletion() {
		Completion c = completionList.getSelectedValue();
		if (c != null) {
			String repl = c.getReplacementText();
			int dot = textComponent.getCaretPosition();
			int start;
			if (repl.startsWith("##")) {
				// On ## comment lines, replace from the start of the line
				javax.swing.text.Document doc = textComponent.getDocument();
				javax.swing.text.Element root = doc.getDefaultRootElement();
				javax.swing.text.Element line = root.getElement(root.getElementIndex(dot));
				start = line.getStartOffset();
			} else {
				String entered = provider.getAlreadyEnteredText(textComponent);
				start = dot - entered.length();
			}
			textComponent.select(start, dot);
			textComponent.replaceSelection(repl);
		}
		hide();
		textComponent.requestFocusInWindow();
	}

    /**
     * Renders completions with bold command name and gray description.
     */
    private static class CompletionCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Completion) {
                Completion c = (Completion) value;
                String cmd = c.getInputText();
                String desc = "";
                if (c instanceof BasicCompletion) {
                    String d = ((BasicCompletion) c).getShortDescription();
                    if (d != null) { desc = d; }
                }
                StringBuilder sb = new StringBuilder("<html><b>");
                sb.append(escapeHtml(cmd));
                if (!desc.isEmpty()) {
                    sb.append("</b> <span style='color:#888888'>- ");
                    sb.append(escapeHtml(desc));
                    sb.append("</span>");
                } else {
                    sb.append("</b>");
                }
                setText(sb.toString());
            }
            return this;
        }

        private String escapeHtml(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
        }
    }
}
