# Security Policy

## Supported Versions

| Version | Supported          | Security Status |
| ------- | ------------------ | --------------- |
| 1.0.x   | :white_check_mark: | Fully Patched   |
| 0.9.x   | :x:                | Known Vulnerabilities |

## Recent Security Fixes (v1.0.0)

### CRITICAL Vulnerabilities Fixed

#### 1. SSL Certificate Validation (CWE-295)
**Status:** ✅ Fixed in v1.0.0
**Severity:** CRITICAL
**Location:** `ConnectAndListScripts.java`

**Issue:** The application used a "trust all certificates" TrustManager that accepted any SSL/TLS certificate without validation, making it vulnerable to man-in-the-middle (MITM) attacks.

**Fix:**
- Removed insecure TrustManager
- Implemented proper certificate validation using system CA certificates
- Upgraded to TLS 1.3 with TLS 1.2 fallback
- Added support for custom certificate paths (for future UI feature)

**Impact:** Prevents attackers from intercepting and modifying ManageSieve traffic.

#### 2. Hardcoded Encryption Key (CWE-798)
**Status:** ✅ Fixed in v1.0.0
**Severity:** CRITICAL
**Location:** `PropertiesSieve.java`

**Issue:** The encryption key "KNQ4VnqF24WLe4HZJ9fB9Sth" was hardcoded in source code, visible to anyone with access to the repository. This allowed anyone to decrypt stored passwords.

**Fix:**
- Removed hardcoded key
- Implemented machine-specific key derivation using:
  - System username
  - Hostname
  - MAC address
- Hash with SHA-256 for consistent key length

**Impact:** Passwords are now encrypted with a machine-specific key that's not exposed in source code.

**Breaking Change:** ⚠️ Users must re-enter passwords after upgrading.

### HIGH Vulnerabilities Fixed

#### 3. Weak Encryption Algorithm (CWE-327)
**Status:** ✅ Fixed in v1.0.0
**Severity:** HIGH
**Location:** `PropertiesSieve.java`

**Issue:** Used weak encryption algorithm `PBEWithMD5AndDES` which is considered cryptographically broken.

**Fix:**
- Upgraded to `PBEWithHmacSHA512AndAES_256`
- Increased iterations from default to 10,000
- Uses AES-256 encryption with HMAC-SHA512

**Impact:** Passwords are now protected with industry-standard strong encryption.

#### 4. Password Displayed in Plain Text (CWE-522)
**Status:** ✅ Fixed in v1.0.0
**Severity:** HIGH
**Location:** `ActionConnect.java`

**Issue:** Password input field displayed typed characters in plain text, visible to anyone looking at the screen or in screenshots.

**Fix:**
- Replaced `JTextField` with `JPasswordField`
- Set echo character to bullet (•)
- Updated code to use `getPassword()` method

**Impact:** Passwords are now visually masked during input.

### MEDIUM Vulnerabilities Fixed

#### 5. Insecure File Permissions (CWE-732)
**Status:** ✅ Fixed in v1.0.0
**Severity:** MEDIUM
**Location:** `PropertiesSieve.java`

**Issue:** Configuration files containing encrypted passwords were created with default permissions, potentially allowing other users to read them.

**Fix:**
- Set file permissions to 600 (owner read/write only)
- Set directory permissions to 700 (owner access only)
- Implemented for POSIX systems (Linux, macOS)
- Windows uses OS-level file protection

**Impact:** Other users on the same system cannot read configuration files.

## Reporting a Vulnerability

If you discover a security vulnerability in SieveEditor, please report it by:

1. **DO NOT** open a public GitHub issue
2. Email the maintainers directly (check CLAUDE.md for contact info)
3. Or use GitHub Security Advisories (private disclosure)

Please include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if available)

We aim to respond within 48 hours and release a fix within 7 days for critical issues.

## Security Best Practices

### For Users

1. **Keep Updated:** Always use the latest version
2. **Verify Downloads:** Check SHA256 checksums of downloaded packages
3. **Use Valid Certificates:** Don't use self-signed certificates for production servers
4. **Secure Your Machine:** Since encryption keys are machine-specific, keep your computer secure
5. **Strong Server Passwords:** Use strong, unique passwords for ManageSieve accounts

### For Developers

1. **Never Commit Secrets:** Use `.gitignore` for sensitive files
2. **Code Review:** All security-related changes require review
3. **Dependency Scanning:** Run `mvn dependency:tree` and check for vulnerabilities
4. **Static Analysis:** CodeQL scans run automatically on PRs
5. **Conventional Commits:** Use `security:` prefix for security fixes

## Vulnerability History

| CVE/ID | Severity | Component | Fixed In | Disclosure |
|--------|----------|-----------|----------|------------|
| N/A    | CRITICAL | SSL Validation | v1.0.0 | 2025-11-17 |
| N/A    | CRITICAL | Hardcoded Key | v1.0.0 | 2025-11-17 |
| N/A    | HIGH     | Weak Crypto | v1.0.0 | 2025-11-17 |
| N/A    | HIGH     | Password UI | v1.0.0 | 2025-11-17 |
| N/A    | MEDIUM   | File Permissions | v1.0.0 | 2025-11-17 |

## Certificate Trust Management

### Interactive Certificate Validation (v1.0.0)

SieveEditor now includes a user-friendly certificate trust dialog for handling self-signed certificates:

**Features:**
- Displays certificate details (subject, issuer, validity dates)
- Shows SHA-256 fingerprint for manual verification
- Three trust options:
  - **Trust & Connect**: Permanently accept certificate
  - **Reject**: Permanently reject certificate
  - **Cancel**: Abort connection without storing decision

**How It Works:**
1. CA-signed certificates are validated automatically (system trust store)
2. Unknown certificates trigger the trust dialog
3. User decisions are stored in `~/.sieveprofiles/certificates.properties`
4. File permissions set to 600 (owner-only access)

**Verifying Certificates:**
```bash
# Get your server's certificate fingerprint
openssl s_client -connect your-server:4190 -starttls imap < /dev/null 2>/dev/null | \
  openssl x509 -fingerprint -sha256 -noout

# Compare with the fingerprint shown in SieveEditor's dialog
```

**Managing Trust Decisions:**
```bash
# View stored certificates
cat ~/.sieveprofiles/certificates.properties

# Reset all trust decisions
rm ~/.sieveprofiles/certificates.properties
```

## Security Roadmap

### Completed Enhancements

- [x] Custom certificate trust store UI (v1.0.0)
- [x] Interactive certificate validation dialog (v1.0.0)
- [x] SHA-256 fingerprint verification (v1.0.0)

### Planned Enhancements

- [ ] Certificate pinning for known servers
- [ ] Certificate expiration warnings
- [ ] Hardware security module (HSM) support
- [ ] Two-factor authentication support
- [ ] Audit logging of all ManageSieve operations
- [ ] Memory protection for password strings (zeroization)
- [ ] OS keychain integration (macOS Keychain, Windows Credential Manager)

## Acknowledgments

Security vulnerabilities were identified through:
- GitHub CodeQL Advanced Security scanning
- Manual security code review
- Community contributions

Thank you to all security researchers and contributors who help keep SieveEditor secure.
