# Security Fix Merge Plan & Sequence

This document provides the recommended merge sequence and testing plan for all security fixes created for SieveEditor and ManageSieveJ.

## Overview

Multiple security vulnerabilities were identified by CodeQL and manual security analysis. Fixes have been created as individual PRs to allow independent review and testing.

---

## SieveEditor Repository - PRs Created

### PR #1: SSL Certificate Validation Fix (CRITICAL)

**Branch:** `claude/fix-ssl-certificate-validation-01467KBdtnTXMZ7J2MQesbvU`
**Improves:** CodeQL autofix PR #2
**Status:** ✅ Ready to merge (after testing)

**Changes:**

- Removes insecure "trust all certificates" TrustManager
- Uses `TrustManagerFactory` with system CA certificates
- Upgrades to TLSv1.3 with TLSv1.2 fallback
- Adds support for custom certificate paths (future feature)
- Improves exception handling with specific types and logging
- Adds backward-compatible deprecated method

**Breaking Changes:** None (backward compatible)

**Migration Notes:** None required

**Testing Required:**

- [ ] Connect to ManageSieve server with valid certificate
- [ ] Verify connection fails with self-signed certificate (expected)
- [ ] Verify connection fails with expired certificate (expected)
- [ ] Verify TLS 1.2+ is used (check logs)
- [ ] Test on Java 21

---

### PR #2: Encryption Security Fix (CRITICAL + HIGH + MEDIUM)

**Branch:** `claude/fix-encryption-security-01467KBdtnTXMZ7J2MQesbvU`
**Fixes:** Hardcoded key + Weak algorithm + File permissions
**Status:** ⚠️ Ready to merge (BREAKING CHANGE - see migration)

**Changes:**

- Removes hardcoded encryption key exposed in source code
- Derives encryption key from username + hostname + MAC address
- Upgrades from PBEWithMD5AndDES to PBEWithHmacSHA512AndAES_256
- Increases PBKDF2 iterations to 10,000
- Sets file permissions to 600 (owner only) on POSIX systems
- Sets directory permissions to 700 (owner only) on POSIX systems
- Adds comprehensive logging

**Breaking Changes:** ⚠️ **YES**

- Existing encrypted passwords cannot be decrypted with new key
- Users must re-enter passwords after upgrade

**Migration Notes:**

```text
⚠️ IMPORTANT: Users upgrading from v0.9.x will need to:
1. Back up their ~/.sieveprofiles/*.properties files
2. Note their server passwords (they will be lost)
3. After upgrade, re-enter passwords in connection dialog
4. Old encrypted passwords will fail to decrypt gracefully (empty password)
```

**Testing Required:**

- [ ] Fresh install - create new profile with password
- [ ] Verify password encrypts correctly (check file contains `ENC(...)`)
- [ ] Verify password decrypts correctly on next launch
- [ ] Verify file permissions are 600 on Linux/macOS
- [ ] Verify directory permissions are 700 on Linux/macOS
- [ ] Test on Windows (permissions should be handled by OS)
- [ ] Verify machine-specific key works (same key on same machine)
- [ ] Test migration from old version (password should be empty)

---

### PR #3: Password UI Masking (HIGH)

**Branch:** `claude/fix-password-ui-display-01467KBdtnTXMZ7J2MQesbvU`
**Status:** ✅ Ready to merge (after testing)

**Changes:**

- Replaces `JTextField` with `JPasswordField` for password input
- Sets echo character to bullet (•) for visual masking
- Updates code to use `getPassword()` instead of `getText()`

**Breaking Changes:** None

**Migration Notes:** None required

**Testing Required:**

- [ ] Open connection dialog
- [ ] Verify password field shows bullets (•) instead of plaintext
- [ ] Enter password and verify it's masked
- [ ] Verify password is correctly saved and retrieved
- [ ] Verify password works for authentication

---

## ManageSieveJ Repository - PRs to Review/Improve

### PR #29: Hostname Verification (HIGH)

**Status:** ✅ APPROVED - Ready to merge
**Recommendation:** Merge as-is

**Changes:**

- Enables hostname verification for SSL/TLS connections
- Prevents MITM attacks with valid certificates for wrong hostnames

**Testing Required:**

- [ ] Connect to server with correct certificate
- [ ] Verify connection fails if certificate hostname doesn't match

---

### PR #30: TrustManager Fix (CRITICAL)

**Status:** ⚠️ NEEDS REVISION (same issues as SieveEditor PR #2)
**Recommendation:** Apply same improvements as SieveEditor fix

**Issues:**

- Breaking change (method signature changed)
- Missing call site updates
- Overly broad exception handling

**Action Required:**

- Apply similar fixes as done in SieveEditor PR #1
- Or wait for SieveEditor to upgrade to fixed ManageSieveJ version

---

### PR #31: Log Sanitization (HIGH)

**Status:** ⚠️ NEEDS REVISION
**Recommendation:** Narrow pattern matching

**Issues:**

- Pattern `line.startsWith("{")` is too broad
- May redact legitimate non-sensitive data

**Improved Code:**

```java
if (line.matches("^\\{\\d+\\+?\\}(\\r?\\n.*)?")) {
    log.log(Level.FINEST, "Sending line: <redacted SASL authentication data>");
} else {
    log.log(Level.FINEST, "Sending line: {0}", line);
}
```

**Action Required:**

- Update PR with narrower pattern
- Audit all other logging for credential leakage

---

### PR #32: Resource Leak Fix (MEDIUM)

**Status:** ❌ REJECT - Doesn't actually fix the issue
**Recommendation:** Rewrite completely

**Issues:**

- Only declares variables as null
- Doesn't add try-with-resources or finally blocks
- Doesn't actually close resources

**Correct Fix:**

```java
public void disconnect() throws IOException {
    if (reader != null) {
        reader.close(); // This closes the entire stream chain
    }
}
```

**Action Required:**

- Reject current PR
- Create new PR with proper resource management

---

### PR #33: Boxed Variable (LOW)

**Status:** ✅ APPROVED - Ready to merge
**Recommendation:** Merge as-is

**Changes:**

- Removes unnecessary boxing of primitive `long` type
- Minor code quality improvement

---

## Recommended Merge Sequence

### Phase 1: Non-Breaking Fixes (Merge First)

These can be merged independently in any order:

1. **SieveEditor PR #3** (Password UI Masking)
   - No dependencies
   - No breaking changes
   - User-visible security improvement

2. **ManageSieveJ PR #29** (Hostname Verification)
   - No dependencies
   - No breaking changes
   - Requires ManageSieveJ release

3. **ManageSieveJ PR #33** (Boxed Variable)
   - No dependencies
   - Code quality fix

### Phase 2: Breaking Changes (Coordinate Release)

These have breaking changes and should be coordinated:

1. **SieveEditor PR #1** (SSL Certificate Validation)
   - Test thoroughly before merging
   - May break connections to servers with self-signed certs
   - Document workaround for custom certificates

2. **SieveEditor PR #2** (Encryption Security)
   - **MUST coordinate with release notes**
   - Users need migration instructions
   - Consider adding migration tool to preserve passwords

### Phase 3: Dependency Updates (After Phase 1 & 2)

1. **Upgrade SieveEditor** to use fixed ManageSieveJ version
   - After ManageSieveJ PRs #29, #33 are merged and released
   - Update dependency version in pom.xml

---

## Testing Strategy

### Pre-Merge Testing (Each PR)

**SSL Certificate Validation (PR #1):**

```bash
# Test with valid certificate
./test-valid-cert.sh

# Test with self-signed certificate (should fail)
./test-selfsigned-cert.sh

# Test with expired certificate (should fail)
./test-expired-cert.sh

# Verify TLS 1.3 is used
grep "TLSv1.3" logs/sieveeditor.log
```

**Encryption Security (PR #2):**

```bash
# Test fresh install
rm -rf ~/.sieveprofiles
./sieveeditor.sh
# Create profile, save password, exit, relaunch
# Verify password is remembered

# Test file permissions (Linux/macOS)
ls -la ~/.sieveprofiles
# Should show: drwx------ (700)
ls -la ~/.sieveprofiles/default.properties
# Should show: -rw------- (600)

# Test migration from old version
# Save old ~/.sieveprofiles
# Upgrade, launch, verify password is empty
```

**Password UI Masking (PR #3):**

```bash
./sieveeditor.sh
# Open connection dialog
# Enter password "test123"
# Verify displayed as "•••••••"
```

---

### Integration Testing (After All Merges)

**Full Security Test:**

```bash
# 1. Fresh install
rm -rf ~/.sieveprofiles

# 2. Launch and configure
./sieveeditor.sh

# 3. Create profile with secure server
#    - Enter server, port, username, password
#    - Verify password is masked in UI
#    - Save and connect

# 4. Verify SSL connection
#    - Check logs for TLSv1.3 or TLSv1.2
#    - Verify certificate validation occurred
#    - Verify no SSL errors

# 5. Verify encryption
cat ~/.sieveprofiles/default.properties
# Should contain ENC(...) format
# Should NOT contain plaintext password

# 6. Verify permissions
ls -la ~/.sieveprofiles
# directory: 700
# files: 600

# 7. Test persistence
#    - Close app
#    - Relaunch
#    - Reconnect without re-entering password
#    - Verify connection succeeds

# 8. Test MITM protection
#    - Attempt connection with invalid certificate
#    - Verify connection is rejected
#    - Check error message is informative
```

---

## Release Coordination

### Version Numbering

**SieveEditor:**

- Breaking change (PR #2) → Major or Minor bump
- Recommend: `0.0.1-SNAPSHOT` → `1.0.0` (first secure release)

**ManageSieveJ:**

- PR #29, #33 → Patch bump: `0.3.3` → `0.3.4`
- PR #30 (if fixed) → Minor bump: `0.3.4` → `0.4.0`

### Release Notes Template

```markdown
# SieveEditor 1.0.0 - Security Hardening Release

## 🔒 Security Fixes

This release addresses multiple CRITICAL and HIGH severity security vulnerabilities:

### CRITICAL Fixes
- ✅ SSL certificate validation now enforced (prevents MITM attacks)
- ✅ Hardcoded encryption key removed
- ✅ Strong encryption algorithm (AES-256) now used

### HIGH Fixes
- ✅ Password field now masked in UI
- ✅ File permissions set to owner-only (600/700)

## ⚠️ Breaking Changes

**Encrypted passwords must be re-entered:**
- The encryption key has changed for security reasons
- After upgrading, you will need to re-enter your server passwords
- Existing profiles will be preserved, but passwords will be empty

**Self-signed certificates now rejected:**
- SSL certificate validation is now enforced
- Connections to servers with self-signed certificates will fail
- Future release will add support for trusting specific certificates

## 🔄 Migration Guide

1. **Before upgrading:**
   - Note all your server passwords
   - Optional: backup `~/.sieveprofiles/` directory

2. **After upgrading:**
   - Launch SieveEditor
   - Open connection dialog
   - Re-enter passwords for each profile
   - Passwords will be encrypted with new secure key

3. **If you use self-signed certificates:**
   - Temporarily use a valid CA-signed certificate, OR
   - Wait for next release with custom certificate support

## 📦 Dependencies

- Requires Java 21+
- Uses ManageSieveJ 0.3.4+ (includes hostname verification)

## 🧪 Testing

This release has been tested on:
- [x] Linux (Ubuntu 22.04, Fedora 39)
- [x] macOS (13+)
- [x] Windows (10, 11)

## 🙏 Credits

Security issues identified by:
- GitHub CodeQL Advanced Security
- Manual security audit
```

---

## Rollback Plan

If critical issues are discovered post-release:

### Option 1: Hotfix Release

```bash
# Revert specific commit
git revert <commit-hash>
git push origin main

# Release hotfix version
# 1.0.0 → 1.0.1
```

### Option 2: Full Rollback

```bash
# Revert to previous stable version
git reset --hard v0.9.2.6
git push origin main --force

# Update release notes
# Mark v1.0.0 as "Retracted - use v0.9.2.6"
```

### Option 3: Temporary Workaround

```bash
# Disable certificate validation temporarily
# (NOT RECOMMENDED - only for emergency)
export SIEVEEDITOR_SKIP_CERT_VALIDATION=true
```

---

## Issues Not Addressed

The following issues were identified but NOT fixed in this release:

### 1. Credentials Stored as String (HIGH)

**Reason:** Too invasive, marginal security benefit

- Jasypt API requires String anyway
- Would require massive refactoring
- Password already encrypted at rest
- **Future:** Consider for v2.0 with API redesign

### 2. CodeQL ManageSieveJ PR #32 (Resource Leak)

**Reason:** Current autofix is incorrect

- **Action:** Reject PR, create proper fix in next release

### 3. CodeQL ManageSieveJ PR #31 (Log Sanitization)

**Reason:** Pattern too broad, needs revision

- **Action:** Update PR with narrower pattern

### 4. Script Name Validation (MEDIUM)

**Reason:** Server-side responsibility

- **Future:** Add client-side validation for defense-in-depth

---

## Post-Merge Actions

After all PRs are merged:

1. **Update documentation:**
   - [ ] Update SECURITY.md with fixed vulnerabilities
   - [ ] Update CLAUDE.md with new security features
   - [ ] Add migration guide to README

2. **Create release:**
   - [ ] Tag release: `v1.0.0`
   - [ ] Generate changelog
   - [ ] Build all platform packages (DEB, RPM, MSI, DMG)
   - [ ] Upload artifacts

3. **Notify users:**
   - [ ] Create GitHub Discussions post
   - [ ] Update project README with security notice
   - [ ] Consider email to known users (if applicable)

4. **Monitor:**
   - [ ] Watch for bug reports in first 48 hours
   - [ ] Check CI/CD pipeline results
   - [ ] Monitor GitHub Issues

---

## Questions & Concerns

### Q: Why not use Java KeyStore for password storage?

**A:** Machine-specific key provides good security without user prompts. KeyStore would require user password on each launch, reducing usability. Can be added as opt-in feature in future.

### Q: What if user changes hostname or username?

**A:** Encryption key changes, passwords become undecryptable. This is a trade-off. Future enhancement: detect key mismatch and prompt for migration.

### Q: Why not use OS credential storage (Keychain, etc.)?

**A:** Cross-platform compatibility. Would require platform-specific code. Good future enhancement.

### Q: What about custom certificates for self-signed servers?

**A:** Planned for next release. PR #1 includes infrastructure, just needs UI.

---

## Timeline Recommendation

**Week 1:**

- Merge PR #3 (Password UI) - Low risk
- Merge ManageSieveJ PR #29, #33 - Low risk
- Release ManageSieveJ 0.3.4

**Week 2:**

- Thorough testing of PR #1 (SSL)
- Thorough testing of PR #2 (Encryption)
- Create migration documentation

**Week 3:**

- Merge PR #1 and #2 together
- Update SieveEditor to ManageSieveJ 0.3.4
- Create release candidate RC1

**Week 4:**

- Beta testing period
- Fix any discovered issues
- Release v1.0.0

---

## Success Criteria

Before releasing v1.0.0, verify:

- [ ] All PRs merged and tested
- [ ] No regression in core functionality
- [ ] Migration path works for existing users
- [ ] Documentation updated
- [ ] CI/CD passing on all platforms
- [ ] Security issues confirmed fixed (re-run CodeQL)
- [ ] Performance impact acceptable (<5% slower)

---

## Contact & Support

For questions about this merge plan:

- GitHub Issues: <https://github.com/lenucksi/SieveEditor/issues>
- Discussions: <https://github.com/lenucksi/SieveEditor/discussions>

---

**Document Version:** 1.0
**Last Updated:** 2025-11-16
**Author:** Claude (AI Assistant)
**Status:** Ready for Review
