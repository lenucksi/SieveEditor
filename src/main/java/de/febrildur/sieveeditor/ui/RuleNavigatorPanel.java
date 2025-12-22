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
	private final JSplitPane splitPane;
	private final JScrollPane warningScrollPane;
	private Consumer<Integer> jumpToLineCallback;
	private SieveWarning lastClickedWarning = null;
	private boolean hasAutoSizedWidth = false; // Track if we've auto-sized the width already

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

		// Add selection listener for warnings - handles first selection
		warningList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && jumpToLineCallback != null) {
				SieveWarning selected = warningList.getSelectedValue();
				if (selected != null && selected != lastClickedWarning) {
					ruleList.clearSelection(); // Clear rule selection

					// First click on this warning - reset cycle
					if (lastClickedWarning != null) {
						lastClickedWarning.resetCycle();
					}
					lastClickedWarning = selected;

					// Jump to the current line number
					Integer lineNumber = selected.getCurrentLineNumber();
					if (lineNumber != null) {
						jumpToLineCallback.accept(lineNumber);
					}
				}
			}
		});

		// Add mouse listener to detect repeated clicks on same warning (for cycling)
		warningList.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				// Filter out horizontal scroll buttons to prevent exceptions
				if (e.getButton() > 3) {
					return;
				}

				if (jumpToLineCallback != null) {
					int index = warningList.locationToIndex(e.getPoint());
					if (index >= 0) {
						SieveWarning selected = warningList.getModel().getElementAt(index);
						if (selected == lastClickedWarning && selected.hasLineNumbers()) {
							// Repeated click - cycle to next occurrence
							ruleList.clearSelection();
							selected.cycleToNextLine();
							Integer lineNumber = selected.getCurrentLineNumber();
							if (lineNumber != null) {
								jumpToLineCallback.accept(lineNumber);
							}
						}
					}
				}
			}
		});

		// Scroll pane for warnings list - smaller, only shows when warnings exist
		warningScrollPane = new JScrollPane(warningList);
		warningScrollPane.setPreferredSize(new Dimension(200, 60));
		warningScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		warningScrollPane.setVisible(false);

		// Use JSplitPane to make warning panel resizable
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, warningScrollPane);
		splitPane.setResizeWeight(1.0); // Give all extra space to rules list
		splitPane.setOneTouchExpandable(false);
		splitPane.setBorder(null); // Remove default border

		// Initially position divider to hide warnings (they'll auto-size when shown)
		splitPane.setDividerLocation(1.0);

		// Layout: use splitPane instead of BorderLayout
		add(splitPane, BorderLayout.CENTER);
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
		warningScrollPane.setVisible(false);

		if (scriptText == null || scriptText.trim().isEmpty()) {
			// Hide warnings when script is empty
			splitPane.setDividerLocation(1.0);
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

			// Auto-size warning panel based on number of warnings
			// Use SwingUtilities.invokeLater to ensure layout is complete
			SwingUtilities.invokeLater(() -> autoSizeWarningPanel(result.getWarnings().size()));
		} else {
			// Hide warnings panel by moving divider to bottom
			splitPane.setDividerLocation(1.0);
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
	 * Auto-sizes the warning panel to fit the given number of warnings.
	 * Calculates optimal height to show 3-4 warnings without scrolling.
	 *
	 * @param warningCount number of warnings to display
	 */
	private void autoSizeWarningPanel(int warningCount) {
		if (warningCount == 0 || splitPane.getHeight() == 0) {
			return;
		}

		// Calculate item height (includes padding/borders)
		int itemHeight = warningList.getFixedCellHeight();
		if (itemHeight <= 0) {
			// Estimate based on font metrics if not set
			itemHeight = warningList.getFontMetrics(warningList.getFont()).getHeight() + 4;
		}

		// Calculate desired height for warnings (show up to 4 warnings, min 2)
		int visibleWarnings = Math.min(4, Math.max(2, warningCount));
		int desiredHeight = visibleWarnings * itemHeight + 10; // +10 for borders/padding

		// Calculate divider location (split pane height minus warning height minus divider size)
		int dividerLocation = splitPane.getHeight() - desiredHeight - splitPane.getDividerSize();

		// Ensure divider is within valid range (leave room for rules list)
		int minRulesHeight = 100; // Minimum space for rules list
		dividerLocation = Math.max(minRulesHeight, Math.min(dividerLocation, splitPane.getHeight() - 50));

		splitPane.setDividerLocation(dividerLocation);
	}

	/**
	 * Calculates the recommended width for the navigator based on rule text lengths.
	 * Uses the longest rule text to prevent horizontal scrolling.
	 *
	 * @return recommended width in pixels (150-400 range)
	 */
	public int getRecommendedWidth() {
		if (listModel.isEmpty()) {
			return 200; // Default width when no rules
		}

		// Find the longest rule display text
		int longestTextWidth = 0;
		FontMetrics fm = ruleList.getFontMetrics(ruleList.getFont());

		for (int i = 0; i < listModel.size(); i++) {
			SieveRule rule = listModel.getElementAt(i);
			String displayText = rule.getDisplayText();
			int textWidth = fm.stringWidth(displayText);
			longestTextWidth = Math.max(longestTextWidth, textWidth);
		}

		// Add padding for borders and small margin (no scrollbar needed)
		int recommendedWidth = longestTextWidth + 20; // +20px for borders and margin

		// Clamp to reasonable bounds
		int minWidth = 150;
		int maxWidth = 400;
		return Math.max(minWidth, Math.min(maxWidth, recommendedWidth));
	}

	/**
	 * Checks if the width has already been auto-sized.
	 *
	 * @return true if width was already auto-sized, false otherwise
	 */
	public boolean isWidthAutoSized() {
		return hasAutoSizedWidth;
	}

	/**
	 * Marks that the width has been auto-sized.
	 */
	public void markWidthAutoSized() {
		this.hasAutoSizedWidth = true;
	}

	/**
	 * Reapplies the warning panel auto-sizing.
	 * Called when the window is resized to adjust the warning panel height.
	 */
	public void reapplyWarningPanelSize() {
		if (warningScrollPane.isVisible() && warningListModel.getSize() > 0) {
			SwingUtilities.invokeLater(() -> autoSizeWarningPanel(warningListModel.getSize()));
		}
	}

	/**
	 * Clears all rules from the navigator.
	 */
	public void clear() {
		listModel.clear();
		warningListModel.clear();
		warningScrollPane.setVisible(false);
		splitPane.setDividerLocation(1.0);
		updateTitle(0);
	}
}
