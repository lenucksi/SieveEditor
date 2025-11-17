# Migration Guide: v0.9.x → v1.0.0

## Overview

Version 1.0.0 includes **critical security fixes** that introduce breaking changes. This guide will help you migrate smoothly.

## Breaking Changes

### 1. Encrypted Passwords Must Be Re-entered

**Why:** The hardcoded encryption key has been replaced with a secure machine-specific key.

**Impact:** All existing encrypted passwords will fail to decrypt.

**Action Required:**

#### Before Upgrading
```bash
# 1. Note all your ManageSieve server passwords
#    Open SieveEditor and record your passwords

# 2. Backup your profiles (optional but recommended)
cp -r ~/.sieveprofiles ~/.sieveprofiles.backup
```

#### After Upgrading
1. Launch SieveEditor v1.0.0
2. For each profile:
   - Open the connection dialog
   - Server, username, and port will be preserved
   - Re-enter the password (it will be empty)
   - Click "Connect" or "Save"
3. Passwords will be re-encrypted with the new secure key

**What Happens:** Old passwords fail to decrypt gracefully and become empty strings. No error messages will appear.

### 2. Self-Signed Certificates Now Require User Approval

**Why:** SSL certificate validation is now properly enforced to prevent MITM attacks.

**Impact:** Connections to servers with self-signed or invalid certificates will prompt for user approval.

**What Happens:**

When you connect to a server with an unknown certificate, you'll see a dialog showing:
- Certificate subject and issuer
- Validity period
- SHA-256 fingerprint
- Three options:
  - **Trust & Connect**: Accept the certificate permanently
  - **Reject**: Reject the certificate permanently
  - **Cancel**: Abort this connection without storing a decision

**Action Required:**

#### Option 1: Trust Your Self-Signed Certificate (Recommended)
1. Connect to your server
2. When the certificate dialog appears, verify the fingerprint matches your server
3. Click "Trust & Connect"
4. Certificate will be remembered for future connections

**Verifying the Fingerprint:**
```bash
# On your server, get the certificate fingerprint
openssl s_client -connect localhost:4190 -starttls imap < /dev/null 2>/dev/null | \
  openssl x509 -fingerprint -sha256 -noout

# Compare with the fingerprint shown in the dialog
```

#### Option 2: Use a Valid CA-Signed Certificate (Most Secure)
- Obtain a free certificate from Let's Encrypt
- Or purchase a certificate from a trusted CA
- Configure your mail server with the valid certificate
- No user interaction needed - works automatically

#### Managing Trusted Certificates

Your certificate decisions are stored in:
```
~/.sieveprofiles/certificates.properties
```

To reset all trust decisions:
```bash
rm ~/.sieveprofiles/certificates.properties
```

## What's Fixed

### Security Improvements

✅ **No more hardcoded encryption key** - Machine-specific key derived from username + hostname + MAC address

✅ **Strong encryption** - Upgraded from `PBEWithMD5AndDES` to `PBEWithHmacSHA512AndAES_256`

✅ **Proper SSL/TLS validation** - Certificates are now validated against system CAs

✅ **Password masking** - Passwords no longer visible in plain text during input

✅ **Secure file permissions** - Configuration files set to 600/700 on Linux/macOS

### Non-Breaking Improvements

✅ **TLS 1.3 support** - With automatic fallback to TLS 1.2

✅ **Better error handling** - More informative error messages

✅ **Comprehensive logging** - Helps diagnose connection issues

## Migration Scenarios

### Scenario 1: Home User with Valid Certificate

**Before:**
- ManageSieve server with CA-signed certificate
- One saved profile with password

**Migration Steps:**
1. Upgrade to v1.0.0
2. Launch SieveEditor
3. Open connection dialog
4. Re-enter password
5. ✅ Done!

**Time:** < 1 minute

---

### Scenario 2: Developer with Self-Signed Certificate

**Before:**
- Local Dovecot server with self-signed cert
- Multiple saved profiles

**Migration Steps:**
1. **Option A:** Replace self-signed cert with Let's Encrypt
   ```bash
   certbot certonly --standalone -d mail.example.com
   # Configure Dovecot to use the new cert
   ```
2. **Option B:** Wait for v1.1.0 custom cert support
3. Upgrade to v1.0.0
4. Re-enter all passwords

**Time:** 5-30 minutes (depending on certificate setup)

---

### Scenario 3: System Administrator (Multiple Machines)

**Before:**
- 10+ machines with SieveEditor installed
- Shared server with valid certificate

**Migration Steps:**
1. Test upgrade on one machine first
2. Verify connection works with re-entered password
3. Roll out to remaining machines
4. Provide users with migration instructions
5. Passwords are machine-specific (different key per machine)

**Time:** 1 hour + (depending on deployment method)

---

## Rollback Instructions

If you encounter critical issues, you can rollback:

### Option 1: Downgrade to v0.9.2.6

```bash
# Download previous version
wget https://github.com/lenucksi/SieveEditor/releases/download/v0.9.2.6/SieveEditor.jar

# Or build from source
git checkout v0.9.2.6
mvn package
```

### Option 2: Restore Backup

```bash
# Restore old profile files
rm -rf ~/.sieveprofiles
mv ~/.sieveprofiles.backup ~/.sieveprofiles
```

⚠️ **Warning:** Downgrading leaves you with known security vulnerabilities. Only do this temporarily.

## Troubleshooting

### Problem: "Failed to decrypt password"

**Cause:** Password was encrypted with old hardcoded key

**Solution:** Re-enter the password. This is expected behavior.

---

### Problem: "Can't start SSL: Certificate validation failed"

**Cause:** Server uses self-signed or invalid certificate

**Solution:**
1. Verify server certificate is valid: `openssl s_client -connect mail.example.com:4190 -starttls imap`
2. Check certificate expiration
3. Ensure hostname matches certificate CN/SAN
4. Use valid CA-signed certificate

---

### Problem: "Connection refused"

**Cause:** Unrelated to security fixes - server not running or firewall

**Solution:**
1. Check server is running: `systemctl status dovecot`
2. Test port: `telnet mail.example.com 4190`
3. Check firewall rules

---

### Problem: Password keeps getting cleared

**Cause:** Encryption key changes if username/hostname/MAC changes

**Solution:**
1. Ensure your username hasn't changed
2. Ensure your hostname is stable
3. If using VMs/containers, MAC address should be stable
4. Check logs: `~/.sieve/logs` for encryption warnings

---

## FAQ

### Q: Why can't I decrypt my old passwords?

**A:** The old hardcoded key "KNQ4VnqF24WLe4HZJ9fB9Sth" was exposed in source code, making all passwords vulnerable. The new machine-specific key is secure but incompatible.

### Q: Will my passwords work on another machine?

**A:** No. Each machine has a unique encryption key. This is a security feature - if someone copies your `.sieveprofiles` directory, they can't decrypt passwords on their machine.

### Q: What if I change my hostname?

**A:** The encryption key will change, and passwords will become undecryptable. You'll need to re-enter them. This is a known limitation - future versions may add migration support.

### Q: Can I export/import profiles?

**A:** Currently no. Profile files are machine-specific. Future versions may add encrypted export/import.

### Q: When will custom certificate support be added?

**A:** Planned for v1.1.0. The infrastructure is already in place - we just need to build the UI.

### Q: Is the new encryption strong enough?

**A:** Yes. We use:
- `PBEWithHmacSHA512AndAES_256` (industry standard)
- 10,000 PBKDF2 iterations
- AES-256 encryption
- SHA-512 HMAC

This meets NIST and OWASP recommendations.

---

## Getting Help

If you encounter issues:

1. **Check Logs:**
   ```bash
   tail -f ~/.sieve/logs/sieveeditor.log
   ```

2. **Search Issues:**
   - https://github.com/lenucksi/SieveEditor/issues

3. **Ask for Help:**
   - GitHub Discussions: https://github.com/lenucksi/SieveEditor/discussions
   - Include: SieveEditor version, OS, server type, error message

4. **Report Bugs:**
   - https://github.com/lenucksi/SieveEditor/issues/new

---

## Timeline

- **2025-11-17:** v1.0.0 released with security fixes
- **2025-12-01:** v1.1.0 planned (custom certificate UI)
- **2026-01-01:** v1.2.0 planned (OS keychain integration)

---

## Acknowledgments

Thank you for upgrading to v1.0.0 and prioritizing security!

We understand breaking changes are inconvenient, but these fixes protect your ManageSieve credentials from serious vulnerabilities.

---

**Questions?** Open a discussion: https://github.com/lenucksi/SieveEditor/discussions
