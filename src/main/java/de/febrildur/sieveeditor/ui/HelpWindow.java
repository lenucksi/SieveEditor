package de.febrildur.sieveeditor.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class HelpWindow extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(HelpWindow.class.getName());

    private static final String HELP_RESOURCE = "/help/sieve-reference.sieve";
    private static final Pattern SECTION_PATTERN =
        Pattern.compile("^##\\s+SECTION:\\s+(.+)$", Pattern.MULTILINE);

    private final RSyntaxTextArea textArea;
    private final JList<String> sectionList;
    private final DefaultListModel<String> sectionModel;
    private final List<Integer> sectionOffsets = new ArrayList<>();

    public static void showHelp(JFrame parent) {
        for (java.awt.Window w : parent.getOwnedWindows()) {
            if (w instanceof HelpWindow && w.isDisplayable()) {
                w.toFront();
                w.requestFocus();
                return;
            }
        }
        new HelpWindow(parent).setVisible(true);
    }

    private HelpWindow(JFrame parent) {
        super(parent, "Sieve Editor Help - Reference & Examples", false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 720);
        setMinimumSize(new Dimension(700, 400));
        setLocationRelativeTo(parent);

        sectionModel = new DefaultListModel<>();
        sectionList = new JList<>(sectionModel);
        sectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sectionList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        sectionList.addListSelectionListener(this::onSectionSelected);

        textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle("text/sieve");
        textArea.setCodeFoldingEnabled(true);
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        textArea.setHighlightCurrentLine(false);
        textArea.setLineWrap(false);
        textArea.setTabSize(4);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.getGutter().setFoldIndicatorStyle(
            org.fife.ui.rtextarea.FoldIndicatorStyle.MODERN);

        JScrollPane sectionScroll = new JScrollPane(sectionList);
        sectionScroll.setMinimumSize(new Dimension(180, 0));
        sectionScroll.setPreferredSize(new Dimension(220, 0));

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, sectionScroll, scrollPane);
        splitPane.setResizeWeight(0.0);
        splitPane.setDividerLocation(220);

        JPanel content = new JPanel(new BorderLayout());
        content.add(splitPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);

        closeButton.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_ESCAPE, 0), "close");
        closeButton.getActionMap().put("close", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });

        content.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(content);

        loadHelpContent();
    }

    private void loadHelpContent() {
        try (var is = getClass().getResourceAsStream(HELP_RESOURCE)) {
            if (is == null) {
                textArea.setText("// Help resource not found: " + HELP_RESOURCE);
                return;
            }
            StringBuilder sb = new StringBuilder(16 * 1024);
            try (var reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            String content = sb.toString();
            textArea.setText(content);
            textArea.setCaretPosition(0);

            buildSectionIndex(content);
            LOGGER.fine("Loaded help resource (" + content.length() + " chars)");
        } catch (IOException e) {
            LOGGER.warning("Failed to load help resource: " + e.getMessage());
            textArea.setText("// Error loading help content:\n// " + e.getMessage());
        }
    }

    private void buildSectionIndex(String content) {
        sectionModel.clear();
        sectionOffsets.clear();
        Matcher matcher = SECTION_PATTERN.matcher(content);
        while (matcher.find()) {
            sectionModel.addElement(matcher.group(1));
            sectionOffsets.add(matcher.start());
        }
        if (sectionModel.size() > 0) {
            sectionList.setSelectedIndex(0);
        }
    }

    private void onSectionSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) { return; }
        int idx = sectionList.getSelectedIndex();
        if (idx < 0 || idx >= sectionOffsets.size()) { return; }
        int offset = sectionOffsets.get(idx);
        textArea.setCaretPosition(offset);
        SwingUtilities.invokeLater(() -> {
            try {
                var r = textArea.modelToView(offset);
                if (r != null) {
                    textArea.scrollRectToVisible(r);
                }
            } catch (javax.swing.text.BadLocationException ex) {
                // ignore
            }
        });
    }
}
