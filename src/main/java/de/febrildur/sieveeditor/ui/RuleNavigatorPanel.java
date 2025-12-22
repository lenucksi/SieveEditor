package de.febrildur.sieveeditor.ui;

import de.febrildur.sieveeditor.parser.SieveRule;
import de.febrildur.sieveeditor.parser.SieveRuleParser;
import de.febrildur.sieveeditor.parser.SieveWarning;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Navigation panel for Sieve script rules.
 *
 * <p>Displays a list of rules extracted from the script with their line numbers.
 * Clicking a rule jumps to that line in the editor.
 */
public class RuleNavigatorPanel extends JPanel {

	private final DefaultListModel<SieveRule> listModel;
	private final JList<SieveRule> ruleList;
	private final DefaultListModel<SieveWarning> warningListModel;
	private final JList<SieveWarning> warningList;
	private Consumer<Integer> jumpToLineCallback;
	private SieveWarning lastClickedWarning = null;

	public RuleNavigatorPanel() {
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder(
			BorderFactory.createEtchedBorder(),
			"Script Rules",
			TitledBorder.DEFAULT_JUSTIFICATION,
			TitledBorder.DEFAULT_POSITION
		));

		// Create warnings list FIRST (needed by ruleList listener)
		warningListModel = new DefaultListModel<>();
		warningList = new JList<>(warningListModel);
		warningList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		warningList.setFont(warningList.getFont().deriveFont(Font.ITALIC));

		// Custom cell renderer for color-coded warnings
		warningList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value,
														  int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof SieveWarning warning) {
					setText(warning.getDisplayText());

					// Color-code by severity (only when not selected)
					if (!isSelected) {
						if (warning.getSeverity() == SieveWarning.Severity.ERROR) {
							setForeground(new Color(139, 0, 0)); // Dark red
						} else {
							setForeground(new Color(255, 140, 0)); // Dark orange
						}
					}

					// Tooltip shows full message
					setToolTipText(warning.getMessage());
				}
				return this;
			}
		});

		// Create list model and list for rules
		listModel = new DefaultListModel<>();
		ruleList = new JList<>(listModel);
		ruleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Custom cell renderer to show rule display text
		ruleList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value,
														  int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof SieveRule rule) {
					setText(rule.getDisplayText());
					setToolTipText("Line " + rule.getLineNumber() + ": " + rule.getComment());
				}
				return this;
			}
		});

		// Add click listener for rules
		ruleList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && jumpToLineCallback != null) {
				SieveRule selected = ruleList.getSelectedValue();
				if (selected != null) {
					warningList.clearSelection(); // Clear warning selection
					jumpToLineCallback.accept(selected.getLineNumber());
				}
			}
		});

		// Scroll pane for rules list
		JScrollPane scrollPane = new JScrollPane(ruleList);
		scrollPane.setPreferredSize(new Dimension(200, 200));
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		// Add click listener for warnings - supports cycling through duplicates
		warningList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && jumpToLineCallback != null) {
				SieveWarning selected = warningList.getSelectedValue();
				if (selected != null) {
					ruleList.clearSelection(); // Clear rule selection

					// Check if this is a repeated click on the same warning
					if (selected == lastClickedWarning && selected.hasLineNumbers()) {
						// Cycle to next occurrence
						selected.cycleToNextLine();
					} else {
						// First click or different warning - reset cycle
						if (lastClickedWarning != null) {
							lastClickedWarning.resetCycle();
						}
						lastClickedWarning = selected;
					}

					// Jump to the current line number
					Integer lineNumber = selected.getCurrentLineNumber();
					if (lineNumber != null) {
						jumpToLineCallback.accept(lineNumber);
					}
				}
			}
		});

		// Scroll pane for warnings list - smaller, only shows when warnings exist
		JScrollPane warningScrollPane = new JScrollPane(warningList);
		warningScrollPane.setPreferredSize(new Dimension(200, 60));
		warningScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		warningScrollPane.setVisible(false);

		// Layout: rules at top, warnings at bottom
		add(scrollPane, BorderLayout.CENTER);
		add(warningScrollPane, BorderLayout.SOUTH);
	}

	/**
	 * Sets the callback to invoke when a rule is selected.
	 *
	 * @param callback function that receives the line number to jump to
	 */
	public void setJumpToLineCallback(Consumer<Integer> callback) {
		this.jumpToLineCallback = callback;
	}

	/**
	 * Updates the navigator with rules from the given script.
	 *
	 * @param scriptText the Sieve script text to parse
	 */
	public void updateRules(String scriptText) {
		listModel.clear();
		warningListModel.clear();

		// Get parent scroll pane for warnings
		JScrollPane warningScrollPane = (JScrollPane) warningList.getParent().getParent();
		warningScrollPane.setVisible(false);

		if (scriptText == null || scriptText.trim().isEmpty()) {
			return;
		}

		SieveRuleParser.ParseResult result = SieveRuleParser.parseRules(scriptText);

		// Add rules to list
		for (SieveRule rule : result.getRules()) {
			listModel.addElement(rule);
		}

		// Show warnings if any
		if (result.hasWarnings()) {
			for (SieveWarning warning : result.getWarnings()) {
				warningListModel.addElement(warning);
			}
			warningScrollPane.setVisible(true);
		}

		// Show count in title
		updateTitle(result.getRules().size());
	}

	/**
	 * Updates the panel title with rule count.
	 */
	private void updateTitle(int count) {
		TitledBorder border = (TitledBorder) getBorder();
		if (count == 0) {
			border.setTitle("Script Rules");
		} else {
			border.setTitle("Script Rules (" + count + ")");
		}
		repaint();
	}

	/**
	 * Clears all rules from the navigator.
	 */
	public void clear() {
		listModel.clear();
		warningListModel.clear();
		JScrollPane warningScrollPane = (JScrollPane) warningList.getParent().getParent();
		warningScrollPane.setVisible(false);
		updateTitle(0);
	}
}
