package de.febrildur.sieveeditor.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Integrated search and replace panel for finding/replacing text in the editor.
 *
 * <p>Designed to be docked above the RuleNavigatorPanel as part of the right-side UI.
 * Provides a compact, always-available search and replace interface without blocking the editor.
 *
 * <p>Keyboard shortcuts:
 * <ul>
 *   <li>F3 - Find Next</li>
 *   <li>Shift+F3 - Find Previous</li>
 *   <li>Ctrl+F - Focus search field</li>
 * </ul>
 */
public class SearchPanel extends JPanel {

	private final JTextField searchField;
	private final JTextField replaceField;
	private final JCheckBox regexCB;
	private final JCheckBox matchCaseCB;
	private RSyntaxTextArea targetEditor;

	public SearchPanel() {
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder(
			BorderFactory.createEtchedBorder(),
			"Find & Replace",
			TitledBorder.DEFAULT_JUSTIFICATION,
			TitledBorder.DEFAULT_POSITION
		));

		// Main panel with compact vertical layout
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		// Search field row
		JPanel searchRow = new JPanel(new BorderLayout(4, 0));
		searchRow.add(new JLabel("Find:"), BorderLayout.WEST);
		searchField = new JTextField();
		searchField.setToolTipText("Text to search for (Ctrl+F to focus, Enter to find next)");
		searchField.addActionListener(e -> performSearch(true));
		searchRow.add(searchField, BorderLayout.CENTER);
		searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, searchField.getPreferredSize().height));
		contentPanel.add(searchRow);
		contentPanel.add(Box.createVerticalStrut(3));

		// Replace field row
		JPanel replaceRow = new JPanel(new BorderLayout(4, 0));
		replaceRow.add(new JLabel("With:"), BorderLayout.WEST);
		replaceField = new JTextField();
		replaceField.setToolTipText("Replacement text (Enter to replace current match)");
		replaceField.addActionListener(e -> performReplace());
		replaceRow.add(replaceField, BorderLayout.CENTER);
		replaceRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, replaceField.getPreferredSize().height));
		contentPanel.add(replaceRow);
		contentPanel.add(Box.createVerticalStrut(3));

		// Find buttons row
		JPanel findButtonRow = new JPanel(new GridLayout(1, 2, 4, 0));
		JButton nextButton = new JButton("Next");
		nextButton.setToolTipText("Find next occurrence (F3)");
		nextButton.addActionListener(e -> performSearch(true));
		findButtonRow.add(nextButton);

		JButton prevButton = new JButton("Previous");
		prevButton.setToolTipText("Find previous occurrence (Shift+F3)");
		prevButton.addActionListener(e -> performSearch(false));
		findButtonRow.add(prevButton);
		findButtonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, nextButton.getPreferredSize().height));
		contentPanel.add(findButtonRow);
		contentPanel.add(Box.createVerticalStrut(3));

		// Replace buttons row
		JPanel replaceButtonRow = new JPanel(new GridLayout(1, 2, 4, 0));
		JButton replaceButton = new JButton("Replace");
		replaceButton.setToolTipText("Replace current match and find next");
		replaceButton.addActionListener(e -> performReplace());
		replaceButtonRow.add(replaceButton);

		JButton replaceAllButton = new JButton("Replace All");
		replaceAllButton.setToolTipText("Replace all occurrences in document");
		replaceAllButton.addActionListener(e -> performReplaceAll());
		replaceButtonRow.add(replaceAllButton);
		replaceButtonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, replaceButton.getPreferredSize().height));
		contentPanel.add(replaceButtonRow);
		contentPanel.add(Box.createVerticalStrut(3));

		// Options row
		JPanel optionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		regexCB = new JCheckBox("Regex");
		matchCaseCB = new JCheckBox("Match Case");
		optionsRow.add(regexCB);
		optionsRow.add(Box.createHorizontalStrut(8));
		optionsRow.add(matchCaseCB);
		optionsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, regexCB.getPreferredSize().height));
		contentPanel.add(optionsRow);

		add(contentPanel, BorderLayout.NORTH);
	}

	/**
	 * Sets the editor that this search panel will search within.
	 * Also registers keyboard shortcuts for find next/previous.
	 */
	public void setTargetEditor(RSyntaxTextArea editor) {
		this.targetEditor = editor;

		// Register F3 for Find Next
		editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "findNext");
		editor.getActionMap().put("findNext", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				performSearch(true);
			}
		});

		// Register Shift+F3 for Find Previous
		editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			KeyStroke.getKeyStroke(KeyEvent.VK_F3, KeyEvent.SHIFT_DOWN_MASK), "findPrevious");
		editor.getActionMap().put("findPrevious", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				performSearch(false);
			}
		});
	}

	/**
	 * Focuses the search field for immediate typing.
	 */
	public void focusSearchField() {
		searchField.requestFocusInWindow();
		searchField.selectAll(); // Select existing text for easy replacement
	}

	/**
	 * Performs a search in the target editor.
	 *
	 * @param forward true to search forward, false to search backward
	 */
	private void performSearch(boolean forward) {
		if (targetEditor == null) {
			return;
		}

		String text = searchField.getText();
		if (text.isEmpty()) {
			JOptionPane.showMessageDialog(this,
				"Please enter text to search for",
				"Empty Search", JOptionPane.WARNING_MESSAGE);
			return;
		}

		SearchContext context = new SearchContext();
		context.setSearchFor(text);
		context.setMatchCase(matchCaseCB.isSelected());
		context.setRegularExpression(regexCB.isSelected());
		context.setSearchForward(forward);
		context.setWholeWord(false);
		context.setSearchWrap(true);

		boolean found = SearchEngine.find(targetEditor, context).wasFound();
		if (!found) {
			JOptionPane.showMessageDialog(this, "Text not found");
		} else {
			// Transfer focus to editor for immediate editing
			targetEditor.requestFocusInWindow();
		}
	}

	/**
	 * Replaces the current selection with the replace text and finds the next occurrence.
	 */
	private void performReplace() {
		if (targetEditor == null) {
			return;
		}

		String searchText = searchField.getText();
		String replaceText = replaceField.getText();

		if (searchText.isEmpty()) {
			JOptionPane.showMessageDialog(this,
				"Please enter text to search for",
				"Empty Search", JOptionPane.WARNING_MESSAGE);
			return;
		}

		SearchContext context = new SearchContext();
		context.setSearchFor(searchText);
		context.setReplaceWith(replaceText);
		context.setMatchCase(matchCaseCB.isSelected());
		context.setRegularExpression(regexCB.isSelected());
		context.setSearchForward(true);
		context.setWholeWord(false);
		context.setSearchWrap(true);

		boolean replaced = SearchEngine.replace(targetEditor, context).wasFound();
		if (!replaced) {
			JOptionPane.showMessageDialog(this, "Text not found");
		} else {
			// Find next occurrence after replacing
			SearchEngine.find(targetEditor, context);
			targetEditor.requestFocusInWindow();
		}
	}

	/**
	 * Replaces all occurrences of the search text with the replace text.
	 */
	private void performReplaceAll() {
		if (targetEditor == null) {
			return;
		}

		String searchText = searchField.getText();
		String replaceText = replaceField.getText();

		if (searchText.isEmpty()) {
			JOptionPane.showMessageDialog(this,
				"Please enter text to search for",
				"Empty Search", JOptionPane.WARNING_MESSAGE);
			return;
		}

		SearchContext context = new SearchContext();
		context.setSearchFor(searchText);
		context.setReplaceWith(replaceText);
		context.setMatchCase(matchCaseCB.isSelected());
		context.setRegularExpression(regexCB.isSelected());
		context.setSearchForward(true);
		context.setWholeWord(false);
		context.setSearchWrap(true);

		int count = SearchEngine.replaceAll(targetEditor, context).getCount();
		JOptionPane.showMessageDialog(this,
			"Replaced " + count + " occurrence" + (count == 1 ? "" : "s"));

		targetEditor.requestFocusInWindow();
	}
}
