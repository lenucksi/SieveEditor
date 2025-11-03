package de.febrildur.sieveeditor.actions;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
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
		putValue("Name", "Save");
		this.parentFrame = parentFrame;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		GridLayout layout = new GridLayout(5, 2, 6, 6);
		panel.setLayout(layout);

		final JDialog frame = new JDialog(parentFrame, "Connection", true);
		frame.getContentPane().add(panel);
		frame.setSize(300, 200);
		frame.setLocationRelativeTo(parentFrame);

		// Create a toolbar with searching options.
		searchField = new JTextField(30);
		frame.add(searchField);
		final JButton nextButton = new JButton("Find Next");
		nextButton.setActionCommand("FindNext");
		nextButton.addActionListener(this);
		frame.add(nextButton);
		searchField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// "FindNext" => search forward, "FindPrev" => search backward
				String command = e.getActionCommand();
				boolean forward = "FindNext".equals(command);

				// Create an object defining our search parameters.
				SearchContext context = new SearchContext();
				String text = searchField.getText();
				if (text.length() == 0) {
					return;
				}
				context.setSearchFor(text);
				context.setMatchCase(matchCaseCB.isSelected());
				context.setRegularExpression(regexCB.isSelected());
				context.setSearchForward(forward);
				context.setWholeWord(false);

				boolean found = SearchEngine.find(parentFrame.getScriptArea(), context).wasFound();
				if (!found) {
					JOptionPane.showMessageDialog(frame, "Text not found");
				}
			}
		});
		JButton prevButton = new JButton("Find Previous");
		prevButton.setActionCommand("FindPrev");
		prevButton.addActionListener(this);
		frame.add(prevButton);
		regexCB = new JCheckBox("Regex");
		frame.add(regexCB);
		matchCaseCB = new JCheckBox("Match Case");
		frame.add(matchCaseCB);
		
		frame.setVisible(true);
	}
}
