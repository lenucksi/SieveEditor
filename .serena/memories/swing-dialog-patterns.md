# Swing Dialog Patterns in SieveEditor

## Dialog Creation Pattern

Most dialogs in SieveEditor follow this pattern:

```java
final JDialog frame = new JDialog(parentFrame, "Title", true);
```

The third parameter (`true`) makes the dialog **modal**, blocking interaction with the parent window.

## Current Dialog Implementations

### ActionReplace.java (Find/Replace Dialog)

- **Location**: `src/main/java/de/febrildur/sieveeditor/actions/ActionReplace.java`
- **Pattern**: Modal dialog (`true` parameter)
- **Issue**: Blocks editor interaction while searching
- **Components**:
  - Search field with Enter key support
  - Find Next/Previous buttons
  - Regex and Match Case checkboxes
  - Uses RSyntaxTextArea's `SearchEngine.find()` API
- **Layout**: GridLayout(4, 2) with 6px gaps

### Other Modal Dialogs

- **ActionConnect.java**: Connection dialog (modal)
- **ActionLoadScript.java**: Script selection dialog (modal)
- **ActionActivateDeactivateScript.java**: Manage scripts dialog (modal)
- **CertificateDialog.java**: Certificate trust dialog (modal - appropriate)

## Making Dialogs Modeless

To convert from modal to modeless:

```java
// Before (modal):
final JDialog frame = new JDialog(parentFrame, "Title", true);

// After (modeless):
final JDialog frame = new JDialog(parentFrame, "Title", false);
```

### Considerations for Modeless Dialogs

1. **State Management**: Dialog can remain open while user edits
2. **Focus Handling**: Need proper window focus behavior
3. **Disposal**: Should close when parent closes
4. **Positioning**: Consider fixed position (top-right) vs centered
5. **Singleton Pattern**: Only one instance should exist at a time

## Implementation: Modeless Find Dialog (Task #1)

**File**: `src/main/java/de/febrildur/sieveeditor/actions/ActionReplace.java`

**Changes Made**:

1. **Changed dialog to modeless** (line 43):

   ```java
   final JDialog frame = new JDialog(parentFrame, "Find", false); // Modeless
   ```

2. **Added singleton pattern** to prevent multiple dialogs:

   ```java
   private JDialog existingDialog; // Field to track instance

   // In actionPerformed():
   if (existingDialog != null && existingDialog.isVisible()) {
       existingDialog.toFront();
       searchField.requestFocusInWindow();
       return;
   }
   ```

3. **Added top-right positioning** (like VS Code/IntelliJ):

   ```java
   private void positionDialogTopRight(JDialog dialog) {
       int x = parentX + parentWidth - dialogWidth - 20;
       int y = parentY + 60; // Below menu bar
       dialog.setLocation(x, y);
   }
   ```

4. **Added cleanup on close**:

   ```java
   frame.addWindowListener(new WindowAdapter() {
       @Override
       public void windowClosing(WindowEvent windowEvent) {
           existingDialog = null;
       }
   });
   ```

**Refinements After User Testing**:

1. **Reduced width** (line 53):
   - Changed from `new JTextField(30)` to `new JTextField(20)`
   - Saves screen space without compromising usability

2. **Improved positioning** (lines 140-146):
   - Accounts for 200px RuleNavigatorPanel on the right
   - Prevents overlap with split pane UI elements
   - Position: `parentWidth - dialogWidth - navigatorWidth - 30px`

3. **Auto-focus editor after search** (lines 123-124):
   - After successful search, focus transfers to editor
   - Enables immediate editing of found text
   - Only on success - focus stays on dialog if text not found

**Benefits**:

- ✅ Non-blocking: User can edit while dialog is open
- ✅ Positioned like modern IDEs (top-right corner)
- ✅ Singleton prevents multiple dialog instances
- ✅ Automatic focus on search field when opened
- ✅ Dialog brought to front if already open
- ✅ Compact size (20 columns instead of 30)
- ✅ Avoids overlapping split pane UI elements
- ✅ Focus returns to editor after successful search for instant editing

## Application Architecture

- **Main Frame**: `Application.java` extends `JFrame`
- **Script Editor**: Accessed via `parentFrame.getScriptArea()` (RSyntaxTextArea)
- **Action Pattern**: Actions extend `AbstractAction`, receive `Application` in constructor
