# Security Hardening Release with Interactive Certificate Trust

## 🔒 Overview

This PR implements comprehensive security fixes addressing **5 vulnerabilities** (2 CRITICAL, 2 HIGH, 1 MEDIUM) and adds a user-friendly certificate trust dialog for self-signed certificates.

## ✨ What's New

### 1. Interactive Certificate Trust Dialog

**New in this PR:** Users can now accept self-signed certificates through an intuitive dialog, similar to web browsers.

- 🖼️ Shows complete certificate details (subject, issuer, validity)
- 🔍 Displays SHA-256 fingerprint for manual verification
- 💾 Stores trust decisions persistently
- ⚠️ Clear warnings about potential MITM attacks
- 🎯 Three options: Trust & Connect, Reject, Cancel

**Storage:** `~/.sieveprofiles/certificates.properties` (600 permissions)

## 🛡️ Security Vulnerabilities Fixed

### CRITICAL Severity

#### 1. Improper Certificate Validation (CWE-295)

**Before:** Accepted ALL certificates (MITM vulnerable)

```java
// Old: Trust everything - INSECURE!
TrustManager[] trustAllCerts = new TrustManager[] {
    new X509TrustManager() {
        public void checkServerTrusted(...) { } // No validation!
    }
};
```

**After:** Proper validation with interactive user approval

- ✅ System CA validation first
- ✅ User trust store second
- ✅ Interactive dialog for unknown certificates
- ✅ TLS 1.3 with TLS 1.2 fallback

**Impact:** Prevents man-in-the-middle attacks on ManageSieve connections.

---

#### 2. Hardcoded Encryption Key (CWE-798)

**Before:** Key exposed in source code

```java
// Old: Hardcoded key visible to anyone
encryptor.setPassword("KNQ4VnqF24WLe4HZJ9fB9Sth");
```

**After:** Machine-specific key derivation

```java
// New: Derived from username + hostname + MAC address
String key = SHA256(username + hostname + macAddress)
```

**Impact:** Passwords no longer decryptable by reading source code.

---

### HIGH Severity

#### 3. Weak Encryption Algorithm (CWE-327)

**Before:** `PBEWithMD5AndDES` (broken, deprecated)

**After:** `PBEWithHmacSHA512AndAES_256`

- ✅ AES-256 encryption
- ✅ HMAC-SHA512 authentication
- ✅ 10,000 PBKDF2 iterations
- ✅ Industry-standard security

---

#### 4. Password Displayed in Plain Text (CWE-522)

**Before:** `JTextField` showed typed characters

**After:** `JPasswordField` with bullet masking

- ✅ Passwords shown as "••••••••"
- ✅ Protected from shoulder surfing
- ✅ Safe in screenshots/screen shares

---

### MEDIUM Severity

#### 5. Insecure File Permissions (CWE-732)

**Before:** Default permissions (readable by all users)

**After:** Restrictive permissions

- ✅ Files: 600 (owner read/write only)
- ✅ Directories: 700 (owner access only)
- ✅ Works on POSIX systems (Linux, macOS)
- ✅ Windows: OS-level protection

---

## ⚠️ Breaking Changes

### 1. Passwords Must Be Re-entered

**Reason:** Encryption key changed from hardcoded to machine-specific.

**What Users Need to Do:**

1. Note all server passwords before upgrading
2. After upgrade, re-enter passwords in connection dialog
3. Passwords will be re-encrypted with secure key

**What Happens:** Old passwords fail to decrypt gracefully (become empty). No error messages.

---

### 2. Self-Signed Certificates Prompt for Approval

**Reason:** Certificate validation now properly enforced.

**What Users Will See:**

- Dialog showing certificate details and fingerprint
- Three choices: Trust & Connect, Reject, or Cancel
- Decision stored for future connections

**Verifying Certificates:**

```bash
# Get server's certificate fingerprint
openssl s_client -connect your-server:4190 -starttls imap < /dev/null 2>/dev/null | \
  openssl x509 -fingerprint -sha256 -noout

# Compare with fingerprint shown in SieveEditor dialog
```

---

## 📊 Changes Summary

### Code Changes

- **Files Created:** 5 new files (971 lines)
  - `CertificateStore.java` - Trust decision storage
  - `CertificateDialog.java` - Certificate approval UI
  - `InteractiveTrustManager.java` - Custom X509TrustManager
  - `SECURITY.md` - Security documentation
  - `MIGRATION-GUIDE-v1.0.md` - Upgrade instructions

- **Files Modified:** 4 files
  - `ConnectAndListScripts.java` - SSL implementation
  - `PropertiesSieve.java` - Encryption overhaul
  - `ActionConnect.java` - Password field + cert dialog integration
  - `README.md` - Security notice

### Commits

```text
9298336 docs: update security docs with certificate trust dialog feature
850e257 feat(security): add interactive certificate trust dialog
49b361f docs: add security documentation and migration guide
ba3d15e security!: replace hardcoded encryption key (BREAKING)
1ede4f5 fix(security): implement proper SSL certificate validation
43ed2b5 security: merge PR #3 - password UI masking
```

---

## 🧪 Testing

### Manual Testing Required

**Test 1: Password Encryption**

```bash
# Fresh install
rm -rf ~/.sieveprofiles
./sieveeditor.sh

# Create profile, save password, exit
# Relaunch - password should be remembered

# Verify encryption
cat ~/.sieveprofiles/default.properties
# Should contain ENC(...), NOT plaintext
```

**Test 2: File Permissions (Linux/macOS)**

```bash
ls -la ~/.sieveprofiles
# Directory: drwx------  (700)

ls -la ~/.sieveprofiles/*.properties
# Files: -rw-------  (600)
```

**Test 3: Certificate Trust Dialog**

```bash
# Connect to server with self-signed cert
# Dialog should appear with certificate details
# Click "Trust & Connect"
# Reconnect - should not prompt again

# Verify stored decision
cat ~/.sieveprofiles/certificates.properties
```

**Test 4: CA-Signed Certificates**

```bash
# Connect to server with valid CA cert (e.g., Let's Encrypt)
# Should connect without dialog
# Should use TLS 1.3 or TLS 1.2
```

---

## 📚 Documentation

### New Documentation

- ✅ **SECURITY.md** - Vulnerability details, reporting process, best practices
- ✅ **MIGRATION-GUIDE-v1.0.md** - Step-by-step upgrade instructions
- ✅ **README.md** - Security notice for upgrading users

### Documentation Includes

- Certificate trust dialog workflow
- Fingerprint verification instructions
- Migration scenarios and timelines
- Troubleshooting common issues
- FAQ for user concerns

---

## 🎯 Recommended Version

**Suggested Version:** `1.0.0` (First secure release)

**Reasoning:**

- Major breaking change (encryption key)
- Complete security overhaul
- New major feature (certificate trust)
- Suitable for production use

---

## 🔄 Rollback Plan

If critical issues discovered:

**Option 1: Hotfix**

```bash
git revert <commit-hash>
# Release 1.0.1
```

**Option 2: Full Rollback**

```bash
git reset --hard v0.9.2.6
# Mark v1.0.0 as retracted
```

**Not Recommended:** Rollback leaves known vulnerabilities.

---

## 📋 Checklist

- [x] All security vulnerabilities addressed
- [x] Certificate trust dialog implemented
- [x] Unit tests pass (where applicable)
- [x] Documentation complete
- [x] Migration guide created
- [x] Breaking changes documented
- [x] Conventional commits used
- [ ] Manual testing performed
- [ ] Beta testing period (recommended)

---

## 🙏 Credits

Security vulnerabilities identified by:

- GitHub CodeQL Advanced Security
- Manual security code review

Feature implementation:

- Claude (AI Assistant)

---

## 📝 Release Notes Preview

```markdown
# SieveEditor 1.0.0 - Security Hardening Release

## 🔒 Security Fixes

### CRITICAL
- ✅ SSL certificate validation now enforced (prevents MITM)
- ✅ Hardcoded encryption key removed
- ✅ Strong encryption (AES-256) implemented

### HIGH
- ✅ Password field now masked in UI
- ✅ File permissions set to owner-only (600/700)

## ✨ New Features

- 🆕 Interactive certificate trust dialog
- 🔍 SHA-256 fingerprint verification
- 💾 Persistent trust decisions

## ⚠️ Breaking Changes

**Action Required:**
- Re-enter passwords after upgrade
- Verify self-signed certificate fingerprints

See MIGRATION-GUIDE-v1.0.md for details.
```

---

## 🔗 Related Issues

This PR addresses security vulnerabilities discovered through automated scanning and manual review. No public issue numbers as these were security-sensitive.

---

## 💬 Questions?

For questions about:

- **Migration:** See `MIGRATION-GUIDE-v1.0.md`
- **Security:** See `SECURITY.md`
- **Usage:** See certificate trust workflow in dialog

---

**Ready to Merge:** ✅ (pending manual testing and review)
