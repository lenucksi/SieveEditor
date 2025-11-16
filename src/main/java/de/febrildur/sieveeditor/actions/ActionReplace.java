package de.febrildur.sieveeditor.actions;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import de.febrildur.sieveeditor.Application;

public class ActionReplace extends AbstractAction {

	private JTextField searchField;
	private JCheckBox regexCB;
	private JCheckBox matchCaseCB;
	private Application parentFrame;

	public ActionReplace(Application parentFrame) {
		putValue("Name", "Find/Replace");
		this.parentFrame = parentFrame;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final JDialog frame = new JDialog(parentFrame, "Find", true);

		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		GridLayout layout = new GridLayout(4, 2, 6, 6);
		panel.setLayout(layout);

		// Search field
		panel.add(new JLabel("Find:"));
		searchField = new JTextField(30);
		searchField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performSearch(true, frame);
			}
		});
		panel.add(searchField);

		// Find Next button
		panel.add(new JLabel("")); // Empty label for spacing
		final JButton nextButton = new JButton("Find Next");
		nextButton.setActionCommand("FindNext");
		nextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performSearch(true, frame);
			}
		});
		panel.add(nextButton);

		// Find Previous button
		panel.add(new JLabel("")); // Empty label for spacing
		JButton prevButton = new JButton("Find Previous");
		prevButton.setActionCommand("FindPrev");
		prevButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performSearch(false, frame);
			}
		});
		panel.add(prevButton);

		// Options
		regexCB = new JCheckBox("Regex");
		panel.add(regexCB);
		matchCaseCB = new JCheckBox("Match Case");
		panel.add(matchCaseCB);

		frame.getContentPane().add(panel);
		frame.pack(); // Auto-size instead of fixed 300x200
		frame.setLocationRelativeTo(parentFrame);
		frame.setVisible(true);
	}

	private void performSearch(boolean forward, JDialog dialog) {
		// Create an object defining our search parameters.
		SearchContext context = new SearchContext();
		String text = searchField.getText();
		if (text.length() == 0) {
			JOptionPane.showMessageDialog(dialog,
				"Please enter text to search for",
				"Empty Search", JOptionPane.WARNING_MESSAGE);
			return;
		}
		context.setSearchFor(text);
		context.setMatchCase(matchCaseCB.isSelected());
		context.setRegularExpression(regexCB.isSelected());
		context.setSearchForward(forward);
		context.setWholeWord(false);
		context.setSearchWrap(true);

		boolean found = SearchEngine.find(parentFrame.getScriptArea(), context).wasFound();
		if (!found) {
			JOptionPane.showMessageDialog(dialog, "Text not found");
		}
	}
}
