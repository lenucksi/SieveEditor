# Bugs and Error Handling Issues

## Executive Summary

The SieveEditor application has **8 HIGH** severity bugs that cause crashes or data loss, and **15 MEDIUM** severity issues that cause incorrect behavior. Most issues stem from missing null checks, incorrect exception handling, and broken UI event handlers.

## CRITICAL Bugs

### 1. Find/Replace Buttons Don't Work (CRITICAL)

**Location:** [ActionReplace.java:48-49, 77](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionReplace.java#L48-L49)

**Issue:** Event handlers are connected to the wrong actions, making the Find Next and Find Previous buttons non-functional.

-> From what I see as a human: Ctrl-F or Ctrl-R does nothing. Maybe wrong hot-keys there. I think there was a menu for it, but it also did nothing. So the problems might run deeper.

```java
nextButton.setActionCommand("FindNext");
nextButton.addActionListener(this); // 'this' is ActionReplace, not search handler
// ...
prevButton.addActionListener(this); // Same problem
```

The actual search logic is attached to the searchField (lines 51-74), not the buttons. When users click the buttons, it triggers ActionReplace.actionPerformed() which creates a new dialog instead of searching.

**Impact:**

- Find/Replace feature is completely broken
- Clicking "Find Next" or "Find Previous" opens a new dialog
- Only way to search is to press Enter in the search field
- Major usability bug

**Fix Required:**

```java
// Remove: nextButton.addActionListener(this);
// Add the search logic to button listeners instead of searchField
nextButton.addActionListener((event) -> {
    String text = searchField.getText();
    if (text.length() == 0) return;

    SearchContext context = new SearchContext();
    context.setSearchFor(text);
    context.setMatchCase(matchCaseCB.isSelected());
    context.setRegularExpression(regexCB.isSelected());
    context.setSearchForward(true);
    context.setWholeWord(false);

    boolean found = SearchEngine.find(parentFrame.getTextArea(), context).wasFound();
    if (!found) {
        JOptionPane.showMessageDialog(frame, "Text not found");
    }
});
```

### 2. Wrong Error Message in Rename Operation (CRITICAL)

**Location:** [ActionActivateDeactivateScript.java:91](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionActivateDeactivateScript.java#L91)

**Issue:** Copy-paste error shows wrong success message.

```java
JOptionPane.showMessageDialog(parentFrame, "deactivate all scripts"); // Should say "renamed"
```

**Impact:**

- User sees "deactivate all scripts" after renaming a script
- Completely misleading feedback
- Users will think the wrong operation was performed

**Fix Required:**

```java
JOptionPane.showMessageDialog(parentFrame, "Script renamed to: " + newname);
```

## HIGH Severity Bugs

### 3. NullPointerException in setScript (HIGH)

**Location:** [Application.java:119](../../../src/main/java/de/febrildur/sieveeditor/Application.java#L119)

**Issue:** No null check for server before calling getScript().

```java
public void setScript(SieveScript script) throws IOException, ParseException {
    this.script = script;
    textArea.setText(server.getScript(script)); // server could be null
}
```

**Impact:**

- Crashes when loading script before connecting to server
- NullPointerException shown to user

**Fix Required:**

```java
public void setScript(SieveScript script) throws IOException, ParseException {
    if (server == null) {
        throw new IllegalStateException("Not connected to server");
    }
    this.script = script;
    textArea.setText(server.getScript(script));
}
```

### 4. NullPointerException in Save Methods (HIGH)

**Location:** [Application.java:122-132](../../../src/main/java/de/febrildur/sieveeditor/Application.java#L122-L132)

**Issue:** No validation for null script, server, or name.

```java
public void save() {
    save(script.getName()); // script could be null
}

public void save(String name) {
    try {
        server.putScript(name, textArea.getText()); // server could be null, name could be null
```

**Impact:**

- Crashes when saving before connecting or loading a script
- Crashes when name parameter is null

**Fix Required:**

```java
public void save() {
    if (script == null) {
        throw new IllegalStateException("No script loaded");
    }
    save(script.getName());
}

public void save(String name) {
    if (server == null) {
        throw new IllegalStateException("Not connected to server");
    }
    if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("Script name cannot be empty");
    }
    try {
        server.putScript(name, textArea.getText());
```

### 5. Save Always Shows Success Message (HIGH)

**Location:** [ActionSaveScript.java:21-23](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionSaveScript.java#L21-L23)

**Issue:** Shows "Script saved" message even if save failed.

```java
public void actionPerformed(ActionEvent e) {
    parentFrame.save(); // Exceptions caught internally
    parentFrame.updateStatus();
    JOptionPane.showMessageDialog(parentFrame, "Script saved.");
}
```

In Application.java:129-131, the save() method catches exceptions and prints them but doesn't rethrow:

```java
} catch (IOException e1) {
    e1.printStackTrace(); // Only prints, doesn't notify caller
}
```

**Impact:**

- User thinks save succeeded when it actually failed
- Data loss if user closes application expecting save worked
- Major reliability issue

**Fix Required:**

```java
// In Application.java, change save() to throw exception or return boolean
public boolean save(String name) {
    try {
        server.putScript(name, textArea.getText());
        return true;
    } catch (IOException e1) {
        JOptionPane.showMessageDialog(this,
            "Failed to save script: " + e1.getMessage(),
            "Save Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }
}

// In ActionSaveScript.java
public void actionPerformed(ActionEvent e) {
    if (parentFrame.save()) {
        parentFrame.updateStatus();
        JOptionPane.showMessageDialog(parentFrame, "Script saved.");
    }
}
```

### 6. Null Input Not Handled in Save As (HIGH)

**Location:** [ActionSaveScriptAs.java:20-23](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionSaveScriptAs.java#L20-L23)

**Issue:** When user clicks Cancel, newName is null but still passed to save().

```java
String newName = JOptionPane.showInputDialog("Rename to:", parentFrame.getScriptName());
try {
    parentFrame.save(newName); // newName could be null
```

**Impact:**

- NullPointerException when user cancels dialog
- Application crashes

**Fix Required:**

```java
String newName = JOptionPane.showInputDialog("Rename to:", parentFrame.getScriptName());
if (newName == null || newName.isBlank()) {
    return; // User cancelled or entered blank
}
try {
    parentFrame.save(newName);
```

### 7. Array Index Out of Bounds (HIGH)

**Location:** [ActionActivateDeactivateScript.java:57, 83](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionActivateDeactivateScript.java#L57)

**Issue:** getSelectedRow() returns -1 if no row selected, causing array access error.

```java
activate.addActionListener((event) -> {
    String script = rowData[table.getSelectedRow()][0]; // getSelectedRow() can return -1
```

Same issue in rename handler (line 83).

**Impact:**

- Crashes when user clicks activate/rename without selecting a row
- ArrayIndexOutOfBoundsException

**Fix Required:**

```java
activate.addActionListener((event) -> {
    int selectedRow = table.getSelectedRow();
    if (selectedRow < 0) {
        JOptionPane.showMessageDialog(parentFrame,
            "Please select a script first",
            "No Selection", JOptionPane.WARNING_MESSAGE);
        return;
    }
    String script = rowData[selectedRow][0];
```

### 8. Incorrect Tokenization Due to Loop Variable Modification (HIGH)

**Location:** [SieveTokenMaker.java:176, 184](../../../src/main/java/de/febrildur/sieveeditor/system/SieveTokenMaker.java#L176)

**Issue:** Attempting to modify loop variable inside forEach lambda has no effect.

```java
IntStream.range(offset, end).forEach(i -> {
    // ...
    case TokenTypes.LITERAL_NUMBER_DECIMAL_INT:
        // ...
        i--; // This modifies local copy, not loop variable!
```

**Impact:**

- Incorrect syntax highlighting
- Characters get skipped or processed incorrectly
- Parser state becomes inconsistent
- Tokens not properly recognized

**Fix Required:**
Replace forEach with traditional for-loop:

```java
for (int i = offset; i < end; i++) {
    char c = array[i];

    switch (currentTokenType) {
        case TokenTypes.LITERAL_NUMBER_DECIMAL_INT:
            // ...
            i--; // Now this works correctly
            currentTokenType = TokenTypes.NULL;
```

### 9. No Null Check in checkScript (HIGH)

**Location:** [ActionCheckScript.java:26](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionCheckScript.java#L26)

**Issue:** Server might be null.

```java
scriptInfo = parentFrame.getServer().checkScript(parentFrame.getScriptText());
```

**Impact:**

- NullPointerException if server connection lost
- Application crashes

**Fix Required:**

```java
ConnectAndListScripts server = parentFrame.getServer();
if (server == null || !server.isLoggedIn()) {
    JOptionPane.showMessageDialog(parentFrame,
        "Not connected to server",
        "Connection Error", JOptionPane.ERROR_MESSAGE);
    return;
}
scriptInfo = server.checkScript(parentFrame.getScriptText());
```

## MEDIUM Severity Bugs

### 10. Resource Leak in ActionConnect (MEDIUM)

**Location:** [ActionConnect.java:73](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionConnect.java#L73)

**Issue:** Dialog not properly disposed.

```java
frame.setVisible(false); // Should use frame.dispose()
```

**Impact:**

- Memory leak with multiple connection attempts
- Dialog resources not released
- Accumulates over time

**Fix Required:**

```java
frame.dispose();
```

### 11. Dialog Created Before Operations That Might Fail (MEDIUM)

**Location:** [ActionLoadScript.java:47](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionLoadScript.java#L47)

**Issue:** Dialog created before getListScripts() call, causing resource leak if exception occurs.

```java
final JDialog frame = new JDialog(parentFrame, "Select Script", true);
try {
    JPanel panel = new JPanel();
    // ...
    SieveScript[] liste = parentFrame.getServer().getListScripts().toArray(new SieveScript[0]);
    // If exception here, frame is created but not visible and not disposed
```

**Impact:**

- Resource leak if getListScripts() throws exception
- Dialog window exists but never shown or disposed

**Fix Required:**

```java
try {
    // Get scripts first, before creating UI
    SieveScript[] liste = parentFrame.getServer().getListScripts().toArray(new SieveScript[0]);

    // Now create dialog
    final JDialog frame = new JDialog(parentFrame, "Select Script", true);
    JPanel panel = new JPanel();
```

### 12. Empty Script List Not Handled (MEDIUM)

**Location:** [ActionLoadScript.java:47-48](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionLoadScript.java#L47-L48)

**Issue:** If no scripts exist on server, shows empty combobox without explanation.

```java
SieveScript[] liste = parentFrame.getServer().getListScripts().toArray(new SieveScript[0]);
JComboBox<SieveScript> tfScript = new JComboBox<SieveScript>(liste);
```

**Impact:**

- Confusing UX - empty dialog with no explanation
- User doesn't know if it's an error or there are no scripts

**Fix Required:**

```java
SieveScript[] liste = parentFrame.getServer().getListScripts().toArray(new SieveScript[0]);
if (liste.length == 0) {
    JOptionPane.showMessageDialog(parentFrame,
        "No scripts found on server",
        "No Scripts", JOptionPane.INFORMATION_MESSAGE);
    return;
}
```

### 13. Null Return Value from getSelectedItem (MEDIUM)

**Location:** [ActionLoadScript.java:62](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionLoadScript.java#L62)

**Issue:** No null check before casting.

```java
parentFrame.setScript((SieveScript) tfScript.getSelectedItem()); // Could be null
```

**Impact:**

- If list is somehow empty, getSelectedItem() returns null
- NullPointerException in setScript()

**Fix Required:**

```java
SieveScript selected = (SieveScript) tfScript.getSelectedItem();
if (selected != null) {
    parentFrame.setScript(selected);
    frame.dispose();
}
```

### 14. Data Not Refreshed After Operations (MEDIUM)

**Location:** [ActionActivateDeactivateScript.java:64, 76](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionActivateDeactivateScript.java#L64)

**Issue:** Script list table shows stale data after activate/deactivate/rename operations.

```java
JOptionPane.showMessageDialog(parentFrame, "activate " + script);
// Dialog not refreshed to show new active status
```

**Impact:**

- User sees outdated information
- Must close and reopen dialog to see changes
- Confusing UX

**Fix Required:**

```java
// After each operation, reload the table data
try {
    parentFrame.getServer().activateScript(script);
    // Reload scripts and refresh table
    Collection<SieveScript> scripts = parentFrame.getServer().getListScripts();
    // Update table model with new data
    // ... refresh table display
    JOptionPane.showMessageDialog(parentFrame, "Activated: " + script);
} catch (IOException | ParseException e1) {
    JOptionPane.showMessageDialog(parentFrame, e1.getMessage());
}
```

### 15. IOException in write() Swallowed (MEDIUM)

**Location:** [PropertiesSieve.java:58-60](../../../src/main/java/de/febrildur/sieveeditor/system/PropertiesSieve.java#L58-L60)

**Issue:** Write failures only print stack trace, don't notify user.

```java
} catch (IOException io) {
    io.printStackTrace(); // Only prints to console
}
```

**Impact:**

- User thinks settings saved when they actually failed
- Lost configuration changes
- No feedback about disk full, permission errors, etc.

**Fix Required:**

```java
} catch (IOException io) {
    io.printStackTrace();
    JOptionPane.showMessageDialog(null,
        "Failed to save settings: " + io.getMessage(),
        "Save Error", JOptionPane.ERROR_MESSAGE);
}
```

### 16. Race Condition on File Creation (MEDIUM)

**Location:** [PropertiesSieve.java:27](../../../src/main/java/de/febrildur/sieveeditor/system/PropertiesSieve.java#L27)

**Issue:** createNewFile() return value not checked.

```java
propFile.createNewFile(); // Can fail if file exists
```

**Impact:**

- Return value ignored
- Might not create file if already exists
- No error handling for failure cases

**Fix Required:**

```java
if (!propFile.exists()) {
    if (!propFile.createNewFile()) {
        throw new IOException("Failed to create properties file: " + propFileName);
    }
}
```

### 17. NumberFormatException on Invalid Port (MEDIUM)

**Location:** [PropertiesSieve.java:35](../../../src/main/java/de/febrildur/sieveeditor/system/PropertiesSieve.java#L35)

**Issue:** If properties file manually edited with invalid port, throws exception.

```java
port = Integer.valueOf(prop.getProperty("sieve.port", "4190"));
```

**Impact:**

- Application won't start if properties file corrupted
- No recovery mechanism

**Fix Required:**

```java
try {
    port = Integer.valueOf(prop.getProperty("sieve.port", "4190"));
} catch (NumberFormatException e) {
    port = 4190; // Use default
    e.printStackTrace();
}
```

### 18. Wrong Dialog Title (MEDIUM)

**Location:** [ActionReplace.java:39](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionReplace.java#L39)

**Issue:** Copy-pasted code has wrong title.

```java
final JDialog frame = new JDialog(parentFrame, "Connection", true); // Should be "Find"
```

**Impact:**

- Confusing UI - shows "Connection" title on Find dialog
- Poor user experience

**Fix Required:**

```java
final JDialog frame = new JDialog(parentFrame, "Find and Replace", true);
```

### 19. Empty Search Silently Ignored (MEDIUM)

**Location:** [ActionReplace.java:60-62](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionReplace.java#L60-L62)

**Issue:** No user feedback for empty search.

```java
if (text.length() == 0) {
    return; // Silently ignores empty search
}
```

**Impact:**

- Button appears non-functional
- No indication why nothing happened

**Fix Required:**

```java
if (text.length() == 0) {
    JOptionPane.showMessageDialog(frame,
        "Please enter text to search for",
        "Empty Search", JOptionPane.WARNING_MESSAGE);
    return;
}
```

## LOW Severity Bugs

### 20. Inconsistent Error Display (LOW)

**Location:** [ActionCheckScript.java:27, 29](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionCheckScript.java#L27)

**Issue:** Success shows full message, error shows only message (not class name like other actions).

```java
JOptionPane.showMessageDialog(parentFrame, scriptInfo); // Line 27
JOptionPane.showMessageDialog(parentFrame, e1.getMessage()); // Line 29
```

**Impact:**

- Inconsistent error reporting style
- Minor usability issue

### 21. Popup Menu Incomplete (LOW)

**Location:** [ActionActivateDeactivateScript.java:95-100](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionActivateDeactivateScript.java#L95-L100)

**Issue:** Only handles mouseReleased; should also handle mousePressed.

```java
table.addMouseListener(new MouseAdapter() {
    public void mouseReleased(MouseEvent me) {
        if (me.isPopupTrigger())
            popmen.show(me.getComponent(), me.getX(), me.getY());
    }
});
```

**Impact:**

- Popup might not work on all platforms
- Windows/Mac/Linux have different popup trigger behavior

**Fix Required:**

```java
table.addMouseListener(new MouseAdapter() {
    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            // Select row under cursor
            int row = table.rowAtPoint(e.getPoint());
            if (row >= 0) {
                table.setRowSelectionInterval(row, row);
            }
            popmen.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }
});
```

### 22. Early Return in Constructor (LOW)

**Location:** [Application.java:55-58](../../../src/main/java/de/febrildur/sieveeditor/Application.java#L55-L58)

**Issue:** Early return leaves JFrame partially initialized.

```java
} catch (IOException e) {
    JOptionPane.showMessageDialog(null, e.getClass().getName() + ": " + e.getMessage());
    return; // Constructor returns early
}
```

**Impact:**

- Application window might be partially created
- Could cause NullPointerExceptions in callbacks

**Fix Required:**
Don't return in constructor; throw exception instead:

```java
} catch (IOException e) {
    JOptionPane.showMessageDialog(null,
        "Failed to load settings: " + e.getMessage(),
        "Initialization Error", JOptionPane.ERROR_MESSAGE);
    throw new RuntimeException("Application initialization failed", e);
}
```

### 23. No Validation of Server Parameters (LOW)

**Location:** [ConnectAndListScripts.java:29](../../../src/main/java/de/febrildur/sieveeditor/system/ConnectAndListScripts.java#L29)

**Issue:** Null or empty strings not validated.

```java
public void connect(String server, int port, String username, String password) {
    client = new ManageSieveClient();
    ManageSieveResponse resp = client.connect(server, port);
```

**Impact:**

- Cryptic errors from underlying library
- Poor error messages for users

**Fix Required:**

```java
public void connect(String server, int port, String username, String password) {
    if (server == null || server.isBlank()) {
        throw new IllegalArgumentException("Server cannot be empty");
    }
    if (port < 1 || port > 65535) {
        throw new IllegalArgumentException("Port must be between 1 and 65535");
    }
    if (username == null || username.isBlank()) {
        throw new IllegalArgumentException("Username cannot be empty");
    }
    // Note: password can potentially be empty for anonymous login
```

### 24. isLoggedIn Name Misleading (LOW)

**Location:** [ConnectAndListScripts.java:93-95](../../../src/main/java/de/febrildur/sieveeditor/system/ConnectAndListScripts.java#L93-L95)

**Issue:** Name suggests authentication state but only checks if client exists.

```java
public boolean isLoggedIn() {
    return client != null; // Should check authentication state
}
```

**Impact:**

- Could return true even if not authenticated
- Misleading method name

**Fix Required:**

```java
public boolean isConnected() { // Rename method
    return client != null;
}
```

### 25. No Null Validation in Renderer (LOW)

**Location:** [ToStringListCellRenderer.java:19](../../../src/main/java/de/febrildur/sieveeditor/system/ToStringListCellRenderer.java#L19)

**Issue:** No null check on value.

```java
return originalRenderer.getListCellRendererComponent(list, toString.toString(value), index, isSelected,
        cellHasFocus);
```

**Impact:**

- NPE if value is null and toString doesn't handle it
- UI rendering fails

**Fix Required:**

```java
String displayText = (value == null) ? "" : toString.toString(value);
return originalRenderer.getListCellRendererComponent(list, displayText, index, isSelected, cellHasFocus);
```

## Summary by Severity

| Severity | Count | Status |
|----------|-------|--------|
| CRITICAL | 2 | Must fix immediately |
| HIGH | 9 | Must fix before release |
| MEDIUM | 10 | Should fix soon |
| LOW | 4 | Nice to fix |
| **TOTAL** | **25** | **All fixable** |

## Recommended Fix Order

### Phase 1: Critical and High Bugs (Week 1)

1. Fix Find/Replace buttons (ActionReplace.java:48-49, 77)
2. Fix wrong error message (ActionActivateDeactivateScript.java:91)
3. Add null checks throughout Application.java
4. Fix save success message always showing (ActionSaveScript.java)
5. Add null checks in ActionSaveScriptAs.java
6. Fix array index bounds (ActionActivateDeactivateScript.java)
7. Fix tokenization loop (SieveTokenMaker.java:176)
8. Add null check in ActionCheckScript.java

### Phase 2: Medium Bugs (Week 2)

9-19. Address all resource leaks, dialog handling, and validation issues

### Phase 3: Low Bugs (Week 3)

20-25. Polish UI behavior and error messages

## Testing Strategy

Each bug fix should include:

1. Unit test demonstrating the bug
2. Unit test verifying the fix
3. Manual testing of the affected feature
4. Regression testing of related features
