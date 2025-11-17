# Next Features Implementation Prompt

## Context

You are working on SieveEditor, a lightweight Java Swing application for editing Sieve mail filter scripts. The critical bugs have been fixed and multi-account support has been implemented. Now we're adding quality-of-life features.

## Project Status

### Completed ✅
- 4K HiDPI scaling fix
- Find/Replace functionality
- Tokenizer bug fix
- Java 21 update
- Multi-account profile support

### Current Task: Add Local File Support and Templates

## Feature 1: Local File Load/Save

### Goal
Allow users to edit Sieve scripts locally without needing a server connection. This is useful for:
- Developing scripts offline
- Backing up scripts locally
- Version control of scripts
- Testing syntax before uploading

### Requirements

1. **File → Open Local Script** (Ctrl+L)
   - Open file chooser filtered to `.sieve` files
   - Load file content into text editor
   - Set application state to "local mode" (script = null)
   - Update window title to show local file name
   - Update action states appropriately

2. **File → Save Local Script** (Ctrl+Shift+S)
   - Open save dialog with `.sieve` extension
   - Save current text area content to file
   - Show success message
   - Keep application in local mode

### Implementation Guide

**File:** `Application.java`

Add two new actions around line 50 (after existing actions):

```java
private AbstractAction actionOpenLocal = new AbstractAction("Open Local Script...") {
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Sieve Scripts (*.sieve)", "sieve");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));

        if (chooser.showOpenDialog(Application.this) == JFileChooser.APPROVE_OPTION) {
            try {
                String content = Files.readString(chooser.getSelectedFile().toPath());
                textArea.setText(content);

                // Set to local mode (not connected to server)
                script = null;
                setTitle("Sieve Editor - " + chooser.getSelectedFile().getName());
                updateStatus();

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(Application.this,
                    "Failed to load file: " + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
};

private AbstractAction actionSaveLocal = new AbstractAction("Save Local Script...") {
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Sieve Scripts (*.sieve)", "sieve");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));

        if (chooser.showSaveDialog(Application.this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            // Add .sieve extension if not present
            if (!file.getName().endsWith(".sieve")) {
                file = new File(file.getAbsolutePath() + ".sieve");
            }

            try {
                Files.writeString(file.toPath(), textArea.getText());
                JOptionPane.showMessageDialog(Application.this,
                    "File saved successfully.",
                    "Save Successful", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(Application.this,
                    "Failed to save file: " + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
};
```

Add to menu (around line 61-75):

```java
JMenu file = new JMenu("File");
menu.add(file);

file.add(new JMenuItem(actionOpenLocal)).setAccelerator(
    KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK));
file.add(new JMenuItem(actionSaveLocal)).setAccelerator(
    KeyStroke.getKeyStroke(KeyEvent.VK_S,
    KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
file.addSeparator();

// Move existing Sieve menu items here or keep separate
```

Add import at top:

```java
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Files;
```

### Testing
1. Open application
2. Press Ctrl+L → select a .sieve file → should load content
3. Edit content
4. Press Ctrl+Shift+S → save to new file → should save successfully
5. Verify file contains edited content

---

## Feature 2: Template Insertion

### Goal
Provide quick insertion of common Sieve script patterns to speed up development.

### Requirements

1. **Insert Template Menu**
   - Add "Insert" menu between "Edit" and "Sieve"
   - Built-in templates for common patterns
   - Support user-defined templates from `~/.sievetemplates/`

2. **Built-in Templates**
   - Spam Filter
   - Vacation Reply
   - Fileinto Rule
   - Header Check
   - Size Check

3. **User Templates**
   - Load .sieve files from `~/.sievetemplates/`
   - Display as menu items
   - Insert at cursor position

### Implementation Guide

**File:** `Application.java`

Add method to create template menu (around line 95, before menu setup):

```java
private JMenu createInsertMenu() {
    JMenu insertMenu = new JMenu("Insert");

    // Built-in templates
    insertMenu.add(createTemplateAction("Spam Filter to Folder",
        "# Move spam to Spam folder\n" +
        "if header :contains \"X-Spam-Flag\" \"YES\" {\n" +
        "    fileinto \"Spam\";\n" +
        "    stop;\n" +
        "}\n"));

    insertMenu.add(createTemplateAction("Vacation Auto-Reply",
        "require [\"vacation\"];\n" +
        "\n" +
        "# Auto-reply for vacation\n" +
        "vacation :days 7 :subject \"Out of Office\"\n" +
        "\"I am currently out of office and will return on [DATE].\n" +
        "Your email will be read upon my return.\";\n"));

    insertMenu.add(createTemplateAction("Fileinto by Subject",
        "# File emails by subject keyword\n" +
        "if header :contains \"subject\" \"[KEYWORD]\" {\n" +
        "    fileinto \"[FOLDER]\";\n" +
        "    stop;\n" +
        "}\n"));

    insertMenu.add(createTemplateAction("Fileinto by From Address",
        "# File emails from specific sender\n" +
        "if address :is \"from\" \"sender@example.com\" {\n" +
        "    fileinto \"[FOLDER]\";\n" +
        "    stop;\n" +
        "}\n"));

    insertMenu.add(createTemplateAction("Reject by Size",
        "require [\"reject\"];\n" +
        "\n" +
        "# Reject emails larger than 10MB\n" +
        "if size :over 10M {\n" +
        "    reject \"Message too large\";\n" +
        "}\n"));

    insertMenu.add(createTemplateAction("Discard by Sender",
        "# Silently discard emails from specific sender\n" +
        "if address :is \"from\" \"spam@example.com\" {\n" +
        "    discard;\n" +
        "    stop;\n" +
        "}\n"));

    // User templates
    File templatesDir = new File(System.getProperty("user.home"), ".sievetemplates");
    if (templatesDir.exists() && templatesDir.isDirectory()) {
        File[] templateFiles = templatesDir.listFiles((dir, name) ->
            name.endsWith(".sieve"));

        if (templateFiles != null && templateFiles.length > 0) {
            insertMenu.addSeparator();
            JMenu userTemplates = new JMenu("User Templates");

            for (File templateFile : templateFiles) {
                try {
                    String content = Files.readString(templateFile.toPath());
                    String name = templateFile.getName().replace(".sieve", "");
                    userTemplates.add(createTemplateAction(name, content));
                } catch (IOException e) {
                    // Skip files that can't be read
                }
            }

            insertMenu.add(userTemplates);
        }
    }

    return insertMenu;
}

private AbstractAction createTemplateAction(String name, String template) {
    return new AbstractAction(name) {
        public void actionPerformed(ActionEvent e) {
            int caretPos = textArea.getCaretPosition();
            textArea.insert(template, caretPos);
            textArea.setCaretPosition(caretPos + template.length());
        }
    };
}
```

Add menu to menu bar (around line 77):

```java
JMenuBar menu = new JMenuBar();
JMenu sieve = new JMenu("Sieve");
menu.add(sieve);

// ... sieve menu items ...

JMenu edit = new JMenu("Edit");
menu.add(edit);

// ... edit menu items ...

// ADD THIS:
menu.add(createInsertMenu());
```

### User Template Directory

Create example user template directory structure:

```bash
mkdir -p ~/.sievetemplates
```

Example user template `~/.sievetemplates/mailing-list.sieve`:
```
# Mailing list filter
if header :contains "List-Id" "mylist.example.com" {
    fileinto "Lists/MyList";
    stop;
}
```

### Testing
1. Open application
2. Click "Insert" menu → should see built-in templates
3. Click "Spam Filter to Folder" → template inserted at cursor
4. Create `~/.sievetemplates/test.sieve` with content
5. Restart application
6. Click "Insert" → "User Templates" → "test" → template inserted

---

## Additional Polish (Optional)

### Better Error Messages

**Current:** Shows exception class names like "java.io.IOException: Connection refused"

**Better:** User-friendly messages

Example in `ActionConnect.java` around line 147:

```java
} catch (IOException e1) {
    String message = "Failed to connect to server";
    if (e1.getMessage().contains("Connection refused")) {
        message += ":\n\nConnection refused. Please check:\n" +
                   "- Server address and port are correct\n" +
                   "- ManageSieve service is running\n" +
                   "- Firewall allows connection";
    } else if (e1.getMessage().contains("Unknown host")) {
        message += ":\n\nServer not found. Please check the server address.";
    } else {
        message += ":\n\n" + e1.getMessage();
    }
    JOptionPane.showMessageDialog(frame, message,
        "Connection Error", JOptionPane.ERROR_MESSAGE);
}
```

---

## Build and Test

After implementing:

```bash
# Build
mvn clean package

# Run
./sieveeditor.sh

# Or
java -jar target/SieveEditor-jar-with-dependencies.jar
```

Test checklist:
- [ ] Ctrl+L opens local file
- [ ] File content loads correctly
- [ ] Ctrl+Shift+S saves to local file
- [ ] Insert menu shows built-in templates
- [ ] Templates insert at cursor position
- [ ] User templates load from ~/.sievetemplates/
- [ ] No errors in console
- [ ] Build succeeds

---

## Commit Strategy

Create commits for each feature:

**Commit 1: Local File Support**
```
Add local file load/save functionality

Allows editing Sieve scripts offline without server connection.

Changes:
- Add File menu with Open Local Script (Ctrl+L)
- Add Save Local Script (Ctrl+Shift+S)
- File chooser filtered to .sieve files
- Update window title with local filename
- Show success/error messages

Users can now:
- Develop scripts offline
- Back up scripts locally
- Use version control for scripts
```

**Commit 2: Template Insertion**
```
Add template insertion for common Sieve patterns

Speeds up script development with predefined templates.

Changes:
- Add Insert menu with built-in templates
- Support user templates from ~/.sievetemplates/
- Templates insert at cursor position
- Include common patterns: spam filter, vacation, fileinto, etc.

Built-in templates:
- Spam filter to folder
- Vacation auto-reply
- Fileinto by subject/sender
- Reject by size
- Discard by sender
```

---

## File Locations

- Main implementation: `src/main/java/de/febrildur/sieveeditor/Application.java`
- User templates: `~/.sievetemplates/*.sieve` (created by user)

---

## Estimated Time

- Local file support: 1-2 hours
- Template insertion: 2-3 hours
- Testing: 1 hour
- **Total: 4-6 hours**

---

## Success Criteria

- [x] Users can open and save .sieve files locally
- [x] Ctrl+L and Ctrl+Shift+S keyboard shortcuts work
- [x] Insert menu provides 6+ built-in templates
- [x] Templates insert at cursor position
- [x] User templates load from ~/.sievetemplates/
- [x] No console errors
- [x] Maven build succeeds
- [x] Manual testing confirms all features work

---

## Notes

- Keep it simple - no complex template system
- Templates are just text insertion
- User templates are just .sieve files in a directory
- No template parameters or variables (nice-to-have for future)
- Focus on productivity, not over-engineering

---

## After Implementation

Update these files:
- `dev-docs/IMPLEMENTATION-STATUS.md` - Mark features as complete
- `README.md` - Document new features
- `dev-docs/analysis/modernization/05-real-world-issues.md` - Update completion status
