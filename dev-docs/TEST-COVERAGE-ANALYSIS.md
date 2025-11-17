# Test Coverage Analysis for SieveEditor

**Date:** 2025-11-15
**Current Coverage:** 0% (No tests exist)
**Target Coverage:** 70%+

## Executive Summary

SieveEditor currently has **zero automated tests**. This document provides a comprehensive analysis of the codebase and recommends priority areas for test coverage improvement. The analysis identifies critical security vulnerabilities, core business logic, and UI components that require testing.

## Current State

### Test Coverage Statistics
- **Unit Tests:** 0
- **Integration Tests:** 0
- **Test Framework:** None configured
- **Coverage Tools:** None configured
- **Coverage:** 0%

### Build Configuration
- Build script explicitly skips tests: `./build.sh` runs `mvn clean package -DskipTests`
- No test dependencies in `app/pom.xml`
- No test directories exist
- No JaCoCo or other coverage tooling

## Codebase Overview

### Total Application Code: ~1,313 lines

**Package Structure:**

```
de.febrildur.sieveeditor/
├── Application.java (165 lines) - Main Swing JFrame window
├── actions/ (562 lines total) - UI action handlers
│   ├── ActionConnect.java (181 lines)
│   ├── ActionLoadScript.java (77 lines)
│   ├── ActionSaveScript.java (26 lines)
│   ├── ActionSaveScriptAs.java (29 lines)
│   ├── ActionCheckScript.java (32 lines)
│   ├── ActionReplace.java (108 lines)
│   └── ActionActivateDeactivateScript.java (109 lines)
└── system/ (586 lines total) - Core business logic
    ├── PropertiesSieve.java (184 lines)
    ├── ConnectAndListScripts.java (143 lines)
    ├── SieveTokenMaker.java (233 lines)
    ├── ToString.java (4 lines)
    └── ToStringListCellRenderer.java (22 lines)
```

### Application Functionality

**SieveEditor** is a Java Swing desktop application for editing and managing Sieve email filtering scripts on ManageSieve servers.

**Core Features:**
- Multi-account profile management
- ManageSieve protocol integration (connect, authenticate, STARTTLS)
- Script CRUD operations (upload, download, activate, deactivate, rename)
- Syntax highlighting for Sieve language
- Find/Replace with regex support
- HiDPI/4K display support

**Technology Stack:**
- Java 21 LTS
- Swing UI framework
- RSyntaxTextArea 3.5.1 (syntax highlighting)
- ManageSieveJ 0.3.2-SNAPSHOT (protocol library)
- Jasypt 1.9.3 (password encryption)
- Maven multi-module build

---

## Priority Areas for Test Improvement

### 1. CRITICAL PRIORITY: Security-Critical Code

These areas contain security vulnerabilities and MUST be tested first.

#### A. SSL/TLS Certificate Validation
**File:** `ConnectAndListScripts.java:97-121`
**Severity:** CRITICAL
**Issue:** Insecure SSL factory that trusts ALL certificates

```java
// getInsecureSSLFactory() - SECURITY VULNERABILITY
public void checkServerTrusted(X509Certificate[] certs, String authType) {
    // Empty - accepts ANY certificate (MITM attack possible!)
}
```

**Security Impact:**
- Vulnerable to Man-in-the-Middle (MITM) attacks
- Attackers can intercept credentials and scripts
- No certificate validation whatsoever

**Recommended Tests:**
```
✓ shouldRejectSelfSignedCertificates()
✓ shouldRejectExpiredCertificates()
✓ shouldRejectCertificatesWithWrongHostname()
✓ shouldUseTLS12OrHigher()
✓ shouldNotAcceptAllCertificates()
✓ shouldValidateEntireCertificateChain()
```

**Action Required:**
1. Fix the SSL validation FIRST
2. Write tests to prevent regression
3. Consider adding certificate pinning

---

#### B. Hardcoded Encryption Key
**File:** `PropertiesSieve.java:47, 65`
**Severity:** CRITICAL
**Issue:** Hardcoded encryption key in source code

```java
encryptor.setPassword("KNQ4VnqF24WLe4HZJ9fB9Sth"); // Hardcoded key!
```

**Security Impact:**
- Anyone with source code access can decrypt all stored passwords
- Key is visible in version control history
- No key rotation possible

**Recommended Tests:**
```
✓ shouldNotHaveHardcodedEncryptionKey()
✓ shouldLoadKeyFromEnvironmentVariable()
✓ shouldUseStrongEncryptionAlgorithm()
✓ shouldEncryptAndDecryptPasswordCorrectly()
✓ shouldHandleCorruptedEncryptedData()
```

**Action Required:**
1. Move key to environment variable or secure keystore
2. Use system-specific key derivation
3. Test encryption/decryption with configurable key

---

#### C. Password Field Security
**File:** `ActionConnect.java:104`
**Severity:** HIGH
**Issue:** Password displayed in plain JTextField instead of JPasswordField

```java
JTextField tfPassword = new JTextField(properties.getPassword(), 15);
// Should be: JPasswordField tfPassword = new JPasswordField(15);
```

**Security Impact:**
- Password visible on screen (shoulder surfing risk)
- Password stored as String in memory (not char[])
- Password may appear in logs/screenshots

**Recommended Tests:**
```
✓ shouldUsePasswordFieldNotTextField()
✓ shouldNotEchoPasswordCharacters()
✓ shouldClearPasswordFromMemoryAfterUse()
```

**Action Required:**
1. Replace JTextField with JPasswordField
2. Use char[] instead of String for password storage
3. Clear password arrays after use

---

### 2. HIGH PRIORITY: Core Business Logic

#### A. Profile Management (`PropertiesSieve.java`)

**Lines:** 184 | **Target Coverage:** 80%+

**Critical Functionality:**
- Multi-profile storage in `~/.sieveprofiles/`
- Profile creation, listing, switching
- Last-used profile tracking
- Encrypted credential storage
- Migration from old `~/.sieveproperties`

**Recommended Tests:**

**Basic Operations:**
```
✓ shouldSaveAndLoadProperties()
✓ shouldGetServer()
✓ shouldSetServer()
✓ shouldGetPort()
✓ shouldSetPort()
✓ shouldGetUsername()
✓ shouldSetUsername()
✓ shouldGetPassword()
✓ shouldSetPassword()
```

**Profile Management:**
```
✓ shouldHandleMultipleProfiles()
✓ shouldGetAvailableProfiles()
✓ shouldReturnDefaultProfileWhenNoneExist()
✓ shouldSaveLastUsedProfile()
✓ shouldRetrieveLastUsedProfile()
✓ shouldReturnDefaultWhenLastUsedMissing()
✓ shouldCheckIfProfileExists()
✓ shouldCreateProfilesDirectory()
```

**Encryption:**
```
✓ shouldEncryptPasswordCorrectly()
✓ shouldDecryptPasswordCorrectly()
✓ shouldHandleEmptyPassword()
✓ shouldHandleCorruptedEncryptedData()
✓ shouldThrowExceptionOnDecryptionFailure()
```

**Migration:**
```
✓ shouldMigrateOldPropertiesFile()
✓ shouldNotOverwriteExistingDefaultProfile()
✓ shouldSkipMigrationWhenOldFileNotExists()
```

**Validation:**
```
✓ shouldValidatePortRange() - Reject < 1 or > 65535
✓ shouldHandleInvalidPortString()
✓ shouldHandleNullValues()
✓ shouldHandleMissingPropertiesFile()
```

**Error Handling:**
```
✓ shouldHandleIOExceptionDuringLoad()
✓ shouldHandleIOExceptionDuringWrite()
✓ shouldCreateNewFileIfNotExists()
```

---

#### B. ManageSieve Protocol (`ConnectAndListScripts.java`)

**Lines:** 143 | **Target Coverage:** 80%+

**Critical Functionality:**
- Server connection with STARTTLS
- Authentication (username/password)
- Script upload/download
- Script validation (checkscript)
- Script activation/deactivation
- Script renaming
- Script listing

**Recommended Tests:**

**Connection:**
```
✓ shouldConnectToServer()
✓ shouldConnectWithPropertiesObject()
✓ shouldThrowExceptionOnConnectionFailure()
✓ shouldStartTLS()
✓ shouldThrowExceptionOnTLSFailure()
```

**Authentication:**
```
✓ shouldAuthenticateSuccessfully()
✓ shouldThrowExceptionOnAuthenticationFailure()
✓ shouldSetClientToNullOnAuthFailure()
```

**Script Operations:**
```
✓ shouldUploadScript()
✓ shouldActivateScriptAfterUpload()
✓ shouldThrowExceptionOnUploadFailure()
✓ shouldDownloadScript()
✓ shouldReturnScriptBody()
✓ shouldThrowExceptionOnDownloadFailure()
✓ shouldListScripts()
✓ shouldReturnEmptyListWhenNoScripts()
✓ shouldThrowExceptionOnListFailure()
```

**Script Management:**
```
✓ shouldActivateScript()
✓ shouldDeactivateScript()
✓ shouldRenameScript()
✓ shouldThrowExceptionOnActivateFailure()
✓ shouldThrowExceptionOnDeactivateFailure()
✓ shouldThrowExceptionOnRenameFailure()
```

**Validation:**
```
✓ shouldCheckScriptSyntax()
✓ shouldReturnValidationMessage()
✓ shouldValidateServerParameter()
✓ shouldValidatePortRange()
✓ shouldValidateUsernameNotNull()
✓ shouldValidatePasswordNotNull()
```

**State Management:**
```
✓ shouldReturnTrueWhenLoggedIn()
✓ shouldReturnFalseWhenNotLoggedIn()
✓ shouldLogout()
✓ shouldSetClientToNullAfterLogout()
```

---

### 3. MEDIUM PRIORITY: UI Actions

#### A. Connection Dialog (`ActionConnect.java`)

**Lines:** 181 | **Target Coverage:** 70%+

**Complex Functionality:**
- Profile selector dropdown
- New profile creation (+button)
- Profile switching with auto-save
- Connection establishment
- Form validation
- Migration trigger

**Recommended Tests:**

**Profile Management:**
```
✓ shouldLoadLastUsedProfile()
✓ shouldPopulateProfileDropdown()
✓ shouldCreateNewProfile()
✓ shouldSanitizeProfileName() - Remove invalid chars
✓ shouldPreventDuplicateProfiles()
✓ shouldShowErrorWhenProfileExists()
```

**Profile Switching:**
```
✓ shouldSaveCurrentProfileDataOnSwitch()
✓ shouldLoadNewProfileDataOnSwitch()
✓ shouldUpdateFormFieldsOnSwitch()
✓ shouldHandleEmptyProfileOnSwitch()
```

**Connection:**
```
✓ shouldConnectWithValidCredentials()
✓ shouldSaveProfileAfterSuccessfulConnection()
✓ shouldUpdateLastUsedProfile()
✓ shouldUpdateParentFrameStatus()
✓ shouldCloseDialogOnSuccess()
```

**Validation:**
```
✓ shouldHandleInvalidPort()
✓ shouldHandleEmptyServer()
✓ shouldHandleConnectionFailure()
✓ shouldShowErrorDialog()
```

**Migration:**
```
✓ shouldTriggerMigrationOnFirstOpen()
```

---

#### B. Save/Load Actions

**Files:** `ActionSaveScript.java`, `ActionSaveScriptAs.java`, `ActionLoadScript.java`
**Combined Lines:** 132 | **Target Coverage:** 70%+

**Recommended Tests:**

**Save Operations:**
```
✓ shouldSaveScript()
✓ shouldShowSuccessMessageOnSave()
✓ shouldNotShowSuccessWhenSaveFails()
✓ shouldUpdateStatusAfterSave()
✓ shouldHandleNullServer()
```

**Save As:**
```
✓ shouldPromptForScriptName()
✓ shouldHandleNullInput()
✓ shouldHandleEmptyInput()
✓ shouldSaveWithNewName()
```

**Load Operations:**
```
✓ shouldLoadScriptFromServer()
✓ shouldPopulateScriptList()
✓ shouldHandleNoScripts()
✓ shouldUpdateEditorContent()
```

---

#### C. Script Management Actions

**Files:** `ActionActivateDeactivateScript.java`, `ActionCheckScript.java`, `ActionReplace.java`
**Combined Lines:** 249 | **Target Coverage:** 70%+

**Recommended Tests:**

**Activate/Deactivate:**
```
✓ shouldActivateScript()
✓ shouldDeactivateScript()
✓ shouldRenameScript()
✓ shouldHandleNoRowSelected() - BUG REGRESSION TEST
✓ shouldShowCorrectMessageAfterRename()
✓ shouldUpdateTableAfterOperation()
```

**Check Script:**
```
✓ shouldValidateScript()
✓ shouldShowValidationResult()
✓ shouldHandleNullServer()
✓ shouldHandleInvalidSyntax()
```

**Find/Replace:**
```
✓ shouldFindText()
✓ shouldFindWithRegex()
✓ shouldFindCaseSensitive()
✓ shouldFindCaseInsensitive()
✓ shouldReplaceText()
✓ shouldReplaceAll()
✓ shouldHighlightFoundText()
✓ shouldHandleNoMatches()
```

---

### 4. MEDIUM PRIORITY: Syntax Highlighting

#### Tokenizer (`SieveTokenMaker.java`)

**Lines:** 233 | **Target Coverage:** 60%+

**Functionality:**
- Custom RSyntaxTextArea tokenizer
- Sieve language syntax support
- Keyword, string, comment, number tokenization

**Known Issues:**
- Line 174: `i--` to backtrack after number tokenization (potential bug)

**Recommended Tests:**

**Basic Tokenization:**
```
✓ shouldTokenizeKeywords() - "if", "require", etc.
✓ shouldTokenizeStrings() - Double-quoted strings
✓ shouldTokenizeComments() - # to end of line
✓ shouldTokenizeNumbers() - Integer literals
✓ shouldTokenizeWhitespace() - Spaces and tabs
✓ shouldTokenizeIdentifiers() - Variable names
```

**Edge Cases:**
```
✓ shouldHandleNumbersFollowedByLetters() - "100K"
✓ shouldHandleUnclosedStrings() - Multiline strings
✓ shouldHandleEmptyInput()
✓ shouldHandleMixedTokens()
✓ shouldCorrectlyBacktrackAfterNumber() - BUG REGRESSION
```

**Sieve-Specific:**
```
✓ shouldHighlightSieveKeywords()
✓ shouldRecognizeCommandNames()
✓ shouldRecognizeTestNames()
✓ shouldRecognizeActionNames()
```

---

### 5. LOW PRIORITY: UI Integration

#### Main Application Window (`Application.java`)

**Lines:** 165 | **Target Coverage:** 40%+

**Recommended Tests:**

**Initialization:**
```
✓ shouldInitializeUI()
✓ shouldSetupMenus()
✓ shouldSetupKeyboardShortcuts()
✓ shouldLoadInitialProperties()
```

**Error Handling:**
```
✓ shouldHandleNullServer()
✓ shouldHandleNullScript()
✓ shouldShowErrorMessages()
```

**Status Updates:**
```
✓ shouldUpdateStatusBar()
✓ shouldShowConnectionStatus()
```

---

## Recommended Testing Infrastructure

### Step 1: Add Test Dependencies

Add to `app/pom.xml`:

```xml
<dependencies>
    <!-- Existing dependencies... -->

    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.1</version>
        <scope>test</scope>
    </dependency>

    <!-- Mockito for mocking -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.8.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>5.8.0</version>
        <scope>test</scope>
    </dependency>

    <!-- AssertJ for fluent assertions -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.24.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Existing plugins... -->

        <!-- Surefire for test execution -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.3</version>
        </plugin>

        <!-- JaCoCo for coverage reports -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Step 2: Create Test Directory Structure

```
app/src/test/java/de/febrildur/sieveeditor/
├── system/
│   ├── PropertiesSieveTest.java
│   ├── ConnectAndListScriptsTest.java
│   └── SieveTokenMakerTest.java
├── actions/
│   ├── ActionConnectTest.java
│   ├── ActionSaveScriptTest.java
│   ├── ActionLoadScriptTest.java
│   ├── ActionSaveScriptAsTest.java
│   ├── ActionReplaceTest.java
│   ├── ActionCheckScriptTest.java
│   └── ActionActivateDeactivateScriptTest.java
├── security/
│   ├── SSLValidationTest.java
│   ├── EncryptionTest.java
│   └── PasswordFieldTest.java
└── testutil/
    └── TestConfiguration.java
```

### Step 3: Update Build Configuration

Modify `build.sh`:
```bash
#!/bin/bash
# Remove -DskipTests to run tests
mvn clean test package
```

Or keep separate scripts:
```bash
# build.sh - quick build without tests
mvn clean package -DskipTests

# test.sh - run tests with coverage
mvn clean test jacoco:report
```

---

## Phased Implementation Plan

### Phase 1: Test Infrastructure Setup (Week 1)
**Goal:** Enable testing capability

- [ ] Add test dependencies to `app/pom.xml`
- [ ] Add JaCoCo plugin for coverage
- [ ] Create test directory structure
- [ ] Create test utility classes
- [ ] Update build scripts
- [ ] Verify test execution with simple smoke test

**Deliverable:** Working test infrastructure with first passing test

---

### Phase 2: Security Tests (Week 2)
**Goal:** Cover critical security vulnerabilities - TARGET: 40% coverage

Before writing tests, fix security issues:
- [ ] Fix SSL validation (remove insecure trust manager)
- [ ] Remove hardcoded encryption key (use environment variable)
- [ ] Replace JTextField with JPasswordField for passwords

Then write tests:
- [ ] `SSLValidationTest.java` - Certificate validation tests
- [ ] `EncryptionTest.java` - Encryption/decryption tests
- [ ] `PasswordFieldTest.java` - UI security tests

**Deliverable:** All security vulnerabilities have regression tests

---

### Phase 3: Core Business Logic - Part 1 (Week 3)
**Goal:** Test profile management - TARGET: 50% coverage

- [ ] `PropertiesSieveTest.java` - Complete test suite
  - Basic get/set operations
  - Profile management (create, list, switch)
  - Encryption/decryption
  - Migration logic
  - Error handling

**Deliverable:** PropertiesSieve.java at 80%+ coverage

---

### Phase 4: Core Business Logic - Part 2 (Week 4)
**Goal:** Test ManageSieve protocol - TARGET: 60% coverage

- [ ] `ConnectAndListScriptsTest.java` - Complete test suite
  - Connection and authentication
  - Script CRUD operations
  - Script management (activate, deactivate, rename)
  - Validation and error handling

**Deliverable:** ConnectAndListScripts.java at 80%+ coverage

---

### Phase 5: UI Actions (Week 5)
**Goal:** Test action handlers - TARGET: 65% coverage

- [ ] `ActionConnectTest.java` - Profile dialog tests
- [ ] `ActionSaveScriptTest.java` - Save operation tests
- [ ] `ActionLoadScriptTest.java` - Load operation tests
- [ ] `ActionActivateDeactivateScriptTest.java` - Script management tests
- [ ] `ActionReplaceTest.java` - Find/replace tests
- [ ] `ActionCheckScriptTest.java` - Validation tests

**Deliverable:** All action classes at 70%+ coverage

---

### Phase 6: Tokenizer & Polish (Week 6)
**Goal:** Complete coverage - TARGET: 70%+ coverage

- [ ] `SieveTokenMakerTest.java` - Syntax highlighting tests
- [ ] Bug regression tests
- [ ] Edge case tests
- [ ] Documentation updates

**Deliverable:** Overall 70%+ coverage achieved

---

## Expected Coverage Outcomes

| Component | Lines | Priority | Target | Phase |
|-----------|-------|----------|--------|-------|
| **ConnectAndListScripts.java** | 143 | CRITICAL | 80% | 4 |
| **PropertiesSieve.java** | 184 | CRITICAL | 80% | 3 |
| **ActionConnect.java** | 181 | HIGH | 70% | 5 |
| **Other actions/*** | 381 | MEDIUM | 70% | 5 |
| **SieveTokenMaker.java** | 233 | MEDIUM | 60% | 6 |
| **Application.java** | 165 | LOW | 40% | 6 |
| **OVERALL** | ~1,313 | - | **70%+** | 6 |

---

## Testing Best Practices

### 1. Test Naming Convention
Use descriptive names that explain intent:
```java
// Good
@Test
void shouldThrowExceptionWhenServerIsNull() { }

// Bad
@Test
void test1() { }
```

### 2. AAA Pattern (Arrange-Act-Assert)
```java
@Test
void shouldEncryptPassword() {
    // Arrange (Given)
    PropertiesSieve config = new PropertiesSieve();

    // Act (When)
    config.setPassword("secret");

    // Assert (Then)
    assertThat(config.getPassword()).isEqualTo("secret");
}
```

### 3. Test Independence
Each test must be independent:
```java
// Good - each test creates its own instance
@Test
void test1() {
    PropertiesSieve config = new PropertiesSieve();
    // ...
}

// Bad - shared state between tests
static PropertiesSieve config;
```

### 4. One Concept Per Test
Focus on single behavior:
```java
// Good - separate tests
@Test
void shouldSetServer() { /* ... */ }

@Test
void shouldSetPort() { /* ... */ }

// Bad - testing multiple things
@Test
void shouldSetServerAndPort() { /* ... */ }
```

---

## Success Metrics

### Quantitative Goals
- ✓ 70%+ overall code coverage
- ✓ 80%+ coverage for critical components
- ✓ All security vulnerabilities have tests
- ✓ All high-severity bugs have regression tests
- ✓ 0 failing tests in CI/CD
- ✓ All tests run in < 2 minutes

### Qualitative Goals
- ✓ Tests are maintainable and readable
- ✓ Tests document expected behavior
- ✓ Tests catch regressions early
- ✓ Developers can run tests locally easily
- ✓ New features include tests (TDD)

---

## Appendix: Known Bugs to Test

These bugs should have regression tests:

| ID | File | Line | Issue | Test |
|----|------|------|-------|------|
| BUG-001 | ActionActivateDeactivateScript.java | 57 | ArrayIndexOutOfBoundsException when no row selected | shouldHandleNoRowSelected() |
| BUG-002 | ActionSaveScript.java | 21-23 | Shows success message even when save fails | shouldNotShowSuccessWhenSaveFails() |
| BUG-003 | ActionReplace.java | 48-49 | Find next button not wired correctly | shouldFindNextWhenButtonClicked() |
| BUG-004 | SieveTokenMaker.java | 174 | Backtrack logic after number tokenization | shouldCorrectlyTokenizeNumbers() |

---

## Maintenance Strategy

### For New Features
1. Write test first (TDD)
2. Implement feature
3. Ensure test passes
4. Check coverage increased

### For Bug Fixes
1. Write failing test that reproduces bug
2. Fix the bug
3. Ensure test passes
4. Add to regression test suite

### For Refactoring
1. Ensure all tests pass before refactoring
2. Refactor code
3. Ensure all tests still pass
4. Update tests only if behavior changed

---

## Conclusion

This analysis identifies SieveEditor's current 0% test coverage and provides a comprehensive roadmap to achieve 70%+ coverage in 6 weeks. The prioritized approach focuses on:

1. **Security-critical code first** (SSL, encryption, passwords)
2. **Core business logic second** (profiles, ManageSieve protocol)
3. **UI and polish third** (actions, tokenizer)

By following this plan, the project will gain:
- Protection against security vulnerabilities
- Prevention of regression bugs
- Confidence in refactoring
- Documentation of expected behavior
- Foundation for future development

**Immediate next step:** Set up test infrastructure (Phase 1)
