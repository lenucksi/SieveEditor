# ManageSieveJ Security Fix Action Plan

## Overview

Full security fix implementation for ManageSieveJ v0.4.0 release.

## Status Summary

| PR | Issue | Severity | Action | Status |
|----|-------|----------|--------|--------|
| #29 | Hostname Verification | HIGH | Merge as-is | ✅ Ready |
| #30 | TrustManager Fix | CRITICAL | Skip (superseded) | ⏭️ Skip |
| #31 | Log Sanitization | HIGH | Fix & merge | 🔧 Needs fix |
| #32 | Resource Leak | MEDIUM | Reject & rewrite | ❌ Reject |
| #33 | Boxed Variable | LOW | Merge as-is | ✅ Ready |

---

## Action Items

### 1. ✅ Merge PR #29: Hostname Verification (Ready)

**No changes needed** - merge as-is.

```bash
# Review and merge PR #29
gh pr review 29 --approve
gh pr merge 29 --squash
```

**What it does:**

- Enables hostname verification for SSL/TLS
- Prevents MITM with valid certs for wrong hostnames

---

### 2. ⏭️ Skip PR #30: TrustManager Fix

**Action:** Close PR with explanation.

**Reason:** SieveEditor now has comprehensive SSL/TLS handling with interactive certificate trust dialog. ManageSieveJ's basic implementation is sufficient when used with SieveEditor's wrapper.

```bash
# Close PR #30 with comment
gh pr comment 30 --body "Closing this PR. The SSL/TLS certificate validation is now handled comprehensively in SieveEditor v1.0.0 with an interactive certificate trust dialog. ManageSieveJ's current implementation works correctly when used with SieveEditor's SSLSocketFactory wrapper."

gh pr close 30
```

---

### 3. 🔧 Fix PR #31: Log Sanitization

**Current Issue:** Pattern too broad

```java
// Current (TOO BROAD):
if (line.startsWith("{")) {
    log.log(Level.FINEST, "Sending line: <redacted>");
}
```

**Fixed Version:** Narrow pattern to match only SASL authentication

```java
// Fixed (PRECISE):
if (line.matches("^\\{\\d+\\+?\\}(\\r?\\n.*)?")) {
    log.log(Level.FINEST, "Sending line: <redacted SASL authentication data>");
} else {
    log.log(Level.FINEST, "Sending line: {0}", line);
}
```

**Pattern Explanation:**

- `^` - Start of line
- `\\{` - Literal opening brace
- `\\d+` - One or more digits (byte count)
- `\\+?` - Optional plus sign (for continuation)
- `\\}` - Literal closing brace
- `(\\r?\\n.*)?` - Optional CRLF and continuation data

**Files to Fix:**

```text
src/main/java/com/fluffypeople/managesieve/ManageSieveClient.java
```

**Action Steps:**

```bash
# 1. Checkout PR branch
git fetch origin pull/31/head:pr-31-log-sanitization
git checkout pr-31-log-sanitization

# 2. Apply fix (see code below)
# Edit the file with the corrected pattern

# 3. Test
mvn test

# 4. Commit
git add .
git commit -m "fix(security): narrow log redaction pattern to SASL auth only

The previous pattern 'startsWith(\"{\"})' was too broad and could
redact legitimate protocol data. New pattern matches only SASL
authentication format: {digit+}CRLF

Pattern: ^\\{\\d+\\+?\\}(\\r?\\n.*)?

This matches ManageSieve SASL authentication literals like:
- {16}\\r\\nusername:password
- {8+}\\r\\ncontinue

Refs: RFC 5804 Section 1.2, CWE-532
"

# 5. Push
git push origin pr-31-log-sanitization

# 6. Request re-review
gh pr comment 31 --body "Updated with narrower pattern that only redacts SASL authentication data."
```

**Code Fix:**

```java
// In ManageSieveClient.java, find the log redaction code and replace with:

private void sendLine(String line) throws IOException {
    // Redact SASL authentication data from logs
    // Pattern matches: {digit+}CRLF or {digit+}+CRLF (continuation)
    if (line.matches("^\\{\\d+\\+?\\}(\\r?\\n.*)?")) {
        log.log(Level.FINEST, "Sending line: <redacted SASL authentication data>");
    } else {
        log.log(Level.FINEST, "Sending line: {0}", line);
    }

    writer.write(line);
    writer.write("\r\n");
    writer.flush();
}
```

**Testing:**

```bash
# Test with normal commands (should NOT be redacted)
PUTSCRIPT "test" {50}
LISTSCRIPTS
LOGOUT

# Test with authentication (SHOULD be redacted)
AUTHENTICATE "PLAIN" {16}
<base64-auth-data>
```

---

### 4. ❌ Reject & Rewrite PR #32: Resource Leak

**Current Issue:** Doesn't actually close resources

```java
// Current (WRONG):
public void disconnect() throws IOException {
    socket = null;
    reader = null;
    writer = null;
    // NO ACTUAL CLOSING!
}
```

**Correct Fix:**

```java
// Correct:
public void disconnect() throws IOException {
    if (reader != null) {
        try {
            reader.close(); // Closes entire chain: reader -> inputStream -> socket
        } catch (IOException e) {
            log.log(Level.WARNING, "Error closing reader during disconnect", e);
        }
    }

    if (writer != null) {
        try {
            writer.close(); // Closes: writer -> outputStream
        } catch (IOException e) {
            log.log(Level.WARNING, "Error closing writer during disconnect", e);
        }
    }

    if (socket != null && !socket.isClosed()) {
        try {
            socket.close();
        } catch (IOException e) {
            log.log(Level.WARNING, "Error closing socket during disconnect", e);
        }
    }

    // Set to null after closing
    socket = null;
    reader = null;
    writer = null;
}
```

**Action Steps:**

```bash
# 1. Comment on PR #32 explaining the issue
gh pr comment 32 --body "This PR doesn't actually fix the resource leak. Setting variables to null doesn't close the underlying resources. The disconnect() method needs to call close() on reader, writer, and socket. I'll create a new PR with the proper fix."

# 2. Close PR #32
gh pr close 32

# 3. Create new branch for proper fix
git checkout -b fix/resource-leak-proper
```

**New PR Code:**

Create file: `resource-leak-fix.patch`

```java
// In ManageSieveClient.java

/**
 * Disconnects from the server and closes all resources.
 * This method ensures proper cleanup of socket, reader, and writer resources.
 */
public void disconnect() throws IOException {
    IOException firstException = null;

    // Close reader (closes input stream chain)
    if (reader != null) {
        try {
            reader.close();
            log.log(Level.FINE, "Reader closed");
        } catch (IOException e) {
            log.log(Level.WARNING, "Error closing reader during disconnect", e);
            if (firstException == null) firstException = e;
        }
    }

    // Close writer (closes output stream chain)
    if (writer != null) {
        try {
            writer.close();
            log.log(Level.FINE, "Writer closed");
        } catch (IOException e) {
            log.log(Level.WARNING, "Error closing writer during disconnect", e);
            if (firstException == null) firstException = e;
        }
    }

    // Close socket if still open
    if (socket != null && !socket.isClosed()) {
        try {
            socket.close();
            log.log(Level.FINE, "Socket closed");
        } catch (IOException e) {
            log.log(Level.WARNING, "Error closing socket during disconnect", e);
            if (firstException == null) firstException = e;
        }
    }

    // Null out references after closing
    socket = null;
    reader = null;
    writer = null;

    // Re-throw first exception if any occurred
    if (firstException != null) {
        throw firstException;
    }
}
```

**Commit & Push:**

```bash
git add .
git commit -m "fix(resource-leak): properly close socket and streams in disconnect

The previous implementation only set variables to null without closing
the underlying resources, causing resource leaks.

Changes:
- Call close() on reader, writer, and socket before nulling
- Collect and re-throw first exception if multiple close operations fail
- Add proper logging for troubleshooting
- Null out references only after successful close

Fixes: Resource leak in disconnect() method
Refs: CWE-404 Improper Resource Shutdown
"

git push origin fix/resource-leak-proper

# Create PR
gh pr create \
  --title "fix(resource-leak): properly close resources in disconnect()" \
  --body-file resource-leak-pr-description.md
```

---

### 5. ✅ Merge PR #33: Boxed Variable (Ready)

**No changes needed** - merge as-is.

```bash
gh pr review 33 --approve
gh pr merge 33 --squash
```

**What it does:**

- Removes unnecessary boxing of primitive long
- Code quality improvement

---

## Testing Strategy

### Integration Testing

```bash
# 1. Build ManageSieveJ with all fixes
mvn clean install

# 2. Update SieveEditor dependency
cd ../SieveEditor
# Edit pom.xml to use new ManageSieveJ version

# 3. Test complete flow
mvn clean package
java -jar target/SieveEditor-jar-with-dependencies.jar

# Test scenarios:
# - Connect with CA-signed cert (should work)
# - Connect with self-signed cert (interactive dialog)
# - Disconnect (verify no resource leaks)
# - Check logs for proper redaction
```

### Resource Leak Testing

```bash
# Monitor file descriptors during connect/disconnect cycles
while true; do
    lsof -p $(pgrep -f SieveEditor) | wc -l
    sleep 1
done

# Should remain stable (no increasing FD count)
```

### Log Sanitization Testing

```bash
# Enable finest logging
java -Djava.util.logging.config.file=logging.properties \
     -jar SieveEditor.jar

# Check logs don't contain passwords
grep -i "password" ~/.sieve/logs/*.log
# Should only show: "<redacted SASL authentication data>"
```

---

## Release Process

### Version Bump

```bash
# ManageSieveJ: 0.3.3 → 0.4.0
# Reason: Multiple bug fixes and security improvements

# Update pom.xml
sed -i 's/<version>0.3.3<\/version>/<version>0.4.0<\/version>/' pom.xml

git add pom.xml
git commit -m "chore(release): bump version to 0.4.0"
git tag -a v0.4.0 -m "Release v0.4.0

Security Fixes:
- Hostname verification enabled
- Log sanitization narrowed to SASL only
- Resource leak in disconnect fixed
- Unnecessary boxing removed

See CHANGELOG.md for full details"

git push origin master --tags
```

### Release Notes

```markdown
# ManageSieveJ v0.4.0

## 🔒 Security Fixes

### HIGH Severity
- **Hostname Verification** - Enabled SSL/TLS hostname verification (PR #29)
- **Log Sanitization** - Fixed overly broad log redaction pattern (PR #31)

### MEDIUM Severity
- **Resource Leak** - Properly close socket/streams in disconnect() (PR #32 rewrite)

## 🐛 Bug Fixes
- Remove unnecessary boxing of primitive long type (PR #33)

## 📝 Notes

**Breaking Changes:** None

**Compatibility:** Java 11+

**SieveEditor Integration:** Works with SieveEditor v1.0.0+ which provides interactive certificate trust dialog.

## 🙏 Credits
- Security issues identified by GitHub CodeQL
- Community contributions via PRs
```

### Update SieveEditor Dependency

```bash
cd ../SieveEditor

# Edit pom.xml
# Change ManageSieveJ version from 0.3.3 to 0.4.0

git add pom.xml
git commit -m "deps: upgrade ManageSieveJ to v0.4.0

Includes security fixes:
- Hostname verification
- Improved log sanitization
- Resource leak fix
"
```

---

## Timeline

**Week 1:**

- Day 1-2: Fix PR #31 (log sanitization)
- Day 3-4: Rewrite PR #32 (resource leak)
- Day 5: Testing

**Week 2:**

- Day 1: Merge all PRs
- Day 2: Final testing
- Day 3: Release v0.4.0
- Day 4: Update SieveEditor dependency
- Day 5: Integration testing

---

## Success Criteria

- [ ] PR #29 merged (hostname verification)
- [ ] PR #30 closed with explanation
- [ ] PR #31 fixed and merged (log sanitization)
- [ ] PR #32 rejected, new PR created and merged (resource leak)
- [ ] PR #33 merged (boxed variable)
- [ ] All tests passing
- [ ] No resource leaks detected
- [ ] Logs properly redact passwords
- [ ] Version tagged v0.4.0
- [ ] Release published
- [ ] SieveEditor updated to use v0.4.0

---

## Rollback Plan

If issues discovered:

```bash
# Revert to v0.3.3
git revert <commit-hash>
git tag -d v0.4.0
git push origin :refs/tags/v0.4.0
```

---

**Document Status:** Ready for execution
**Last Updated:** 2025-11-17
**Assignee:** Development team
