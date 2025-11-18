# Real-World User Issues & Pragmatic Fixes

## Implementation Status

### Completed (Week 1 - All Done!)

- ✅ **4K Scaling Fix** - Launcher script created with HiDPI support
- ✅ **Find/Replace Functionality** - Fixed event handlers and added keyboard shortcuts
- ✅ **Last Character Bug** - Fixed tokenizer loop issue
- ✅ **RSyntaxTextArea Update** - Updated to version 3.5.1
- ✅ **Additional improvements:**
  - Maven encoding warnings fixed
  - Java 21 update completed
  - Enter key search functionality added
  - Search wrap-around functionality added

### Deferred (Week 2 - Security)

- ⚠️ **Security Fixes** - SSL certificate validation and encryption improvements deferred per user decision
  - User chose to defer these as they work with trusted internal servers
  - Detailed implementation plan available in `SECURITY-FIXES-PROMPT.md` if needed in future

### Remaining Open (Week 3-5)

- ⚠️ **Multi-Account Support** - Not yet implemented
  - Detailed implementation plan available in `MULTI-ACCOUNT-PROMPT.md`
- ⚠️ **Local File Load/Save** - Not yet implemented
- ⚠️ **Template Insertion** - Not yet implemented

### Implementation Details

See `../../FIXES-APPLIED.md` for complete details of all changes made.

---

## Context: This is a Mini-App

**Important:** This is NOT an enterprise application. It's a small, practical tool that works. The goal is to fix real problems users face, not to over-engineer with patterns and abstractions.

## User-Reported Problems (Priority Order)

### 1. 🔴 CRITICAL: 4K Display Scaling Issue (NEW FINDING)

**User Report:**
> "Aktuell wird unter Linux leider die UI auf einem 4K Monitor winzig gerendert, das war vorher nicht der Fall, da skalierte es normal mit 4k."

**Analysis:**

This is a Java/Swing HiDPI scaling issue on Linux with recent GNOME versions. The app renders tiny because Java's default scaling detection isn't working.

#### Possible Causes

**Hypothesis 1: Java 11 HiDPI Support Not Enabled**

- Java 9+ has HiDPI support but requires explicit flags
- GNOME's Wayland/X11 scaling may not be detected automatically

**Hypothesis 2: GNOME/Wayland Changes**

- Recent GNOME versions changed how they report DPI
- Java may not be reading the correct environment variables

**Hypothesis 3: Missing GDK_SCALE Environment Variable**

- Java on Linux reads `GDK_SCALE` for HiDPI
- May not be set correctly in recent GNOME versions

#### How to Test

**Test 1: Check Java HiDPI Detection**

```bash
# Run with debug output
java -Dsun.java2d.uiScale.enabled=true \
     -Dsun.java2d.uiScale=2.0 \
     -jar SieveEditor-jar-with-dependencies.jar
```

**Test 2: Check Current Scaling**

```bash
# Check what GNOME reports
echo $GDK_SCALE
echo $GDK_DPI_SCALE
xrdb -query | grep Xft.dpi
```

**Test 3: Force Different Scale Factors**

```bash
# Try various scale factors
for scale in 1.5 2.0 2.5; do
  java -Dsun.java2d.uiScale=$scale -jar SieveEditor-jar-with-dependencies.jar
done
```

**Test 4: Check Wayland vs X11**

```bash
# Check if running under Wayland
echo $XDG_SESSION_TYPE

# Try forcing X11
GDK_BACKEND=x11 java -jar SieveEditor-jar-with-dependencies.jar
```

#### Solutions (in order of simplicity)

**Solution 1: Create Launch Script (RECOMMENDED)**

Create `sieveeditor.sh`:

```bash
#!/bin/bash
# SieveEditor launcher with HiDPI support

# Detect scale factor
if [ -n "$GDK_SCALE" ]; then
    SCALE=$GDK_SCALE
elif command -v gsettings &> /dev/null; then
    # Try to read from GNOME settings
    SCALE=$(gsettings get org.gnome.desktop.interface scaling-factor | cut -d' ' -f2)
else
    # Default to 2.0 for 4K
    SCALE=2.0
fi

# Set Java properties for HiDPI
JAVA_OPTS="-Dsun.java2d.uiScale.enabled=true"
JAVA_OPTS="$JAVA_OPTS -Dsun.java2d.uiScale=$SCALE"
JAVA_OPTS="$JAVA_OPTS -Dawt.useSystemAAFontSettings=lcd"
JAVA_OPTS="$JAVA_OPTS -Dswing.defaultlaf=javax.swing.plaf.nimbus.NimbusLookAndFeel"

# Launch application
java $JAVA_OPTS -jar "$(dirname "$0")/SieveEditor-jar-with-dependencies.jar" "$@"
```

**Solution 2: Add to Application Code**

In `Application.java` constructor, before UI setup:

```java
// Add at line ~55, before UI initialization
private void setupHiDPI() {
    // Enable HiDPI on Linux
    System.setProperty("sun.java2d.uiScale.enabled", "true");

    // Try to detect scale from environment
    String gdkScale = System.getenv("GDK_SCALE");
    if (gdkScale != null && !gdkScale.isEmpty()) {
        System.setProperty("sun.java2d.uiScale", gdkScale);
    }

    // Better font rendering on Linux
    System.setProperty("awt.useSystemAAFontSettings", "lcd");
    System.setProperty("swing.aatext", "true");
}
```

**Solution 3: Maven Assembly Plugin Configuration**

Update `pom.xml` to create launcher with correct JVM args:

```xml
<!-- Add to pom.xml -->
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>appassembler-maven-plugin</artifactId>
    <version>2.1.0</version>
    <configuration>
        <programs>
            <program>
                <mainClass>de.febrildur.sieveeditor.Application</mainClass>
                <id>sieveeditor</id>
            </program>
        </programs>
        <extraJvmArguments>-Dsun.java2d.uiScale.enabled=true -Dawt.useSystemAAFontSettings=lcd</extraJvmArguments>
    </configuration>
</plugin>
```

#### Recommendation

**Use Solution 1 (Launch Script)** - It's the simplest and doesn't require code changes. Users can adjust scale factor easily if needed.

---

### 2. 🔴 CRITICAL: Find/Replace Doesn't Work (CONFIRMED)

**User Report:**
> "Es soll wohl eine suchen & ersetzen funktion im editor sein, aber die funktioniert nicht."
> "Ctrl-F or Ctrl-R does nothing. Maybe wrong hot-keys there. I think there was a menu for it, but it also did nothing."

**Analysis:**

I analyzed this - the problem is actually **two separate issues**:

1. **Event handlers attached to wrong objects** - Buttons open new dialog instead of searching
2. **Menu item has wrong action name** - Should be "Replace" not "Save"

#### Quick Fix (Minimal Changes)

**File:** [ActionReplace.java](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionReplace.java)

```java
// Line 28: Fix the action name
public ActionReplace(Application parentFrame) {
    super("Find/Replace"); // CHANGE FROM "Save"
    this.parentFrame = parentFrame;
}

// Remove lines 48-49 and 77 (wrong listeners)
// nextButton.addActionListener(this); // DELETE
// prevButton.addActionListener(this); // DELETE

// Add correct listeners (around line 75)
nextButton.addActionListener((event) -> performSearch(true));
prevButton.addActionListener((event) -> performSearch(false));

// Add this new method after line 74:
private void performSearch(boolean forward) {
    String text = searchField.getText();
    if (text.isEmpty()) return;

    SearchContext context = new SearchContext();
    context.setSearchFor(text);
    context.setMatchCase(matchCaseCB.isSelected());
    context.setRegularExpression(regexCB.isSelected());
    context.setSearchForward(forward);

    boolean found = SearchEngine.find(parentFrame.getTextArea(), context).wasFound();
    if (!found) {
        JOptionPane.showMessageDialog(frame, "Text not found");
    }
}
```

**File:** [Application.java](../../../src/main/java/de/febrildur/sieveeditor/Application.java)

```java
// Line 82-84: Add to Edit menu with proper keyboard shortcut
JMenuItem miReplace = new JMenuItem(actionReplace);
miReplace.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
editMenu.add(miReplace);
```

---

### 3. 🔴 HIGH: Last Character on Line Unreachable

**User Report:**
> "beim editieren kann teilweise der letzte buchstabe auf der zeile nicht mehr erreicht werden was zu seltsamen fehlern in bestimmten zielen führt."

**Analysis:**

This is likely related to the tokenizer bug in `SieveTokenMaker.java:176` where the loop variable modification doesn't work. This causes incorrect token boundaries.

#### Quick Fix

Replace the `IntStream.forEach` with a simple for-loop (see bug fix in section 3.6 of bugs document).

---

### 4. 🟠 MEDIUM: Only One Account Supported

**User Report:**
> "App speichert credentials im Home, als JSON vermutlich. Dort kann immer nur ein Account angelegt werden, daher muss ich mit Symlinks arbeiten um mehrere accounts mit dem tool editieren zu können."

**Current Implementation:**

- Single properties file: `~/.sieveproperties`
- Only stores one server configuration

#### Pragmatic Solution (No Over-Engineering)

**Option A: Multiple Profile Support (Simple)**

Create `~/.sieveprofiles/` directory with multiple files:

```text
~/.sieveprofiles/
  ├── default.properties
  ├── work.properties
  ├── personal.properties
```

**Changes Needed:**

1. Add profile selector to connection dialog (combobox)
2. Load/save to selected profile file
3. Store last-used profile name

**Code Changes (Minimal):**

```java
// PropertiesSieve.java
private String profileName = "default";

public PropertiesSieve(String profileName) {
    this.profileName = profileName;
    this.propFileName = System.getProperty("user.home") +
        File.separator + ".sieveprofiles" +
        File.separator + profileName + ".properties";
}

public static List<String> getAvailableProfiles() {
    File profilesDir = new File(System.getProperty("user.home"), ".sieveprofiles");
    if (!profilesDir.exists()) return Arrays.asList("default");

    return Arrays.stream(profilesDir.listFiles())
        .filter(f -> f.getName().endsWith(".properties"))
        .map(f -> f.getName().replace(".properties", ""))
        .collect(Collectors.toList());
}
```

```java
// ActionConnect.java - Add profile selector to dialog
JComboBox<String> profileCombo = new JComboBox<>(
    PropertiesSieve.getAvailableProfiles().toArray(new String[0])
);
panel.add(new JLabel("Profile:"));
panel.add(profileCombo);
```

**Effort:** 2-3 hours

---

### 5. 🟢 LOW: Old Dependencies

**User Report:**
> "vermutlich sind etliche dependencies uralt, gammlig, abandoned oder was weiß ich. aber dies ist er der einzige editor der tatsächlich funktioniert..."

**Dependency Check:**

| Dependency | Current | Latest | Status | Action |
|------------|---------|--------|--------|--------|
| rsyntaxtextarea | 3.3.4 | 3.5.0 | Active | ✅ Update |
| managesievej | 0.3.1 | 0.3.1 | Abandoned (2014) | ⚠️ Keep (works) |
| commons-codec | 1.16.0 | 1.16.0 | Active | ✅ Current |
| jasypt | 1.9.3 | 1.9.3 | Slow (2014) | ⚠️ Keep or replace |

**Pragmatic Approach:**

- Update RSyntaxTextArea to 3.5.0 (easy, safe)
- Keep ManageSieveJ (abandoned but works, no alternative)
- Keep commons-codec (current)
- Keep Jasypt for now (or remove if using OS credential storage)

**Changes to pom.xml:**

```xml
<dependency>
    <groupId>com.fifesoft</groupId>
    <artifactId>rsyntaxtextarea</artifactId>
    <version>3.5.0</version> <!-- UPDATE from 3.3.4 -->
</dependency>
```

---

## Nice-to-Have Features

### 1. Local File Load/Save

**User Request:**
> "Die Skripte von der Platte lokal laden und speichern zu können"

**Implementation:**

Add menu items:

- File → Open Local Script... (Ctrl+L)
- File → Save Local Script... (Ctrl+Shift+S)

```java
// Application.java
private AbstractAction actionOpenLocal = new AbstractAction("Open Local Script...") {
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Sieve Scripts (*.sieve)", "sieve"));
        if (chooser.showOpenDialog(Application.this) == JFileChooser.APPROVE_OPTION) {
            try {
                String content = Files.readString(chooser.getSelectedFile().toPath());
                textArea.setText(content);
                // Set script to null to indicate it's local, not on server
                script = null;
                updateStatus();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(Application.this,
                    "Failed to load file: " + ex.getMessage());
            }
        }
    }
};

private AbstractAction actionSaveLocal = new AbstractAction("Save Local Script...") {
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Sieve Scripts (*.sieve)", "sieve"));
        if (chooser.showSaveDialog(Application.this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(chooser.getSelectedFile().toPath(), textArea.getText());
                JOptionPane.showMessageDialog(Application.this, "File saved.");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(Application.this,
                    "Failed to save file: " + ex.getMessage());
            }
        }
    }
};
```

**Effort:** 1-2 hours

### 2. Templating

**User Request:**
> "Templating um bestimmte sich wiederholende Elemente einfach mit neuen parametern im skript einbauen zu können"

**Simple Implementation:**

Add "Insert Template" menu with common patterns:

```java
// Create templates directory: ~/.sievetemplates/
// Users can add .sieve files there

private JMenu createTemplateMenu() {
    JMenu templateMenu = new JMenu("Insert Template");

    // Add built-in templates
    templateMenu.add(createTemplateAction("Spam Filter",
        "if header :contains \"subject\" \"spam\" {\n" +
        "    fileinto \"Spam\";\n" +
        "    stop;\n" +
        "}\n"));

    templateMenu.add(createTemplateAction("Vacation Reply",
        "require [\"vacation\"];\n" +
        "vacation :days 7 \"I'm on vacation\";\n"));

    templateMenu.addSeparator();

    // Load user templates from ~/.sievetemplates/
    File templatesDir = new File(System.getProperty("user.home"), ".sievetemplates");
    if (templatesDir.exists()) {
        for (File f : templatesDir.listFiles((d, name) -> name.endsWith(".sieve"))) {
            try {
                String content = Files.readString(f.toPath());
                String name = f.getName().replace(".sieve", "");
                templateMenu.add(createTemplateAction(name, content));
            } catch (IOException ignored) {}
        }
    }

    return templateMenu;
}

private AbstractAction createTemplateAction(String name, String template) {
    return new AbstractAction(name) {
        public void actionPerformed(ActionEvent e) {
            textArea.insert(template, textArea.getCaretPosition());
        }
    };
}
```

**Effort:** 2-3 hours

---

## Revised Priority List (Pragmatic)

### Week 1: Critical User-Facing Issues

1. [x] Fix 4K scaling (launcher script) - **1 hour** (✅ DONE - see FIXES-APPLIED.md)
2. [x] Fix Find/Replace functionality - **2 hours** (✅ DONE - see FIXES-APPLIED.md)
3. [x] Fix last character bug (tokenizer) - **1 hour** (✅ DONE - see FIXES-APPLIED.md)
4. [x] Update RSyntaxTextArea to 3.5.1 - **30 minutes** (✅ DONE - see FIXES-APPLIED.md)

**Planned:** ~1 day of work | **Actual:** ~1 day of work ✅

### Week 2: Security & Stability

1. [x] Fix SSL certificate validation - **2 hours** (⚠️ DEFERRED - see SECURITY-FIXES-PROMPT.md)
2. [x] Remove hardcoded encryption key - **4 hours** (⚠️ DEFERRED - see SECURITY-FIXES-PROMPT.md)
3. [x] Add null checks to prevent crashes - **2 hours** (⚠️ DEFERRED - see SECURITY-FIXES-PROMPT.md)

**Planned:** ~1 day of work | **Actual:** Not implemented (deferred per user decision)

### Week 3: Multi-Account Support

1. [ ] Multiple profile support - **3 hours** (⚠️ TODO - prompt available in MULTI-ACCOUNT-PROMPT.md)

**Planned:** Half day | **Status:** Not yet implemented

### Week 4: Nice-to-Have Features

1. [ ] Local file load/save - **2 hours** (⚠️ TODO)
2. [ ] Template insertion - **3 hours** (⚠️ TODO)

**Planned:** Half day | **Status:** Not yet implemented

### Week 5: Polish & Testing

1. [ ] Write basic tests for fixes - **1 day** (⚠️ TODO)
2. [ ] Manual testing on 4K display - **2 hours** (⚠️ TODO)
3. [ ] Update documentation - **2 hours** (⚠️ TODO)

**Planned:** ~1.5 days | **Status:** Not yet implemented

---

## Total Revised Effort Estimate

**Original Plan:** 5 Days

**Actual Implementation:**

- Week 1 (Critical Fixes): ✅ ~1 day (COMPLETED)
- Week 2 (Security): ⚠️ ~1 day (DEFERRED per user decision)
- Week 3-5 (Enhancements): ⚠️ ~2 days (NOT YET IMPLEMENTED)

**Not 12 weeks.** This is a pragmatic mini-app approach.

---

## What We're NOT Doing (Appropriately)

❌ Full dependency injection framework
❌ Complex abstraction layers
❌ Enterprise design patterns
❌ 80% test coverage (aim for 40% on critical paths)
❌ Extensive refactoring
❌ Over-engineering

✅ Fix what's broken
✅ Add what's useful
✅ Keep it simple
✅ Focus on user experience

---

## Testing Strategy (Pragmatic)

**Don't aim for 80% coverage.** This is overkill for a mini-app.

**Aim for:**

- ✅ Test security fixes (SSL, encryption)
- ✅ Test critical bug fixes (NPE prevention)
- ✅ Test Find/Replace functionality
- ✅ Manual testing on 4K display
- ✅ Manual testing of multi-profile support

**Target: 30-40% coverage on critical paths only.**

---

## Updated Documentation Structure

The original analysis documents are still valuable for understanding issues in depth, but here's the pragmatic implementation guide:

1. **This document** - Real problems, real solutions, realistic timeline
2. Original docs - Reference for detailed analysis when needed

---

## Conclusion

**Original Analysis:** Correct but over-engineered for a mini-app
**This Analysis:** Pragmatic, focused on real user problems
**Timeline:** 5 days instead of 12 weeks
**Approach:** Fix what's broken, keep what works, don't over-complicate

The app is "kruschtig" (crusty) but it works. Let's keep it that way while fixing the real issues users face.
