package de.febrildur.sieveeditor.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import de.febrildur.sieveeditor.Application;

/**
 * Action to activate the integrated search panel.
 *
 * <p>The search functionality is now provided by the SearchPanel component
 * which is permanently docked above the RuleNavigatorPanel on the right side.
 * This action simply focuses the search field for immediate use.
 */
public class ActionReplace extends AbstractAction {

	private final Application parentFrame;

	public ActionReplace(Application parentFrame) {
	putValue(NAME, "Find/Replace");
	putValue(ACCELERATOR_KEY, javax.swing.KeyStroke.getKeyStroke(
		java.awt.event.KeyEvent.VK_F, java.awt.event.KeyEvent.CTRL_DOWN_MASK));
	this.parentFrame = parentFrame;
}

	@Override
	public void actionPerformed(ActionEvent e) {
		// Simply focus the integrated search panel
		parentFrame.getSearchPanel().focusSearchField();
	}
}
