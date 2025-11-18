# Implementation Roadmap: SieveEditor Modernization

## Implementation Status

**Approach Taken:** PRAGMATIC (not full Enterprise 12-week roadmap)

**Reference:** See [05-real-world-issues.md](05-real-world-issues.md) for the pragmatic approach that was followed.

**Detailed Changes:** See [../FIXES-APPLIED.md](../FIXES-APPLIED.md) for complete documentation of all implemented fixes.

### What Was Completed

#### Phase 0: Build System (Partial)

- ✅ Maven updated to current version
- ✅ Build works (`mvn clean package`)
- ✅ UTF-8 encoding configured in pom.xml
- ❌ Test infrastructure NOT added (not needed for pragmatic approach)
- ❌ JaCoCo coverage NOT added (deferred)

#### Phase 1: Security Fixes

- ❌ SSL certificate validation NOT fixed (deferred - documented but not implemented)
- ❌ Hardcoded encryption key NOT fixed (deferred - documented but not implemented)

**Note:** Security issues are documented in analysis but were NOT addressed in the pragmatic approach per user's decision.

#### Phase 2: Bug Fixes

- ✅ Find/Replace completely fixed (ActionReplace.java rewritten with correct event handlers)
- ✅ Tokenizer bug fixed (SieveTokenMaker.java converted from forEach to for-loop)
- ❌ Password field security NOT fixed (still uses JTextField, not JPasswordField)

#### Phase 3: Java Modernization

- ✅ Updated to Java 21 LTS (from Java 11)
- ✅ Updated maven-compiler-plugin to 3.13.0
- ✅ Updated RSyntaxTextArea to 3.5.1
- ✅ Platform encoding fixed in pom.xml

#### Additional Improvements (not in original roadmap)

- ✅ 4K HiDPI scaling fixed (new sieveeditor.sh launcher script)
- ✅ Ctrl+F keyboard shortcut added
- ✅ Enter key search in Find dialog
- ✅ Search wrap-around added

### What Remains

This roadmap below represents the **original 12-week enterprise plan**. It is kept as a reference for potential future work. Most items remain unimplemented as the pragmatic approach focused only on critical user-facing bugs.

For future modernization efforts, this roadmap provides a comprehensive plan for:

- Security fixes (Phase 1-2)
- Testing infrastructure (Phase 4-6)
- Code quality improvements (Phase 5-9)

---

## Executive Summary

This roadmap outlines a 12-week plan to modernize the SieveEditor application, addressing 2 CRITICAL and 4 HIGH severity security vulnerabilities, 9 HIGH severity bugs, and implementing comprehensive test coverage from 0% to 80%.

The approach balances immediate security fixes with sustainable refactoring and comprehensive testing.

## Project Goals

### Primary Goals

1. **Security:** Fix all CRITICAL and HIGH severity security vulnerabilities
2. **Reliability:** Fix all CRITICAL and HIGH severity bugs
3. **Quality:** Achieve 80%+ test coverage
4. **Maintainability:** Refactor for better separation of concerns and testability

### Secondary Goals

1. **Modernization:** Adopt Java 11+ features and best practices
2. **UX:** Improve error messages and user feedback
3. **Documentation:** Create comprehensive developer documentation

## Implementation Phases

### Phase 0: Preparation (Week 1)

**Goal:** Set up infrastructure for development and testing

**Note:** Some items in this phase were completed as part of the pragmatic approach. See "Implementation Status" section at the top of this document.

#### Tasks

**0.1 Development Environment Setup**

- [ ] Set up IDE (Eclipse/IntelliJ) with code formatter
- [ ] Configure Checkstyle for code quality
- [ ] Set up SpotBugs for static analysis
- [ ] Configure OWASP Dependency Check

**0.2 Version Control**

- [ ] Create feature branches:
  - `feature/security-fixes`
  - `feature/bug-fixes`
  - `feature/testing-infrastructure`
  - `feature/refactoring`
- [ ] Set up branch protection rules on main/master
- [ ] Require code reviews for merges

**0.3 Build System**

- [x] Update Maven to latest version (✅ DONE - see FIXES-APPLIED.md)
- [ ] Add test dependencies (JUnit 5, Mockito, AssertJ) (⚠️ DEFERRED - pragmatic approach)
- [ ] Configure Surefire and Failsafe plugins (⚠️ DEFERRED - pragmatic approach)
- [ ] Set up JaCoCo for code coverage (⚠️ DEFERRED - pragmatic approach)
- [x] Configure assembly plugin for releases (✅ DONE - build produces JAR with dependencies)

**0.4 CI/CD Pipeline**

- [ ] Create GitHub Actions workflow for:
  - Unit tests on every push
  - Integration tests on PR
  - Security scans (OWASP Dependency Check)
  - Code coverage reporting (Codecov)
  - Release builds on tags

**0.5 Documentation**

- [ ] Create CONTRIBUTING.md
- [ ] Create ARCHITECTURE.md
- [ ] Create SECURITY.md (vulnerability reporting)
- [ ] Update README.md with build instructions

**Deliverables:**

- Working CI/CD pipeline
- Development environment guide
- All tooling configured

---

### Phase 1: Critical Security Fixes (Week 2)

**Goal:** Fix the 2 CRITICAL security vulnerabilities

**Status:** ⚠️ DEFERRED - Security fixes were documented but NOT implemented in the pragmatic approach per user's decision. These remain as future work.

#### 1.1 Fix SSL Certificate Validation (CRITICAL)

**File:** [ConnectAndListScripts.java:97-121](../../../src/main/java/de/febrildur/sieveeditor/system/ConnectAndListScripts.java#L97-L121)

**Status:** ⚠️ DEFERRED - Security issues documented but NOT addressed in pragmatic approach per user's decision

**Tasks:**

- [ ] Remove `getInsecureSSLFactory()` method entirely
- [ ] Use default `SSLSocketFactory.getDefault()`
- [ ] Add proper exception handling for certificate validation errors
- [ ] Update ManageSieveClient to use secure SSL
- [ ] Test with valid SSL certificate
- [ ] Test with self-signed certificate (should fail)
- [ ] Test with expired certificate (should fail)
- [ ] Document how users can add custom CA certificates if needed

**Code Changes:**

```java
// REMOVE this entire method
public static SSLSocketFactory getInsecureSSLFactory() { ... }

// UPDATE connect method to use default SSL
public void connect(String server, int port, String username, String password)
        throws IOException, ParseException {
    validateParameters(server, port, username, password);

    client = new ManageSieveClient();
    ManageSieveResponse resp = client.connect(server, port);
    if (!resp.isOk()) {
        client = null;
        throw new IOException("Can't connect to server: " + resp.getMessage());
    }

    // Use default SSL - proper certificate validation
    resp = client.starttls();
    if (!resp.isOk()) {
        client = null;
        throw new IOException("Can't start SSL: " + resp.getMessage());
    }

    resp = client.authenticate(username, password);
    if (!resp.isOk()) {
        client = null;
        throw new IOException("Could not authenticate: " + resp.getMessage());
    }
}
```

**Tests:**

- [ ] SSLValidationTest.shouldRejectSelfSignedCertificates()
- [ ] SSLValidationTest.shouldRejectExpiredCertificates()
- [ ] SSLValidationTest.shouldAcceptValidCertificates()

#### 1.2 Fix Hardcoded Encryption Key (CRITICAL)

**File:** [PropertiesSieve.java:29](../../../src/main/java/de/febrildur/sieveeditor/system/PropertiesSieve.java#L29)

**Status:** ⚠️ DEFERRED - Security issues documented but NOT addressed in pragmatic approach per user's decision

**Tasks:**

- [ ] Remove hardcoded encryption key
- [ ] Implement OS-specific credential storage:
  - Windows: Use `Advapi32` (DPAPI) via JNA
  - macOS: Use Keychain via `security` command
  - Linux: Use Secret Service API via `libsecret`
- [ ] Create `CredentialStore` interface
- [ ] Implement platform-specific credential stores
- [ ] Fallback: Prompt for master password if OS storage unavailable
- [ ] Migrate existing encrypted passwords (one-time migration)
- [ ] Update encryption algorithm to PBEWithHmacSHA512AndAES_256

**Code Changes:**

```java
// NEW: CredentialStore.java
public interface CredentialStore {
    void store(String key, char[] value) throws IOException;
    char[] retrieve(String key) throws IOException;
    void delete(String key) throws IOException;
    boolean isAvailable();
}

// NEW: WindowsCredentialStore.java
public class WindowsCredentialStore implements CredentialStore {
    // Use JNA to call Windows DPAPI
    // CredWrite, CredRead, CredDelete
}

// NEW: MacOSCredentialStore.java
public class MacOSCredentialStore implements CredentialStore {
    // Use ProcessBuilder to call 'security' command
    // security add-generic-password
    // security find-generic-password
}

// NEW: LinuxCredentialStore.java
public class LinuxCredentialStore implements CredentialStore {
    // Use Secret Service API via dbus
    // or fallback to gnome-keyring
}

// NEW: CredentialStoreFactory.java
public class CredentialStoreFactory {
    public static CredentialStore createDefault() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new WindowsCredentialStore();
        } else if (os.contains("mac")) {
            return new MacOSCredentialStore();
        } else if (os.contains("linux")) {
            return new LinuxCredentialStore();
        }
        return new MasterPasswordCredentialStore(); // Fallback
    }
}

// UPDATED: PropertiesSieve.java
public class PropertiesSieve implements SieveConfiguration {
    private final CredentialStore credentialStore;
    private String server;
    private int port;
    private String username;

    public PropertiesSieve() {
        this.credentialStore = CredentialStoreFactory.createDefault();
    }

    public String getPassword() throws IOException {
        char[] password = credentialStore.retrieve("sieve.password");
        return new String(password); // TODO: Return char[] instead
    }

    public void setPassword(String password) throws IOException {
        credentialStore.store("sieve.password", password.toCharArray());
    }
}
```

**Dependencies to Add:**

```xml
<!-- For Windows DPAPI -->
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna-platform</artifactId>
    <version>5.13.0</version>
</dependency>
```

**Tests:**

- [ ] EncryptionTest.shouldStorePasswordSecurely()
- [ ] EncryptionTest.shouldRetrievePasswordCorrectly()
- [ ] EncryptionTest.shouldNotHaveHardcodedKey()
- [ ] CredentialStoreTest for each platform

**Deliverables:**

- SSL certificate validation enabled
- No hardcoded encryption keys
- Platform-specific credential storage
- All CRITICAL security tests passing

---

### Phase 2: High Priority Security Fixes (Week 3)

**Goal:** Fix remaining HIGH severity security vulnerabilities

**Status:** ⚠️ DEFERRED - Security fixes were documented but NOT implemented in the pragmatic approach per user's decision. These remain as future work.

#### 2.1 Fix Password Field Display (HIGH)

**File:** [ActionConnect.java:59](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionConnect.java#L59)

**Status:** ⚠️ DEFERRED - Security issues documented but NOT addressed in pragmatic approach per user's decision

**Tasks:**

- [ ] Replace `JTextField` with `JPasswordField`
- [ ] Use `getPassword()` method (returns `char[]`)
- [ ] Clear char[] after use
- [ ] Test password not visible on screen

**Code Changes:**

```java
// BEFORE
JTextField tfPassword = new JTextField(parentFrame.getProp().getPassword(), 15);

// AFTER
JPasswordField tfPassword = new JPasswordField(15);
// Don't pre-fill password for security
// Or if needed:
String savedPassword = parentFrame.getProp().getPassword();
if (savedPassword != null && !savedPassword.isEmpty()) {
    tfPassword.setText(savedPassword);
}

// In button handler:
char[] password = tfPassword.getPassword();
try {
    parentFrame.getProp().setPassword(new String(password));
    // ... use password
} finally {
    Arrays.fill(password, '\0'); // Clear password from memory
}
```

**Tests:**

- [ ] ActionConnectTest.shouldUsePasswordField()
- [ ] ActionConnectTest.shouldClearPasswordAfterUse()

#### 2.2 Upgrade SSL Protocol (HIGH)

**File:** [ConnectAndListScripts.java:115](../../../src/main/java/de/febrildur/sieveeditor/system/ConnectAndListScripts.java#L115)

**Status:** ⚠️ DEFERRED - Security issues documented but NOT addressed in pragmatic approach per user's decision

**Tasks:**

- [ ] Change from generic "SSL" to "TLSv1.3" or "TLSv1.2"
- [ ] Configure cipher suites (disallow weak ciphers)
- [ ] Test TLS version negotiation

**Code Changes:**

```java
// BEFORE
SSLContext sc = SSLContext.getInstance("SSL");

// AFTER
SSLContext sc = SSLContext.getInstance("TLSv1.3");
// Fallback to TLSv1.2 if TLSv1.3 not available
```

#### 2.3 Use char[] for Passwords (HIGH)

**Files:** Multiple

**Status:** ⚠️ DEFERRED - Security issues documented but NOT addressed in pragmatic approach per user's decision

**Tasks:**

- [ ] Change `String password` to `char[] password` in:
  - ConnectAndListScripts.connect()
  - PropertiesSieve getter/setter
  - ActionConnect handler
- [ ] Clear char[] arrays after use
- [ ] Update all call sites

**Code Changes:**

```java
// PropertiesSieve.java
public char[] getPassword() throws IOException {
    return credentialStore.retrieve("sieve.password");
}

public void setPassword(char[] password) throws IOException {
    try {
        credentialStore.store("sieve.password", password);
    } finally {
        Arrays.fill(password, '\0');
    }
}

// ConnectAndListScripts.java
public void connect(String server, int port, String username, char[] password)
        throws IOException, ParseException {
    try {
        // ... use password
        String passwordString = new String(password);
        resp = client.authenticate(username, passwordString);
    } finally {
        Arrays.fill(password, '\0');
    }
}
```

#### 2.4 Upgrade Encryption Algorithm (HIGH)

**File:** [PropertiesSieve.java:28](../../../src/main/java/de/febrildur/sieveeditor/system/PropertiesSieve.java#L28)

**Status:** ⚠️ DEFERRED - Security issues documented but NOT addressed in pragmatic approach per user's decision

**Tasks:**

- [ ] Note: This is now handled by OS credential store
- [ ] If fallback encryption needed, use strong algorithm
- [ ] Document algorithm choice

**Deliverables:**

- All HIGH security vulnerabilities fixed
- Security tests passing
- Updated documentation

---

### Phase 3: Critical Bug Fixes (Week 4)

**Goal:** Fix all CRITICAL and HIGH severity bugs

**Status:** ✅ PARTIALLY DONE - Critical bugs (Find/Replace and Tokenizer) were fixed. Other bugs remain unaddressed.

#### 3.1 Fix Find/Replace Functionality (CRITICAL)

**File:** [ActionReplace.java:48-49, 77](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionReplace.java#L48-L49)

**Status:** ✅ DONE - See FIXES-APPLIED.md

**Tasks:**

- [x] Attach event handlers to correct buttons (✅ DONE)
- [x] Remove searchField listener (✅ DONE)
- [x] Add "Find Next" button handler (✅ DONE)
- [x] Add "Find Previous" button handler (✅ DONE)
- [x] Add keyboard shortcuts (Enter, Shift+Enter) (✅ DONE - Enter triggers Find Next, also added Ctrl+F)
- [x] Test find functionality works (✅ DONE - confirmed by user)

**Code Changes:**

```java
// Remove searchField listener
// searchField.addActionListener(...); // DELETE THIS

// Fix button handlers
nextButton.addActionListener((event) -> {
    performSearch(true); // forward
});

prevButton.addActionListener((event) -> {
    performSearch(false); // backward
});

// Extract search logic
private void performSearch(boolean forward) {
    String text = searchField.getText();
    if (text.length() == 0) {
        JOptionPane.showMessageDialog(frame,
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

    boolean found = SearchEngine.find(parentFrame.getTextArea(), context).wasFound();
    if (!found) {
        JOptionPane.showMessageDialog(frame, "Text not found");
    }
}
```

**Tests:**

- [ ] ActionReplaceTest.shouldFindNextWhenButtonClicked()
- [ ] ActionReplaceTest.shouldFindPreviousWhenButtonClicked()
- [ ] ActionReplaceTest.shouldShowErrorForEmptySearch()

#### 3.2 Fix Wrong Error Message (CRITICAL)

**File:** [ActionActivateDeactivateScript.java:91](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionActivateDeactivateScript.java#L91)

**Code Changes:**

```java
// BEFORE
JOptionPane.showMessageDialog(parentFrame, "deactivate all scripts");

// AFTER
JOptionPane.showMessageDialog(parentFrame, "Script renamed to: " + newname);
```

#### 3.3 Fix All NullPointerExceptions (HIGH)

**Files:** Application.java, ActionSaveScriptAs.java, ActionCheckScript.java, ActionLoadScript.java

**Tasks:**

- [ ] Add null checks before all server operations
- [ ] Add null checks before all script operations
- [ ] Validate method parameters
- [ ] Show user-friendly error messages

**Code Changes:**

```java
// Application.java
public void setScript(SieveScript script) throws IOException, ParseException {
    if (server == null) {
        throw new IllegalStateException("Not connected to server");
    }
    if (script == null) {
        throw new IllegalArgumentException("Script cannot be null");
    }
    this.script = script;
    textArea.setText(server.getScript(script));
}

public void save() {
    if (script == null) {
        JOptionPane.showMessageDialog(this,
            "No script loaded",
            "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }
    save(script.getName());
}

public boolean save(String name) {
    if (server == null) {
        JOptionPane.showMessageDialog(this,
            "Not connected to server",
            "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }
    if (name == null || name.isBlank()) {
        JOptionPane.showMessageDialog(this,
            "Script name cannot be empty",
            "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }

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
```

#### 3.4 Fix Save Always Shows Success (HIGH)

**File:** [ActionSaveScript.java:21-23](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionSaveScript.java#L21-L23)

**Tasks:**

- [ ] Change Application.save() to return boolean
- [ ] Only show success message if save actually succeeded
- [ ] Show error message if save failed

**Code Changes:**

```java
// ActionSaveScript.java
public void actionPerformed(ActionEvent e) {
    if (parentFrame.save()) {
        parentFrame.updateStatus();
        JOptionPane.showMessageDialog(parentFrame, "Script saved.");
    }
    // Error already shown by Application.save()
}
```

#### 3.5 Fix Array Index Out of Bounds (HIGH)

**File:** [ActionActivateDeactivateScript.java:57, 83](../../../src/main/java/de/febrildur/sieveeditor/actions/ActionActivateDeactivateScript.java#L57)

**Code Changes:**

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
    // ... rest of logic
});
```

#### 3.6 Fix Tokenization Loop (HIGH)

**File:** [SieveTokenMaker.java:176](../../../src/main/java/de/febrildur/sieveeditor/system/SieveTokenMaker.java#L176)

**Status:** ✅ DONE - See FIXES-APPLIED.md

**Tasks:**

- [x] Replace IntStream.forEach with traditional for-loop (✅ DONE)
- [x] Remove AtomicInteger wrappers (✅ DONE)
- [x] Test tokenization works correctly (✅ DONE - fixed last character unreachable issue)

**Code Changes:**

```java
// BEFORE
IntStream.range(offset, end).forEach(i -> {
    AtomicInteger currentTokenStart = new AtomicInteger(offset);
    AtomicInteger currentTokenType = new AtomicInteger(startTokenType);
    // ... 160 lines of lambda code
    i--; // This doesn't work!
});

// AFTER
int currentTokenStart = offset;
int currentTokenType = startTokenType;

for (int i = offset; i < end; i++) {
    char c = array[i];

    switch (currentTokenType) {
        case TokenTypes.NULL:
            // ...
            break;

        case TokenTypes.LITERAL_NUMBER_DECIMAL_INT:
            // ...
            i--; // Now this works!
            currentTokenType = TokenTypes.NULL;
            break;

        // ... other cases
    }
}
```

**Tests:**

- [ ] SieveTokenMakerTest.shouldCorrectlyTokenizeNumbers()
- [ ] SieveTokenMakerTest.shouldHandleNumbersFollowedByLetters()

**Deliverables:**

- All CRITICAL bugs fixed
- All HIGH bugs fixed
- Bug regression tests passing
- Release notes documenting fixes

---

### Phase 4: Testing Infrastructure & Unit Tests (Week 5-6)

**Goal:** Achieve 40% test coverage with focus on critical paths

#### 4.1 Refactor for Testability

**Tasks:**

- [ ] Create interfaces: SieveServerConnection, SieveConfiguration, DialogFactory
- [ ] Implement dependency injection in Application class
- [ ] Extract business logic from UI classes
- [ ] Create test doubles (mocks, stubs, fakes)

#### 4.2 Write Unit Tests

**Priority 1: Security & Bug Fixes**

- [ ] ConnectAndListScriptsTest (80% coverage target)
  - Connection validation
  - SSL/TLS handling
  - Error handling
- [ ] PropertiesSieveTest (80% coverage target)
  - Load/save configuration
  - Credential storage
  - Validation
- [ ] All Action tests (70% coverage target)
  - ActionConnectTest
  - ActionSaveScriptTest
  - ActionSaveScriptAsTest
  - ActionLoadScriptTest
  - ActionCheckScriptTest
  - ActionReplaceTest
  - ActionActivateDeactivateScriptTest

**Priority 2: Core Logic**

- [ ] ApplicationTest (60% coverage target)
  - Save/load operations
  - Script management
  - State management
- [ ] SieveTokenMakerTest (60% coverage target)
  - Tokenization correctness
  - Edge cases

**Test Count Target:** 80+ unit tests

**Deliverables:**

- 40%+ code coverage
- All critical paths tested
- CI/CD running tests automatically

---

### Phase 5: Medium Priority Bugs & Improvements (Week 7-8)

**Goal:** Fix MEDIUM severity bugs and improve code quality

#### 5.1 Fix Resource Leaks

**Tasks:**

- [ ] Fix dialog not disposed (ActionConnect.java:73)
- [ ] Fix dialog created before operations (ActionLoadScript.java:47)
- [ ] Implement try-with-resources where applicable
- [ ] Add AutoCloseable to relevant classes

#### 5.2 Improve Input Validation

**Tasks:**

- [ ] Validate script names (ActionSaveScriptAs.java)
- [ ] Validate empty script list (ActionLoadScript.java)
- [ ] Validate port numbers (PropertiesSieve.java)
- [ ] Validate server parameters (ConnectAndListScripts.java)

#### 5.3 Improve User Feedback

**Tasks:**

- [ ] Consistent error messages
- [ ] Show operation progress
- [ ] Refresh data after operations
- [ ] Better empty state handling

#### 5.4 Code Quality Improvements

**Tasks:**

- [ ] Fix inconsistent exception handling
- [ ] Fix popup menu platform compatibility
- [ ] Fix wrong dialog titles
- [ ] Remove hardcoded strings (i18n preparation)

**Deliverables:**

- All MEDIUM bugs fixed
- Improved error handling
- Better user experience

---

### Phase 6: Integration & End-to-End Tests (Week 9)

**Goal:** Test component interactions and full workflows

#### 6.1 Integration Tests

**Tasks:**

- [ ] Set up TestContainers for Cyrus IMAP server
- [ ] Create MockSieveServer for controlled testing
- [ ] Test full connection workflow
- [ ] Test script upload/download cycle
- [ ] Test script activation/deactivation
- [ ] Test error recovery

**Tests:**

- [ ] ServerConnectionIT
- [ ] ScriptManagementIT
- [ ] ErrorHandlingIT

#### 6.2 GUI Tests

**Tasks:**

- [ ] Set up AssertJ-Swing
- [ ] Test main window initialization
- [ ] Test menu interactions
- [ ] Test dialog flows
- [ ] Test keyboard shortcuts

**Tests:**

- [ ] ApplicationGUITest
- [ ] ConnectDialogTest
- [ ] LoadScriptDialogTest
- [ ] FindReplaceDialogTest

#### 6.3 End-to-End Tests

**Tasks:**

- [ ] Test complete user workflows:
  - Connect → Load → Edit → Save
  - Connect → Load → Check → Fix → Save
  - Connect → View Scripts → Activate
  - Connect → Rename Script

**Tests:**

- [ ] EndToEndIT

**Deliverables:**

- 60%+ code coverage
- Integration tests passing
- GUI tests passing

---

### Phase 7: Modernization & Java 11+ Features (Week 10)

**Goal:** Adopt modern Java features and best practices

**Status:** ✅ PARTIALLY DONE - Java updated to 21 LTS, dependencies updated. Code modernization not performed.

#### 7.1 Java 11+ Features

**Tasks:**

- [ ] Replace anonymous inner classes with lambdas
- [ ] Use var for local variables
- [ ] Use String.isBlank() instead of isEmpty()
- [ ] Use List.of(), Set.of(), Map.of()
- [ ] Use Optional for nullable returns
- [ ] Use try-with-resources
- [ ] Use Stream API where appropriate (not in loops!)

**Examples:**

```java
// Lambda
SwingUtilities.invokeLater(() -> new Application().setVisible(true));

// var
var menu = new JMenuBar();
var textArea = new RSyntaxTextArea();

// Optional
public Optional<SieveScript> getCurrentScript() {
    return Optional.ofNullable(script);
}

// Collections
var keywords = List.of("if", "elsif", "else", "require");
```

#### 7.2 Design Patterns

**Tasks:**

- [ ] Implement Factory pattern for dialog creation
- [ ] Implement Strategy pattern for credential storage
- [ ] Implement Observer pattern for connection status
- [ ] Implement Command pattern for actions (if beneficial)

#### 7.3 Dependency Updates

**Tasks:**

- [x] Update RSyntaxTextArea to latest version (✅ DONE - updated to 3.5.1, see FIXES-APPLIED.md)
- [ ] Update ManageSieveJ (check for updates)
- [ ] Update Jasypt (or remove if using OS credential store)
- [x] Update all Maven plugins (✅ DONE - maven-compiler-plugin updated to 3.13.0)
- [ ] Run OWASP Dependency Check
- [ ] Fix any vulnerable dependencies

**Deliverables:**

- Modern Java code
- Updated dependencies
- No known vulnerabilities in dependencies

---

### Phase 8: Enhanced Features & Polish (Week 11)

**Goal:** Add missing features and polish the application

#### 8.1 Enhanced Syntax Highlighting

**File:** [SieveTokenMaker.java](../../../src/main/java/de/febrildur/sieveeditor/system/SieveTokenMaker.java)

**Tasks:**

- [ ] Add all Sieve keywords (not just "if")
- [ ] Add Sieve extensions (fileinto, vacation, etc.)
- [ ] Support multi-line strings
- [ ] Support comments (#)
- [ ] Highlight syntax errors
- [ ] Add code folding regions

**Keywords to Add:**

- Control: if, elsif, else, require, stop
- Tests: address, allof, anyof, exists, header, not, size, true, false
- Actions: fileinto, redirect, keep, discard, reject
- Extensions: vacation, imap4flags, relational, etc.

#### 8.2 Enhanced Error Messages

**Tasks:**

- [ ] User-friendly error messages (not exception class names)
- [ ] Suggest solutions for common errors
- [ ] Link to help documentation
- [ ] Log detailed errors to file

#### 8.3 Missing Functionality

**Tasks:**

- [ ] Add "Replace" functionality to Find/Replace dialog
- [ ] Add "Replace All" functionality
- [x] Add wrap-around search option (✅ DONE - see FIXES-APPLIED.md)
- [ ] Add recent servers list
- [ ] Add script templates
- [x] Add keyboard shortcuts guide (✅ PARTIAL - Ctrl+F added, guide not created)

#### 8.4 Logging

**Tasks:**

- [ ] Add SLF4J + Logback
- [ ] Log all connections
- [ ] Log all authentications (success/failure)
- [ ] Log all script operations
- [ ] Log errors with stack traces
- [ ] Configurable log level

**Deliverables:**

- Complete Sieve syntax support
- Better error messages
- Logging framework
- Enhanced features

---

### Phase 9: Final Testing & Documentation (Week 12)

**Goal:** Achieve 80% coverage and complete documentation

#### 9.1 Final Testing Push

**Tasks:**

- [ ] Write tests for all remaining uncovered code
- [ ] Achieve 80%+ code coverage
- [ ] Fix any failing tests
- [ ] Run mutation testing (PIT)
- [ ] Performance testing
- [ ] Memory leak testing

**Coverage Targets:**

- ConnectAndListScripts: 90%
- PropertiesSieve: 90%
- Application: 80%
- All actions: 80%
- SieveTokenMaker: 80%
- Overall: 80%+

#### 9.2 Security Audit

**Tasks:**

- [ ] Run OWASP Dependency Check
- [ ] Run SpotBugs security analysis
- [ ] Manual security review
- [ ] Penetration testing (SSL, auth, injection)
- [ ] Create SECURITY.md with vulnerability reporting

#### 9.3 Documentation

**Tasks:**

- [ ] Update README.md with:
  - Build instructions
  - Usage guide
  - Configuration guide
  - Troubleshooting
- [ ] Create ARCHITECTURE.md
- [ ] Create CONTRIBUTING.md
- [ ] JavaDoc for all public APIs
- [ ] Create user guide
- [ ] Create developer guide

#### 9.4 Release Preparation

**Tasks:**

- [ ] Update version to 1.0.0
- [ ] Create CHANGELOG.md
- [ ] Create release notes
- [ ] Build release artifacts
- [ ] Test release on all platforms (Windows, macOS, Linux)
- [ ] Create installation guide
- [ ] Update GitHub releases

**Deliverables:**

- 80%+ test coverage
- Complete documentation
- Release candidate

---

## Success Metrics

### Security Metrics

- [ ] 0 CRITICAL vulnerabilities (down from 2)
- [ ] 0 HIGH vulnerabilities (down from 4)
- [ ] SSL certificate validation enabled
- [ ] No hardcoded secrets
- [ ] OWASP Dependency Check clean

### Quality Metrics

- [ ] 0 CRITICAL bugs (down from 2)
- [ ] 0 HIGH bugs (down from 9)
- [ ] 80%+ code coverage (up from 0%)
- [ ] 0 SpotBugs high-priority issues
- [ ] 0 Checkstyle violations (with reasonable rules)

### Process Metrics

- [ ] All tests pass in CI/CD
- [ ] Code review required for all changes
- [ ] Automated security scanning
- [ ] Automated dependency updates (Dependabot)

### Documentation Metrics

- [ ] README.md complete
- [ ] ARCHITECTURE.md complete
- [ ] CONTRIBUTING.md complete
- [ ] SECURITY.md complete
- [ ] User guide complete
- [ ] JavaDoc for all public APIs

---

## Risk Management

### High Risks

**Risk 1: Breaking Existing Functionality**

- **Mitigation:** Write tests before refactoring
- **Mitigation:** Maintain backward compatibility
- **Mitigation:** Incremental changes with testing

**Risk 2: OS-Specific Credential Storage Issues**

- **Mitigation:** Test on all target platforms
- **Mitigation:** Provide fallback master password option
- **Mitigation:** Clear documentation for users

**Risk 3: ManageSieveJ Library Limitations**

- **Mitigation:** Research library capabilities early
- **Mitigation:** Consider contributing patches
- **Mitigation:** Consider forking if necessary

**Risk 4: Time Overruns**

- **Mitigation:** Prioritize security and critical bugs
- **Mitigation:** Move nice-to-have features to Phase 10
- **Mitigation:** Regular progress reviews

### Medium Risks

**Risk 5: Test Complexity with Swing**

- **Mitigation:** Use AssertJ-Swing
- **Mitigation:** Focus on unit tests over GUI tests
- **Mitigation:** Separate business logic from UI

**Risk 6: Incomplete Test Coverage**

- **Mitigation:** Set incremental goals (40%, 60%, 80%)
- **Mitigation:** Focus on critical paths first
- **Mitigation:** Use JaCoCo to track progress

---

## Dependencies & Prerequisites

### Tools Required

- Java 11+ JDK
- Maven 3.6+
- Git
- IDE (Eclipse/IntelliJ)

### Platform-Specific

- **Windows:** Windows 10+ for DPAPI
- **macOS:** macOS 10.14+ for Keychain
- **Linux:** libsecret-1-dev for credential storage

### Team Skills

- Java development
- Swing/AWT GUI programming
- Unit testing (JUnit, Mockito)
- Security best practices
- Maven build system

---

## Maintenance Plan

### Post-Release (Ongoing)

**Monthly:**

- [ ] Run OWASP Dependency Check
- [ ] Update dependencies
- [ ] Review security advisories

**Quarterly:**

- [ ] Review and update documentation
- [ ] Evaluate new Java features
- [ ] Performance profiling

**Yearly:**

- [ ] Security audit
- [ ] Dependency major version updates
- [ ] Code quality review

---

## Conclusion

This 12-week roadmap provides a structured approach to modernizing the SieveEditor application. By prioritizing security fixes and critical bugs first, we ensure the application becomes safe to use early in the process. The phased approach allows for incremental progress while maintaining a working application throughout the modernization effort.

The focus on testing ensures that improvements don't introduce regressions, and the final phases on modernization and polish make the codebase maintainable for the long term.
