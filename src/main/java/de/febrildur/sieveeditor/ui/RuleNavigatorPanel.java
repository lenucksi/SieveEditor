package de.febrildur.sieveeditor.ui;

import de.febrildur.sieveeditor.parser.SieveRule;
import de.febrildur.sieveeditor.parser.SieveRuleParser;

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
	private final JLabel warningLabel;
	private Consumer<Integer> jumpToLineCallback;

	public RuleNavigatorPanel() {
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder(
			BorderFactory.createEtchedBorder(),
			"Script Rules",
			TitledBorder.DEFAULT_JUSTIFICATION,
			TitledBorder.DEFAULT_POSITION
		));

		// Create list model and list
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

		// Add click listener
		ruleList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && jumpToLineCallback != null) {
				SieveRule selected = ruleList.getSelectedValue();
				if (selected != null) {
					jumpToLineCallback.accept(selected.getLineNumber());
				}
			}
		});

		// Scroll pane for list - reduced width to 200px, allow horizontal scroll
		JScrollPane scrollPane = new JScrollPane(ruleList);
		scrollPane.setPreferredSize(new Dimension(200, 200));
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		// Warning label for numbering issues - changed to bright red for errors
		warningLabel = new JLabel();
		warningLabel.setForeground(new Color(220, 20, 60)); // Crimson red - more visible than orange
		warningLabel.setFont(warningLabel.getFont().deriveFont(Font.ITALIC));
		warningLabel.setVisible(false);

		// Layout
		add(scrollPane, BorderLayout.CENTER);
		add(warningLabel, BorderLayout.SOUTH);
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
		warningLabel.setVisible(false);

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
			String warningText = String.join(", ", result.getWarnings());
			if (warningText.length() > 50) {
				warningText = warningText.substring(0, 47) + "...";
			}
			warningLabel.setText("⚠ " + warningText);
			warningLabel.setToolTipText(String.join("\n", result.getWarnings()));
			warningLabel.setVisible(true);
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
		warningLabel.setVisible(false);
		updateTitle(0);
	}
}
