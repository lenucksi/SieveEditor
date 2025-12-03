# Credential Storage Backends - Status Report

**Date:** 2025-12-02
**Status:** TEMPORARY DEACTIVATION OF BACKENDS

## Executive Summary

All credential storage backends except "Manual Password Entry" have been temporarily deactivated due to being broken and difficult to test. The infrastructure remains in place for future fixes.

## Current State

### Active Backends

| Backend | Class | Status | Reason |
|---------|-------|--------|--------|
| Manual Password Entry | `UserPromptMasterKeyProvider` | ✓ ACTIVE | Always works, user-friendly fallback |

### Deactivated Backends

| Backend | Class | Status | Issues |
|---------|-------|--------|--------|
| KeePassXC | `KeePassXCMasterKeyProvider` | ❌ DEACTIVATED | Broken, hard to test |
| OS Keychain | `OSKeychainMasterKeyProvider` | ❌ DEACTIVATED | Broken, hard to test |

## Technical Details

### What Was Changed

#### 1. MasterKeyProviderFactory.java

**Modified methods:**

- `showBackendSelectionDialog()`:
  - Commented out KeePassXC backend initialization
  - Commented out OS Keychain backend initialization
  - Simplified dialog to show only Manual Password Entry
  - Added informational message about temporarily unavailable backends

- `createProviderByName()`:
  - Redirects KeePassXC requests to UserPromptMasterKeyProvider
  - Redirects OS Keychain requests to UserPromptMasterKeyProvider
  - Logs warnings when redirecting

- `createProviderByBackendArg()`:
  - Redirects all backend arguments to UserPromptMasterKeyProvider
  - Logs warnings when redirecting

**Added documentation:**

- Class-level JavaDoc updated with deactivation status
- References to `dev-docs/CREDENTIAL-BACKENDS-STATUS.md`

#### 2. KeePassXCMasterKeyProviderTest.java

**Changes:**

- Added `@Disabled` annotation to entire test class
- Updated class-level JavaDoc with deactivation status
- Added reference to this documentation file

#### 3. Documentation

**New files:**

- `src/test/java/de/febrildur/sieveeditor/system/credentials/README.md`
- `dev-docs/CREDENTIAL-BACKENDS-STATUS.md` (this file)

### Preserved Infrastructure

All backend implementation classes remain unchanged and ready for future fixes:

- `src/main/java/de/febrildur/sieveeditor/system/credentials/`
  - `MasterKeyProvider.java` - Interface
  - `MasterKeyProviderFactory.java` - Factory (with deactivation code)
  - `KeePassXCMasterKeyProvider.java` - Complete implementation
  - `OSKeychainMasterKeyProvider.java` - Complete implementation
  - `UserPromptMasterKeyProvider.java` - Complete implementation
  - `CredentialException.java` - Exception class

## Known Issues

### KeePassXC Backend

**Problem:**

- Connection and association logic is complex
- Requires KeePassXC running with database unlocked
- Difficult to test automatically in CI/CD

**Documented behavior in tests:**

- `testLockedDatabaseScenario_DocumentsExpectedBehavior()` documents the expected flow
- Implementation exists in source code but is untested

**Dependencies:**

- `org.purejava:keepassxc-proxy-access` library
- KeePassXC application with browser integration enabled

### OS Keychain Backend

**Problem:**

- Platform-specific implementation (Windows/macOS/Linux)
- Difficult to test across all platforms
- Requires system credential manager to be available

**Dependencies:**

- `com.github.javakeyring:java-keyring` library
- Platform-specific credential managers:
  - Windows: Credential Manager (Wincred API)
  - macOS: Keychain
  - Linux: Secret Service API (GNOME Keyring, KWallet)

**No tests currently exist** for this backend.

## User Impact

### What Users See

When starting SieveEditor:

1. **First-time users:**
   - See simplified dialog: "SieveEditor Master Password Storage"
   - Informed they're using "Manual Password Entry"
   - Notified that other backends are "temporarily unavailable"
   - Must enter master password on every application start

2. **Existing users with saved backend preference:**
   - If preference was "Manual Password Entry": Works as before
   - If preference was "KeePassXC" or "OS Keychain": Redirected to Manual Password Entry
   - Warning logged to application logs about backend deactivation

3. **Users with command-line backend argument:**
   - `--backend keepassxc`: Redirected to Manual Password Entry
   - `--backend keychain`: Redirected to Manual Password Entry
   - `--backend prompt`: Works as expected
   - Warning logged about backend deactivation

### Security Implications

**No security regression:**

- Manual Password Entry has always been available as fallback
- User passwords are still encrypted with Jasypt
- Master password is still hashed with SHA-256
- No plaintext password storage

**User inconvenience:**

- Must enter master password on every application start
- Cannot use KeePassXC integration for password sync
- Cannot use OS keychain for automatic password management

## Re-enabling Backends

### Prerequisites for Re-enabling

Before re-enabling any backend:

1. **Fix the implementation:**
   - Address known issues in backend class
   - Test manually with real backend system
   - Document any special requirements

2. **Improve tests:**
   - Add unit tests with mocks where possible
   - Use `@Disabled` for integration tests that require external systems
   - Document test requirements in test class JavaDoc

3. **Update factory:**
   - Uncomment backend initialization in `showBackendSelectionDialog()`
   - Remove redirect logic in `createProviderByName()`
   - Remove redirect logic in `createProviderByBackendArg()`

4. **Update documentation:**
   - Update this file
   - Update `src/test/java/.../credentials/README.md`
   - Update class-level JavaDoc in `MasterKeyProviderFactory.java`
   - Remove `@Disabled` annotation from test classes

### Step-by-Step Re-enabling Process

#### For KeePassXC

1. **Test the implementation:**

   ```bash
   # Ensure KeePassXC is installed and running
   # Unlock a KeePassXC database
   # Enable browser integration in KeePassXC settings
   ```

2. **Fix `MasterKeyProviderFactory.java`:**

   ```java
   // In showBackendSelectionDialog():
   // UNCOMMENT:
   if (keepassXCInstance == null) {
       keepassXCInstance = new KeePassXCMasterKeyProvider();
   }
   if (keepassXCInstance.isAvailable()) {
       availableProviders.add(keepassXCInstance);
   }

   // In createProviderByName():
   // REPLACE redirect logic with original:
   case "KeePassXC" -> {
       if (keepassXCInstance == null) {
           keepassXCInstance = new KeePassXCMasterKeyProvider();
       }
       yield keepassXCInstance;
   }

   // In createProviderByBackendArg():
   // REPLACE redirect logic with original:
   case "keepassxc", "keepass" -> {
       if (keepassXCInstance == null) {
           keepassXCInstance = new KeePassXCMasterKeyProvider();
       }
       yield keepassXCInstance;
   }
   ```

3. **Update tests:**

   ```java
   // In KeePassXCMasterKeyProviderTest.java:
   // REMOVE @Disabled annotation from class
   ```

4. **Update dialog:**
   - Restore original multi-option dialog in `showBackendSelectionDialog()`
   - Remove simplified single-option dialog

5. **Test manually:**
   - Build application
   - Run with KeePassXC backend
   - Verify connection, association, get/set operations

#### For OS Keychain

Follow similar process, but note:

- Test on all supported platforms (Windows, macOS, Linux)
- Create tests if none exist
- Document platform-specific requirements

## Testing Strategy

### Current Approach (Deactivated Backends)

- All tests for deactivated backends are marked with `@Disabled`
- Tests remain in codebase as documentation of expected behavior
- No automated testing occurs in CI/CD

### Recommended Approach (When Re-enabling)

1. **Unit Tests with Mocks:**
   - Mock KeePassXC proxy access
   - Mock OS keychain library
   - Test internal logic without external dependencies

2. **Integration Tests:**
   - Mark with `@Disabled` by default
   - Require manual setup (e.g., KeePassXC running)
   - Document setup requirements in test JavaDoc
   - Use `@EnabledIf` annotations for conditional execution

3. **Manual Testing:**
   - Test on real systems before releases
   - Document test procedures
   - Create test checklists

4. **Use Test Mode:**
   - `MasterKeyProviderFactory.setTestMode()` exists for testing
   - Use in application tests that need credential storage
   - Avoids GUI dialogs during automated tests

## Related Files

### Source Code

- [MasterKeyProviderFactory.java](../src/main/java/de/febrildur/sieveeditor/system/credentials/MasterKeyProviderFactory.java)
- [MasterKeyProvider.java](../src/main/java/de/febrildur/sieveeditor/system/credentials/MasterKeyProvider.java)
- [KeePassXCMasterKeyProvider.java](../src/main/java/de/febrildur/sieveeditor/system/credentials/KeePassXCMasterKeyProvider.java)
- [OSKeychainMasterKeyProvider.java](../src/main/java/de/febrildur/sieveeditor/system/credentials/OSKeychainMasterKeyProvider.java)
- [UserPromptMasterKeyProvider.java](../src/main/java/de/febrildur/sieveeditor/system/credentials/UserPromptMasterKeyProvider.java)

### Tests

- [KeePassXCMasterKeyProviderTest.java](../src/test/java/de/febrildur/sieveeditor/system/credentials/KeePassXCMasterKeyProviderTest.java)
- [Test README](../src/test/java/de/febrildur/sieveeditor/system/credentials/README.md)

### Documentation

- [CLAUDE.md](../CLAUDE.md) - Main development guide

## Timeline

- **2025-12-02:** Backends deactivated, infrastructure preserved
- **Future:** Re-enable when implementations are fixed and tested

## Questions?

- See test class JavaDoc for expected behavior documentation
- See implementation classes for technical details
- See `MasterKeyProviderFactory.java` for factory logic

## Conclusion

This is a **temporary measure** to ensure application stability while backend issues are addressed. The infrastructure remains intact, making it straightforward to re-enable backends once they're fixed and properly tested.

The "Manual Password Entry" backend provides a reliable fallback that all users can use, ensuring SieveEditor remains functional while backend improvements are developed.
