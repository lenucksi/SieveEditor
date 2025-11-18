# Start Prompt: Multi-Account Profile Support

## Context

SieveEditor is a Java 21 Swing desktop application for editing Sieve mail filter scripts. Currently, it only supports storing credentials for **one account** at a time.

**User Problem:**
> "App speichert credentials im Home, als JSON vermutlich. Dort kann immer nur ein Account angelegt werden, daher muss ich mit Symlinks arbeiten um mehrere accounts mit dem tool editieren zu können."

**Current Implementation:**

- Single properties file: `~/.sieveproperties`
- Stores: server, port, username, encrypted password
- User must use symlinks to switch between different accounts (awkward!)

**Previous Work Completed:**

- ✅ Java 21 LTS
- ✅ Find/Replace fixed
- ✅ Tokenizer bugs fixed
- ✅ 4K HiDPI scaling fixed
- ⚠️ Security issues documented but deferred

## Your Task

Implement **simple multi-account profile support** so users can manage multiple mail servers without using symlinks.

### Design Requirements

**Keep It Simple** - "Das ist eine Mini-App. Don't overdo patterns."

**User Experience:**

1. User opens connection dialog
2. Sees dropdown to select profile (default, work, personal, etc.)
3. Each profile stores separate server credentials
4. Can create new profiles from UI
5. Last-used profile is remembered

**No Need For:**

- ❌ Profile import/export (too complex)
- ❌ Profile encryption beyond what exists (use existing SimpleEncrypter)
- ❌ Profile synchronization (not needed)
- ❌ Profile templates (over-engineering)
- ✅ Just simple file-based profiles with a selector UI

---

## Implementation Plan

### Step 1: Update Storage Layer

**File:** `src/main/java/de/febrildur/sieveeditor/system/PropertiesSieve.java`

**Current Structure:**

```text
~/.sieveproperties (single file)
```

**New Structure:**

```text
~/.sieveprofiles/
  ├── default.properties
  ├── work.properties
  ├── personal.properties
  └── .lastused (stores last profile name)
```

**Changes Needed:**

1. **Modify constructor** to accept profile name:

```java
public class PropertiesSieve {
    private String profileName;
    private String propFileName;

    public PropertiesSieve(String profileName) {
        this.profileName = profileName;
        File profilesDir = new File(System.getProperty("user.home"), ".sieveprofiles");
        if (!profilesDir.exists()) {
            profilesDir.mkdirs();
        }
        this.propFileName = new File(profilesDir, profileName + ".properties").getAbsolutePath();
    }

    // Keep existing load/save methods, they work with propFileName
}
```

1. **Add profile management methods:**

```java
public static List<String> getAvailableProfiles() {
    File profilesDir = new File(System.getProperty("user.home"), ".sieveprofiles");
    if (!profilesDir.exists() || profilesDir.listFiles() == null) {
        return Arrays.asList("default");
    }

    return Arrays.stream(profilesDir.listFiles())
        .filter(f -> f.getName().endsWith(".properties"))
        .map(f -> f.getName().replace(".properties", ""))
        .sorted()
        .collect(Collectors.toList());
}

public static String getLastUsedProfile() {
    File lastUsedFile = new File(System.getProperty("user.home"),
        ".sieveprofiles/.lastused");
    if (!lastUsedFile.exists()) {
        return "default";
    }
    try {
        return Files.readString(lastUsedFile.toPath()).trim();
    } catch (IOException e) {
        return "default";
    }
}

public static void saveLastUsedProfile(String profileName) {
    File lastUsedFile = new File(System.getProperty("user.home"),
        ".sieveprofiles/.lastused");
    try {
        Files.writeString(lastUsedFile.toPath(), profileName);
    } catch (IOException e) {
        // Ignore - not critical
    }
}

public static boolean profileExists(String profileName) {
    File profileFile = new File(System.getProperty("user.home"),
        ".sieveprofiles/" + profileName + ".properties");
    return profileFile.exists();
}
```

1. **Migration for existing users:**

```java
public static void migrateOldProperties() {
    // Check if old ~/.sieveproperties exists
    File oldFile = new File(System.getProperty("user.home"), ".sieveproperties");
    if (!oldFile.exists()) {
        return; // Nothing to migrate
    }

    // Create new profiles directory
    File profilesDir = new File(System.getProperty("user.home"), ".sieveprofiles");
    profilesDir.mkdirs();

    // Move old file to default.properties
    File newFile = new File(profilesDir, "default.properties");
    if (!newFile.exists()) {
        try {
            Files.copy(oldFile.toPath(), newFile.toPath());
            System.out.println("Migrated old properties to default profile");
        } catch (IOException e) {
            System.err.println("Failed to migrate: " + e.getMessage());
        }
    }
}
```

---

### Step 2: Update Connection Dialog UI

**File:** `src/main/java/de/febrildur/sieveeditor/actions/ActionConnect.java`

**Add profile selector to dialog:**

```java
@Override
public void actionPerformed(ActionEvent e) {
    // Run migration on first use
    PropertiesSieve.migrateOldProperties();

    JDialog frame = new JDialog(parentFrame, "Connection", true);
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
    GridLayout layout = new GridLayout(6, 2, 6, 6); // Changed from 5 to 6 rows
    panel.setLayout(layout);

    // Profile selector (NEW)
    panel.add(new JLabel("Profile:"));
    JPanel profilePanel = new JPanel(new BorderLayout(6, 0));

    List<String> profiles = PropertiesSieve.getAvailableProfiles();
    String lastUsed = PropertiesSieve.getLastUsedProfile();
    JComboBox<String> profileCombo = new JComboBox<>(profiles.toArray(new String[0]));
    profileCombo.setSelectedItem(lastUsed);
    profilePanel.add(profileCombo, BorderLayout.CENTER);

    JButton newProfileButton = new JButton("+");
    newProfileButton.setToolTipText("Create new profile");
    newProfileButton.addActionListener(ev -> {
        String newName = JOptionPane.showInputDialog(frame,
            "Enter new profile name:", "New Profile", JOptionPane.PLAIN_MESSAGE);
        if (newName != null && !newName.trim().isEmpty()) {
            newName = newName.trim().replaceAll("[^a-zA-Z0-9_-]", "");
            if (!newName.isEmpty() && !PropertiesSieve.profileExists(newName)) {
                profileCombo.addItem(newName);
                profileCombo.setSelectedItem(newName);
            }
        }
    });
    profilePanel.add(newProfileButton, BorderLayout.EAST);
    panel.add(profilePanel);

    // Load properties for selected profile
    PropertiesSieve properties = new PropertiesSieve(
        (String) profileCombo.getSelectedItem());
    properties.load();

    // ... rest of existing fields (server, port, username, password)

    // When profile changes, reload properties
    profileCombo.addActionListener(ev -> {
        String selectedProfile = (String) profileCombo.getSelectedItem();
        PropertiesSieve newProps = new PropertiesSieve(selectedProfile);
        newProps.load();

        // Update form fields
        tfServer.setText(newProps.getServer());
        tfPort.setText(String.valueOf(newProps.getPort()));
        tfUsername.setText(newProps.getUsername());
        tfPassword.setText(newProps.getPassword());
    });

    // When OK is clicked, save to selected profile
    buttonOK.addActionListener(event -> {
        String selectedProfile = (String) profileCombo.getSelectedItem();
        PropertiesSieve propsToSave = new PropertiesSieve(selectedProfile);

        // ... existing save logic

        // Save last used profile
        PropertiesSieve.saveLastUsedProfile(selectedProfile);

        // ... rest of connection logic
    });

    // ... rest of dialog setup
}
```

---

### Step 3: Update Application Startup

**File:** `src/main/java/de/febrildur/sieveeditor/Application.java`

**Load last-used profile on startup:**

```java
// In constructor, around line 90-100 where PropertiesSieve is created
public Application() {
    super("Sieve Script Editor");

    // Run migration once
    PropertiesSieve.migrateOldProperties();

    // Load last used profile
    String lastProfile = PropertiesSieve.getLastUsedProfile();
    properties = new PropertiesSieve(lastProfile);
    properties.load();

    // ... rest of initialization
}
```

---

## Testing Requirements

After implementation, verify:

### Test 1: Migration

1. Create old-style `~/.sieveproperties` with test credentials
2. Launch application
3. Should migrate to `~/.sieveprofiles/default.properties`
4. Original file should remain (don't delete it)
5. Connection should work with migrated credentials

### Test 2: Multiple Profiles

1. Open connection dialog
2. See "default" profile in dropdown
3. Click "+" button to create "work" profile
4. Enter different credentials for "work"
5. Connect successfully
6. Close and reopen app
7. "work" should be pre-selected (last used)

### Test 3: Profile Switching

1. Create profiles: "default", "work", "personal"
2. Store different credentials in each
3. Switch between profiles in dropdown
4. Form fields should update with correct credentials
5. Connect to each successfully

### Test 4: Profile Persistence

1. Create profile, enter credentials, connect
2. Close application
3. Reopen application
4. Last-used profile should be selected
5. Credentials should be loaded correctly

### Test 5: Error Handling

1. Try to create profile with invalid name (special chars)
2. Should sanitize or reject
3. Try to create duplicate profile name
4. Should prevent or warn

---

## File Structure Summary

**Files to Modify:**

1. `src/main/java/de/febrildur/sieveeditor/system/PropertiesSieve.java` - Storage layer
2. `src/main/java/de/febrildur/sieveeditor/actions/ActionConnect.java` - Connection UI
3. `src/main/java/de/febrildur/sieveeditor/Application.java` - Startup logic

**New Files Created:**
None (uses existing file structure)

**New Directories:**

- `~/.sieveprofiles/` - Created automatically on first run

---

## User Experience Flow

```text
1. User clicks "Connect"
   ↓
2. Dialog shows profile dropdown (default: last used or "default")
   ↓
3. User can:
   - Select existing profile → form fields auto-fill
   - Click "+" to create new profile → enter name → empty form
   ↓
4. User enters/edits server credentials
   ↓
5. User clicks OK → saves to selected profile
   ↓
6. Connection established
   ↓
7. Profile name is saved as "last used"
   ↓
8. Next time app opens, that profile is pre-selected
```

---

## Edge Cases to Handle

1. **First-time user** (no profiles exist)
   - Show "default" profile
   - Create ~/.sieveprofiles/ directory
   - Save as default.properties

2. **User with old ~/.sieveproperties**
   - Auto-migrate to default.properties
   - Keep old file (don't delete)
   - Show migration message in console

3. **Invalid profile name**
   - Sanitize: remove special characters
   - Only allow: a-z, A-Z, 0-9, _, -
   - Prevent empty names

4. **Duplicate profile name**
   - Check with `profileExists()` before adding
   - Show error: "Profile already exists"

5. **Deleted .lastused file**
   - Fall back to "default" profile
   - Not critical, just convenience

6. **Corrupted profile file**
   - Properties.load() handles gracefully
   - Empty fields if properties missing
   - Let user re-enter and save

---

## Git Commit Message

```text
Add multi-account profile support

Problem: Users could only store credentials for one account,
forcing them to use symlinks to manage multiple mail servers.

Solution: Implemented profile system with:
- Multiple profiles stored in ~/.sieveprofiles/
- Profile selector in connection dialog
- Create new profiles with "+" button
- Auto-migration from old ~/.sieveproperties
- Last-used profile remembered

User experience:
- Select profile from dropdown
- Credentials auto-fill from selected profile
- Create new profiles easily
- No more symlink workarounds needed

Files modified:
- PropertiesSieve.java: Profile storage and management
- ActionConnect.java: Profile selector UI
- Application.java: Load last-used profile on startup

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

---

## Success Criteria

- [ ] Old ~/.sieveproperties automatically migrated
- [ ] Profile dropdown in connection dialog
- [ ] Can create new profiles with "+" button
- [ ] Form fields update when switching profiles
- [ ] Credentials saved per profile
- [ ] Last-used profile remembered across restarts
- [ ] Multiple profiles work independently
- [ ] No symlinks needed anymore!
- [ ] Build succeeds: `mvn clean package`
- [ ] Manual testing with 2-3 profiles

**Estimated Time:** 3 hours

**Priority:** MEDIUM - Nice quality-of-life improvement for users with multiple accounts

**Philosophy:** Keep it simple - file-based profiles with dropdown selector, no fancy features.
