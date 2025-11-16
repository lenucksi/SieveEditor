package de.febrildur.sieveeditor.actions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test suite for ActionActivateDeactivateScript class.
 *
 * This class has known bugs that should have regression tests:
 * - BUG-001: ArrayIndexOutOfBoundsException when no row selected (line 57)
 * - BUG-002: Incorrect message after rename operation (line 91)
 *
 * Note: Cannot test without refactoring for dependency injection.
 */
class ActionActivateDeactivateScriptTest {

    @Test
    void shouldExist() {
        // When/Then
        assertThat(ActionActivateDeactivateScript.class).isNotNull();
    }

    /**
     * BUG REGRESSION TEST (requires refactoring):
     *
     * @Test
     * void shouldHandleNoRowSelected() {
     *     // Given
     *     TestApplication app = new TestApplication();
     *     ActionActivateDeactivateScript action = new ActionActivateDeactivateScript(app);
     *
     *     JTable table = new JTable();
     *     table.clearSelection(); // Ensures getSelectedRow() returns -1
     *
     *     // When/Then - Should not throw ArrayIndexOutOfBoundsException
     *     assertThatCode(() ->
     *         action.actionPerformed(new ActionEvent(table, 0, "activate")))
     *         .doesNotThrowAnyException();
     *
     *     // Should show error message
     *     assertThat(app.getLastMessage()).contains("select a script");
     * }
     */

    /**
     * BUG REGRESSION TEST (requires refactoring):
     *
     * @Test
     * void shouldShowCorrectMessageAfterRename() {
     *     // Given - Connected with script selected
     *     TestApplication app = createConnectedApp();
     *     ActionActivateDeactivateScript action = new ActionActivateDeactivateScript(app);
     *
     *     // When - Rename script
     *     action.renameScript("oldname", "newname");
     *
     *     // Then - Should show rename success message, not activate message
     *     assertThat(app.getLastMessage())
     *         .contains("renamed")
     *         .doesNotContain("activated");
     * }
     */

    /**
     * TESTABILITY ISSUE:
     *
     * Current code at line 57:
     *     String selected = data[table.getSelectedRow()][0];
     *
     * Problem: If no row selected, getSelectedRow() returns -1
     * Result: ArrayIndexOutOfBoundsException
     *
     * Fix needed:
     *     int selectedRow = table.getSelectedRow();
     *     if (selectedRow < 0) {
     *         JOptionPane.showMessageDialog(frame, "Please select a script");
     *         return;
     *     }
     *     String selected = data[selectedRow][0];
     *
     * After fix, add regression test to ensure it stays fixed.
     */
}
