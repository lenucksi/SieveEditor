package de.febrildur.sieveeditor.ui;
// SPDX-FileCopyrightText: 2025 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

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

		warningListModel = new DefaultListModel<>();
		warningList = new JList<>(warningListModel);
		warningList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		warningList.setFont(warningList.getFont().deriveFont(Font.ITALIC));
		warningList.setCellRenderer(new WarningCellRenderer());

		listModel = new DefaultListModel<>();
		ruleList = new JList<>(listModel);
		ruleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ruleList.setCellRenderer(new RuleCellRenderer());

		ruleList.addListSelectionListener(this::handleRuleSelection);
		warningList.addListSelectionListener(this::handleWarningSelection);
		warningList.addMouseListener(new WarningMouseAdapter());

		JScrollPane scrollPane = new JScrollPane(ruleList);
		scrollPane.setPreferredSize(new Dimension(200, 200));
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		warningScrollPane = new JScrollPane(warningList);
		warningScrollPane.setPreferredSize(new Dimension(200, 60));
		warningScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		warningScrollPane.setVisible(false);

		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, warningScrollPane);
		splitPane.setResizeWeight(1.0);
		splitPane.setOneTouchExpandable(false);
		splitPane.setBorder(null);
		splitPane.setDividerLocation(1.0);

		add(splitPane, BorderLayout.CENTER);
	}

	private void handleRuleSelection(javax.swing.event.ListSelectionEvent e) {
		if (!e.getValueIsAdjusting() && jumpToLineCallback != null) {
			SieveRule selected = ruleList.getSelectedValue();
			if (selected != null) {
				warningList.clearSelection();
				jumpToLineCallback.accept(selected.getLineNumber());
			}
		}
	}

	private void handleWarningSelection(javax.swing.event.ListSelectionEvent e) {
		if (!e.getValueIsAdjusting() && jumpToLineCallback != null) {
			SieveWarning selected = warningList.getSelectedValue();
			if (selected != null && selected != lastClickedWarning) {
				ruleList.clearSelection();
				if (lastClickedWarning != null) {
					lastClickedWarning.resetCycle();
				}
				lastClickedWarning = selected;
				Integer lineNumber = selected.getCurrentLineNumber();
				if (lineNumber != null) {
					jumpToLineCallback.accept(lineNumber);
				}
			}
		}
	}

	private class WarningMouseAdapter extends java.awt.event.MouseAdapter {
		@Override
		public void mouseClicked(java.awt.event.MouseEvent e) {
			if (e.getButton() > 3) {
				return;
			}
			if (jumpToLineCallback != null) {
				int index = warningList.locationToIndex(e.getPoint());
				if (index >= 0) {
					SieveWarning selected = warningList.getModel().getElementAt(index);
					if (selected == lastClickedWarning && selected.hasLineNumbers()) {
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
	}

	private static class WarningCellRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value,
													  int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof SieveWarning warning) {
				setText(warning.getDisplayText());
				if (!isSelected) {
					if (warning.getSeverity() == SieveWarning.Severity.ERROR) {
						setForeground(new Color(139, 0, 0));
					} else {
						setForeground(new Color(255, 140, 0));
					}
				}
				setToolTipText(warning.getMessage());
			}
			return this;
		}
	}

	private static class RuleCellRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value,
													  int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof SieveRule rule) {
				setText(rule.getDisplayText());
				StringBuilder tooltip = new StringBuilder("<html>");
				tooltip.append(rule.getComment()).append("<br><br>");
				tooltip.append("<b>UniqueId:</b> ").append(rule.getRuleNumber()).append("<br>");
				tooltip.append("<b>Rulename:</b> ").append(rule.getLabel().isEmpty() ? "(empty)" : rule.getLabel()).append("<br>");
				if (rule.getFlag() != null && !rule.getFlag().trim().isEmpty()) {
					tooltip.append("<b>Flag:</b> ").append(rule.getFlag()).append("<br>");
				}
				if (rule.getLastModified() != null) {
					tooltip.append("<b>Last Modified:</b> ").append(rule.getLastModified()).append("<br>");
				}
				if (rule.getModifiedBy() != null) {
					tooltip.append("<b>Modified By:</b> ").append(rule.getModifiedBy());
				}
				tooltip.append("</html>");
				setToolTipText(tooltip.toString());
			}
			return this;
		}
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
