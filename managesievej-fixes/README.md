# ManageSieveJ Security Fixes

This directory contains patches and documentation for security fixes to be applied to the ManageSieveJ library.

## Quick Reference

| File | Purpose | Severity |
|------|---------|----------|
| `pr31-log-sanitization-fix.patch` | Fix overly broad log redaction | HIGH |
| `pr32-resource-leak-fix.patch` | Properly close resources on disconnect | MEDIUM |

## Applying Patches

### For ManageSieveJ Maintainers

```bash
# In ManageSieveJ repository root
cd ManageSieveJ

# Apply log sanitization fix
git apply ../SieveEditor/managesievej-fixes/pr31-log-sanitization-fix.patch

# Apply resource leak fix
git apply ../SieveEditor/managesievej-fixes/pr32-resource-leak-fix.patch

# Test
mvn clean test

# Commit
git add .
git commit -m "fix(security): apply log sanitization and resource leak fixes"
```

### For SieveEditor Integration

After ManageSieveJ v0.4.0 is released:

```bash
cd SieveEditor

# Update pom.xml dependency
# Change: <version>0.3.3</version>
# To:     <version>0.4.0</version>

# Test
mvn clean install
```

## Patch Details

### PR #31 Fix: Log Sanitization

**Problem:** Pattern `line.startsWith("{")` was too broad and redacted legitimate protocol data.

**Solution:** Use precise regex pattern that matches only SASL authentication literals:
- Pattern: `^\\{\\d+\\+?\\}(\\r?\\n.*)?`
- Matches: `{16}\r\nauth-data` or `{8+}\r\ncontinued`

**Impact:** Prevents credential leakage while not obscuring legitimate protocol messages.

---

### PR #32 Fix: Resource Leak

**Problem:** `disconnect()` only set variables to null without closing underlying resources.

**Solution:** Properly call `close()` on reader, writer, and socket before nulling.

**Impact:** Prevents file descriptor exhaustion and memory leaks in long-running applications.

---

## Testing

### Log Sanitization Test

```bash
# Enable finest logging
java -Djava.util.logging.level=FINEST \
     -Djava.util.logging.ConsoleHandler.level=FINEST \
     -jar test-app.jar

# Check logs
grep "Sending line:" test.log

# Should see:
# "Sending line: LISTSCRIPTS"          ← Not redacted (normal command)
# "Sending line: <redacted SASL..."    ← Redacted (auth data)
```

### Resource Leak Test

```bash
# Monitor file descriptors
watch -n 1 'lsof -p $(pgrep -f YourApp) | wc -l'

# Perform 100 connect/disconnect cycles
for i in {1..100}; do
    echo "Cycle $i"
    # Your connect/disconnect test
done

# FD count should remain stable (not increase)
```

## See Also

- [MANAGESIEVEJ-ACTION-PLAN.md](../MANAGESIEVEJ-ACTION-PLAN.md) - Complete implementation plan
- [SECURITY-FIX-MERGE-PLAN.md](../SECURITY-FIX-MERGE-PLAN.md) - Overall security strategy
