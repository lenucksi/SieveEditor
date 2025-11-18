# Start Prompt: Fix Critical SSL/TLS Security Vulnerabilities

## Context

This is a Java 21 Swing desktop application for editing Sieve mail filter scripts. The codebase has **2 CRITICAL security vulnerabilities** related to SSL/TLS that need to be fixed.

Previous work completed:

- ✅ Java 21 LTS update
- ✅ Find/Replace functionality fixed
- ✅ Tokenizer bugs fixed
- ✅ 4K HiDPI scaling fixed
- ⚠️ Security vulnerabilities documented but NOT yet fixed

## Your Task

Fix the 2 CRITICAL SSL/TLS security vulnerabilities in the SieveEditor application:

### 1. SSL Certificate Validation Disabled (CRITICAL)

**File:** `src/main/java/de/febrildur/sieveeditor/system/ConnectAndListScripts.java`

**Problem:** Lines 97-121 contain `getInsecureSSLFactory()` that disables ALL SSL certificate validation using a TrustManager that accepts everything.

**Current vulnerable code:**

```java
public static SSLSocketFactory getInsecureSSLFactory() throws NoSuchAlgorithmException, KeyManagementException {
    TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) { }
            public void checkServerTrusted(X509Certificate[] chain, String authType) { }
            public X509Certificate[] getAcceptedIssuers() { return null; }
        }
    };
    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, trustAllCerts, new java.security.SecureRandom());
    return sc.getSocketFactory();
}
```

**What to do:**

1. Remove the `getInsecureSSLFactory()` method entirely
2. Update `connect()` method to use `SSLSocketFactory.getDefault()` instead
3. Remove the insecure SSL configuration from ManageSieveClient setup
4. Test that connection works with valid SSL certificates
5. Ensure expired/invalid certificates are properly rejected

**Security requirement:** Application MUST validate SSL certificates by default.

---

### 2. Hardcoded Encryption Key (CRITICAL)

**File:** `src/main/java/de/febrildur/sieveeditor/system/SimpleEncrypter.java`

**Problem:** Lines 12-13 contain hardcoded encryption password embedded in source code.

**Current vulnerable code:**

```java
public class SimpleEncrypter {
    private static String PASSWORD = "SomeSecurePasswordHere";
    private static String ALGORITHM = "PBEWithMD5AndDES";
    // ...
}
```

**What to do:**

1. Remove hardcoded `PASSWORD` constant
2. Generate a unique encryption key per installation (on first run)
3. Store the key securely using Java KeyStore or OS credential storage
4. For Linux: Use Secret Service API (GNOME Keyring/KWallet)
5. For macOS: Use Keychain
6. For Windows: Use Windows Credential Store
7. Fallback: Store in `~/.config/sieveeditor/keystore` with restricted permissions (0600)

**Security requirement:** Each installation must have its own unique encryption key, NOT shared across all users.

---

## Additional Context

### Project Philosophy

- **"Das ist eine Mini-App. Don't overdo patterns."** - Keep solutions simple and practical
- No need for enterprise patterns or over-engineering
- Focus on fixing the security issues correctly but simply

### Files to Review

**Security vulnerability documentation:**

- `dev-docs/analysis/modernization/01-security-vulnerabilities.md` - Complete analysis of both issues

**Implementation plan:**

- `dev-docs/analysis/modernization/04-implementation-roadmap.md` - See Phase 1 & 2 for detailed tasks

**Related files:**

- `src/main/java/de/febrildur/sieveeditor/system/ConnectAndListScripts.java` - SSL issue
- `src/main/java/de/febrildur/sieveeditor/system/SimpleEncrypter.java` - Hardcoded key issue
- `src/main/java/de/febrildur/sieveeditor/actions/ActionConnect.java` - Uses connection
- `src/main/java/de/febrildur/sieveeditor/system/CredentialsStorage.java` - Uses encryption

### Technical Requirements

**Java Version:** Java 21 LTS (already configured)

**Dependencies Available:**

```xml
<dependency>
    <groupId>com.fluffypeople</groupId>
    <artifactId>managesievej</artifactId>
    <version>0.3.1</version>
</dependency>
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
    <version>1.16.0</version>
</dependency>
<dependency>
    <groupId>org.jasypt</groupId>
    <artifactId>jasypt</artifactId>
    <version>1.9.3</version>
</dependency>
```

**You may add new dependencies if needed** for secure key storage (e.g., `java-keyring` library).

### Testing Requirements

After fixing, verify:

**SSL Fix:**

1. Connect to server with valid SSL certificate → should work
2. Connect to server with self-signed certificate → should fail with clear error
3. Connect to server with expired certificate → should fail with clear error
4. Error messages should be user-friendly and suggest solutions

**Encryption Key Fix:**

1. First run generates new key → stores securely
2. Subsequent runs use same key → passwords decrypt correctly
3. Key is NOT visible in source code or config files
4. Key storage has proper file permissions (if file-based)
5. Each installation has unique key

### Build & Run

```bash
# Build
mvn clean package

# Run
./sieveeditor.sh

# Or directly
java -jar target/SieveEditor-jar-with-dependencies.jar
```

### User Experience Goals

**For SSL errors:**

- Clear error message: "SSL certificate validation failed"
- Explain what this means (untrusted certificate)
- Suggest solution: "Add certificate to Java truststore" with command example

**For encryption:**

- Transparent to user (auto-generates key on first run)
- If key is lost, clear error: "Encryption key not found. Stored passwords cannot be recovered."
- Option to reset and re-enter passwords

### Git Commit Guidelines

Create separate commits for each fix:

```text
Fix SSL certificate validation (CRITICAL security issue)

Problem: Application disabled all SSL certificate validation, accepting
any certificate including expired, self-signed, or malicious ones.

Solution: Removed insecure TrustManager and now use Java's default
SSL certificate validation.

Security impact: Prevents man-in-the-middle attacks.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

```text
Fix hardcoded encryption key (CRITICAL security issue)

Problem: Encryption key was hardcoded in source code and shared
across all installations, making encrypted passwords easily decryptable.

Solution: Generate unique key per installation, store in OS keyring
or secure keystore with restricted permissions.

Security impact: Prevents password theft from encrypted credentials.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

## Start Here

Begin by:

1. Read `dev-docs/analysis/modernization/01-security-vulnerabilities.md` for complete analysis
2. Fix SSL certificate validation first (simpler fix)
3. Then fix hardcoded encryption key (more complex)
4. Test both fixes thoroughly
5. Update `dev-docs/FIXES-APPLIED.md` with details
6. Create proper git commits

**Remember:** Keep it simple, practical, and secure. No over-engineering needed.

---

## Success Criteria

- [ ] SSL certificate validation enabled (default Java behavior)
- [ ] Invalid certificates are rejected with clear error messages
- [ ] Hardcoded encryption key removed from source code
- [ ] Unique key generated per installation
- [ ] Key stored securely (OS keyring or restricted file)
- [ ] Existing functionality still works (connection, credential storage)
- [ ] Clear error messages for SSL and encryption issues
- [ ] Build succeeds: `mvn clean package`
- [ ] Documentation updated in FIXES-APPLIED.md
- [ ] Git commits created with proper messages

**Estimated Time:** 4-6 hours for both fixes

**Priority:** CRITICAL - These are serious security vulnerabilities that expose users to attacks
