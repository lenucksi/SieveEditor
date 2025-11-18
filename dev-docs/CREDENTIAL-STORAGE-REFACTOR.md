# Credential Storage System Refactor

**Date:** 2025-11-18
**Author:** Claude Code
**Status:** Implemented, Bug Fixes in Progress

## Executive Summary

Replaced broken machine-specific encryption key derivation with a secure, cross-platform credential storage system supporting multiple backends: KeePassXC, OS Keychains, and manual password entry.

## Problem Statement

### Original Implementation Issues

1. **Broken Key Derivation**
   - Used hostname lookup via `InetAddress.getLocalHost().getHostName()`
   - Failed on systems with DNS issues: `UnknownHostException: archtest: Name or service not known`
   - Attempted MAC address retrieval as fallback, also failed
   - Resulted in weak fallback keys like `"localhost"` and `"NO-MAC-ADDRESS"`

2. **Non-Portable Storage**
   - Hardcoded `~/.sieveprofiles` directory
   - Violated platform conventions:
     - Linux: Should use XDG Base Directory Specification
     - Windows: Should use `%LOCALAPPDATA%`
     - macOS: Should use `~/Library/Application Support`

3. **No Cross-Machine Sync**
   - Machine-specific keys prevented profile portability
   - Users couldn't sync configurations across devices

## Solution Architecture

### High-Level Design

```
┌─────────────────────────────────────────────────────┐
│           MasterKeyProviderFactory                  │
│  (Auto-detection, Selection Dialog, Preference)    │
└──────────────┬──────────────────────────────────────┘
               │
      ┌────────┴────────┬─────────────┐
      │                 │             │
┌─────▼──────┐  ┌──────▼──────┐  ┌──▼──────────┐
│ KeePassXC  │  │ OS Keychain │  │ User Prompt │
│  Backend   │  │   Backend   │  │   Backend   │
└────────────┘  └─────────────┘  └─────────────┘
      │                 │             │
      │                 │             │
      └────────┬────────┴─────────────┘
               │
         Master Key (256-bit random)
               │
               ▼
      Jasypt PBE Encryptor
      (AES-256 or TripleDES)
               │
               ▼
     Encrypted Server Passwords
  (Stored in profile .properties files)
```

### Component Overview

#### 1. MasterKeyProvider Interface

```java
public interface MasterKeyProvider {
    boolean isAvailable();
    String getMasterKey() throws CredentialException;
    void setMasterKey(String masterKey) throws CredentialException;
    String getName();
    String getDescription();
    void close();
}
```

**Implementations:**

- `KeePassXCMasterKeyProvider` - Stores master key in KeePassXC database entry
- `OSKeychainMasterKeyProvider` - Uses OS credential managers via java-keyring
- `UserPromptMasterKeyProvider` - Prompts user on each startup (memory-only)

#### 2. AppDirectoryService

Platform-specific directory resolution using `net.harawata:appdirs`:

| Platform | Config Directory | Data Directory |
|----------|-----------------|----------------|
| **Linux** | `~/.config/sieveeditor` | `~/.local/share/sieveeditor` |
| **Windows** | `%LOCALAPPDATA%\febrildur\sieveeditor` | `%LOCALAPPDATA%\febrildur\sieveeditor` |
| **macOS** | `~/Library/Preferences/sieveeditor` | `~/Library/Application Support/sieveeditor` |

**Key Features:**
- Auto-creates directories with secure permissions (700/600 on POSIX)
- Auto-migrates from legacy `~/.sieveprofiles`
- Respects `XDG_CONFIG_HOME` and `XDG_DATA_HOME` environment variables

#### 3. MasterKeyProviderFactory

**Responsibilities:**
- Auto-detects available backends
- Shows selection dialog on first run
- Saves user preference to `<config-dir>/.storage-backend`
- Auto-loads saved preference on subsequent runs
- Handles backend unavailability gracefully

**Selection Dialog:**
```
┌─────────────────────────────────────────────────┐
│ How should SieveEditor store your master       │
│ password?                                       │
│                                                 │
│ ⦿ KeePassXC                                    │
│   Store master key in your KeePassXC database │
│   (most secure, syncs across devices)         │
│                                                 │
│ ○ Linux Secret Service                         │
│   Store master key in GNOME Keyring or        │
│   KWallet (Linux Secret Service)              │
│                                                 │
│ ○ Manual Password Entry                        │
│   Enter master password manually each time    │
│   (not stored)                                 │
│                                                 │
│              [OK]    [Cancel]                   │
└─────────────────────────────────────────────────┘
```

## Implementation Details

### Backend: KeePassXC

**Library:** `org.purejava:keepassxc-proxy-access:1.3.0`

**How It Works:**
1. Connects to KeePassXC via native messaging proxy
2. Associates with KeePassXC (user approves connection)
3. Stores/retrieves master key as password in KeePassXC entry:
   - Title: `SieveEditor Master Key`
   - URL: `sieveeditor://master-key`
   - Username: `sieveeditor`
   - Password: `<256-bit random master key>`

**Advantages:**
- Most secure (leverages KeePassXC's strong encryption)
- Syncs across devices (if KeePassXC database is synced)
- No plaintext storage

**Requirements:**
- KeePassXC 2.6.0+ installed and running
- Browser integration enabled in KeePassXC settings
- Database must be unlocked for association

**Known Issues (Being Fixed):**
- Association fails if database is locked
- No user prompt to unlock database before association attempt
- See "Current Bugs" section below

### Backend: OS Keychain

**Library:** `com.github.javakeyring:java-keyring:1.0.4`

**Platform Implementations:**
- **Linux:** D-Bus Secret Service API (GNOME Keyring, KWallet)
- **macOS:** Keychain Services via JNA/jkeychain
- **Windows:** Credential Manager via Wincred API

**Storage:**
- Service: `SieveEditor`
- Account: `master-encryption-key`
- Password: `<256-bit random master key>`

**Advantages:**
- Native OS integration
- No extra software required
- Platform-standard security

**Limitations:**
- Security varies by OS and app packaging
- No automatic cross-device sync
- Linux: Requires session unlock (no additional security when logged in)

### Backend: User Prompt

**Implementation:** Pure Java (Swing JPasswordField)

**How It Works:**
1. Shows password dialog on startup
2. Hashes password with SHA-256 to create consistent-length master key
3. Stores in memory only for session duration
4. Cleared on application exit

**Advantages:**
- Always available (ultimate fallback)
- No dependencies
- User controls security (strong password = strong encryption)

**Limitations:**
- User must remember password (unrecoverable if forgotten)
- Prompted every startup

### Master Key Generation

On first run (when no master key exists):
```java
SecureRandom random = new SecureRandom();
byte[] keyBytes = new byte[32]; // 256 bits
random.nextBytes(keyBytes);
String masterKey = Base64.getEncoder().encodeToString(keyBytes);
```

This cryptographically random key is then stored via the selected backend.

### Encryption Algorithm Selection

PropertiesSieve tries algorithms in order of strength:

**Tier 1 - AES (Strongest):**
- `PBEWITHHMACSHA512ANDAES_256`
- `PBEWITHHMACSHA256ANDAES_256`
- Requires `RandomIvGenerator`
- 10,000 key obtention iterations

**Tier 2 - TripleDES (Strong, Compatible):**
- `PBEWithSHA1AndDESede`
- `PBEWithMD5AndTripleDES`
- No IV generator needed

**Tier 3 - DES (Weak, Last Resort):**
- `PBEWithMD5AndDES`
- Logs warning if used

## Migration Strategy

### Automatic Migration

On first run, `PropertiesSieve.migrateOldProperties()`:

1. Checks if `~/.sieveprofiles` exists
2. Copies all `*.properties` files to new location
3. Copies `.lastused` file to new config directory
4. Sets secure permissions (600) on copied files
5. Leaves original files intact (user can delete manually)

### Flatpak Considerations

Updated `de.febrildur.sieveeditor.yml`:

```yaml
finish-args:
  # D-Bus access for Secret Service API (GNOME Keyring/KWallet)
  - --talk-name=org.freedesktop.secrets

  # XDG-compliant directories
  - --filesystem=xdg-data/sieveeditor:create
  - --filesystem=xdg-config/sieveeditor:create

  # Legacy migration (read-only)
  - --filesystem=~/.sieveprofiles:ro
```

## User Experience Flow

### First Run

```
1. Application starts
2. Detects no backend preference saved
3. Shows backend selection dialog
4. User selects KeePassXC
5. Factory creates KeePassXCMasterKeyProvider
6. Provider checks if KeePassXC is running
   → Running: Proceeds to step 7
   → Not running: Shows "Please start KeePassXC" dialog with [Retry] [Use Fallback]
7. Provider checks if database is unlocked
   → Unlocked: Proceeds to step 8
   → Locked: [BUG - See Current Bugs section]
8. Provider calls associate()
   → Success: KeePassXC shows "Allow SieveEditor?" dialog
   → User clicks "Allow"
9. Provider generates random 256-bit master key
10. Stores key in KeePassXC entry
11. Saves preference: "KeePassXC"
12. Application continues with master key
```

### Subsequent Runs

```
1. Application starts
2. Loads saved preference: "KeePassXC"
3. Creates KeePassXCMasterKeyProvider
4. Provider connects and associates
5. Retrieves master key from KeePassXC entry
6. Application continues
```

### Fallback Chain

```
KeePassXC unavailable
  ↓
User clicks "Use Fallback"
  ↓
Shows selection dialog again
  ↓
Offers: OS Keychain, Manual Password Entry
```

## Security Considerations

### Threat Model

**Protected Against:**
- Unauthorized file access (600/700 permissions)
- Weak/predictable encryption keys (256-bit random)
- Hardcoded passwords (never done)

**Not Protected Against:**
- Memory dumps while application running
- Root/Administrator access
- Keyloggers (for User Prompt backend)
- Compromised OS keychain

### Password Storage Flow

```
User Password (plaintext)
    ↓
Jasypt PBE Encryption (AES-256, 10k iterations, Random IV)
    ↓
ENC(base64-encoded-ciphertext)
    ↓
Stored in <profile>.properties file
```

Example encrypted password in file:
```properties
sieve.password=ENC(sYXp9BxGqN7... base64 data ...)
```

### Master Key Protection

- **KeePassXC:** Protected by KeePassXC's database encryption (AES-256)
- **OS Keychain:** Protected by OS-specific mechanisms (varies)
- **User Prompt:** Not stored (only in memory during session)

## Current Bugs

### Issue #1: Association Fails When Database Locked

**Status:** 🔴 **CRITICAL - In Progress**

**Symptoms:**
```
de.febrildur.sieveeditor.system.credentials.CredentialException:
  Failed to associate with KeePassXC. Please allow the association
  request in KeePassXC.
```

**Root Cause:**
Code checks if database is locked AFTER calling `associate()`:

```java
// Current (broken) flow
kpa.connect();        // ✓ Works
kpa.associate();      // ✗ Fails silently if DB locked
if (kpa.isDatabaseLocked()) {  // ✗ Never reached
    // Try to unlock
}
```

**Expected Behavior:**
```java
// Correct flow
kpa.connect();
if (kpa.isDatabaseLocked()) {
    // Show user prompt to unlock
    kpa.getDatabasehash(true);  // Trigger unlock prompt
}
kpa.associate();  // Now succeeds
```

**Fix Strategy:**
1. Write failing test: `testAssociateWithLockedDatabase()`
2. Refactor `ensureConnected()` to check lock status BEFORE association
3. Show clear user message: "Please unlock your KeePassXC database"
4. Retry association after unlock
5. Verify test passes

**Files to Modify:**
- `KeePassXCMasterKeyProvider.java:125-198` (ensureConnected method)

### Issue #2: SLF4J Binding Missing

**Status:** 🟡 **MINOR - Quick Fix**

**Symptoms:**
```
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
```

**Root Cause:**
- `keepassxc-proxy-access` uses SLF4J for logging
- No SLF4J implementation provided in dependencies

**Fix:**
Add SLF4J simple binding to `pom.xml`:
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.17</version>
</dependency>
```

## Testing Strategy

### Unit Tests (Planned)

```
KeePassXCMasterKeyProviderTest:
  ✓ testIsAvailable_KeePassXCRunning()
  ✓ testIsAvailable_KeePassXCNotRunning()
  ✗ testAssociateWithLockedDatabase()  [RED - To be implemented]
  ✗ testUnlockPromptFlow()              [RED - To be implemented]
  ✓ testSetAndGetMasterKey()

OSKeychainMasterKeyProviderTest:
  ✓ testIsAvailable()
  ✓ testSetAndGetMasterKey()

UserPromptMasterKeyProviderTest:
  ✓ testIsAvailable()
  ✓ testPasswordHashing()
  ✓ testMemoryClearing()

AppDirectoryServiceTest:
  ✓ testPlatformSpecificPaths()
  ✓ testMigration()
  ✓ testSecurePermissions()
```

### Integration Tests

```
End-to-End Scenarios:
  ✓ Fresh install → Select KeePassXC → Store credentials → Restart → Retrieve
  ✓ Fresh install → Select OS Keychain → Store credentials → Restart → Retrieve
  ✓ Migration from ~/.sieveprofiles to new location
  ✗ KeePassXC locked → Unlock prompt → Association → Success [FAILING]
```

## Dependencies Added

```xml
<!-- Credential Storage -->
<dependency>
    <groupId>org.purejava</groupId>
    <artifactId>keepassxc-proxy-access</artifactId>
    <version>1.3.0</version>
</dependency>

<dependency>
    <groupId>com.github.javakeyring</groupId>
    <artifactId>java-keyring</artifactId>
    <version>1.0.4</version>
</dependency>

<dependency>
    <groupId>net.harawata</groupId>
    <artifactId>appdirs</artifactId>
    <version>1.5.0</version>
</dependency>

<!-- Logging (to be added) -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.17</version>
</dependency>
```

## Files Created

```
src/main/java/de/febrildur/sieveeditor/system/
├── AppDirectoryService.java                    [NEW]
└── credentials/
    ├── CredentialException.java                [NEW]
    ├── MasterKeyProvider.java                  [NEW]
    ├── MasterKeyProviderFactory.java           [NEW]
    ├── KeePassXCMasterKeyProvider.java         [NEW - BUGGY]
    ├── OSKeychainMasterKeyProvider.java        [NEW]
    └── UserPromptMasterKeyProvider.java        [NEW]
```

## Files Modified

```
src/main/java/de/febrildur/sieveeditor/system/
└── PropertiesSieve.java                        [MODIFIED]
    - Removed: getMachineSpecificEncryptionKey()
    - Removed: Machine-specific key derivation logic
    - Added: MasterKeyProvider integration
    - Added: migrateOldProperties() with enhanced logic
    - Changed: Directory paths to use AppDirectoryService

de.febrildur.sieveeditor.yml                    [MODIFIED]
    - Added: D-Bus Secret Service permission
    - Changed: Filesystem permissions to XDG-compliant
    - Added: Legacy migration read-only access

pom.xml                                         [MODIFIED]
    - Added: 3 new dependencies
```

## Performance Impact

**Minimal:**
- Backend selection: Once on first run
- Master key retrieval: Once on startup (~50-200ms depending on backend)
- File I/O: Negligible (small .properties files)

**Memory:**
- Master key: 44 bytes (Base64-encoded 256-bit key)
- Backend provider: ~1-5 KB depending on implementation

## Future Enhancements

1. **Backend Switching:** Allow users to change backend preference via UI
2. **Master Key Rotation:** Provide UI to re-encrypt all passwords with new master key
3. **Backup/Export:** Encrypted profile export for disaster recovery
4. **Biometric Support:** Integrate with OS biometric APIs where available
5. **Hardware Token:** Support for FIDO2/U2F keys
6. **Multi-Device Sync:** Built-in cloud sync for non-KeePassXC backends

## References

- [XDG Base Directory Specification](https://specifications.freedesktop.org/basedir-spec/latest/)
- [KeePassXC Browser Protocol](https://github.com/keepassxreboot/keepassxc-browser/blob/develop/keepassxc-protocol.md)
- [java-keyring Documentation](https://github.com/javakeyring/java-keyring)
- [AppDirs Library](https://github.com/harawata/appdirs)
- [Jasypt Documentation](http://www.jasypt.org/)

## Commit History

Will be added after commits are created with conventional commit format.

---

**Last Updated:** 2025-11-18
**Next Action:** Fix KeePassXC locked database handling
