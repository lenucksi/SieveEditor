package de.febrildur.sieveeditor.system;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

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

    private boolean visible;
    private AWTEventListener dismissListener;

    public LayeredCompletionOverlay(JTextComponent textComponent, CompletionProvider provider, JFrame frame) {
        this.textComponent = textComponent;
        this.provider = provider;
        this.frame = frame;

        listModel = new DefaultListModel<>();
        completionList = new JList<>(listModel);
        completionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        popupPanel = new JPanel(new BorderLayout());
        popupPanel.setBorder(javax.swing.BorderFactory.createLineBorder(
            new java.awt.Color(180, 180, 180)));
        popupPanel.add(new JScrollPane(completionList));

        completionList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "insert");
        completionList.getActionMap().put("insert", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertSelectedCompletion();
            }
        });

        completionList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    insertSelectedCompletion();
                }
            }
        });

        autoActivationTimer = new Timer(300, e -> show());
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

        // Ctrl+Space
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, java.awt.event.InputEvent.CTRL_DOWN_MASK);
        textComponent.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, "layered-completion");
        textComponent.getActionMap().put("layered-completion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                show();
            }
        });
    }

    public void show() {
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

        popupPanel.setPreferredSize(new Dimension(
            Math.max(250, popupPanel.getPreferredSize().width),
            Math.min(400, Math.max(80, completions.size() * 22 + 5))));
        popupPanel.setSize(popupPanel.getPreferredSize());
        popupPanel.validate();

        JLayeredPane layeredPane = frame.getRootPane().getLayeredPane();
        positionPopup(layeredPane);

        layeredPane.add(popupPanel, JLayeredPane.POPUP_LAYER);
        popupPanel.setVisible(true);
        visible = true;
        completionList.setSelectedIndex(0);
        installDismissListener();
        textComponent.getActionMap().put("insertCompletion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertSelectedCompletion();
            }
        });
    }

    public void hide() {
        if (!visible) { return; }
        visible = false;
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

    private void insertSelectedCompletion() {
        Completion c = completionList.getSelectedValue();
        if (c != null) {
            String entered = provider.getAlreadyEnteredText(textComponent);
            int dot = textComponent.getCaretPosition();
            int start = dot - entered.length();
            textComponent.select(start, dot);
            textComponent.replaceSelection(c.getReplacementText());
        }
        hide();
        textComponent.requestFocusInWindow();
    }
}
