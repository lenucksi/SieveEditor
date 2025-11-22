# SieveEditor Research Reports

This document summarizes the research conducted for new features in SieveEditor.

## Table of Contents

1. [Sieve Linting/Validation Libraries](#sieve-lintingvalidation-libraries)
2. [Template Variable Systems for Java](#template-variable-systems-for-java)

---

## Sieve Linting/Validation Libraries

**Research Date:** November 2025
**Purpose:** Investigate RFC-compliant Java libraries for local Sieve script validation.

### Recommendation: Apache jSieve

**Maven Coordinates:**

```xml
<dependency>
    <groupId>org.apache.james</groupId>
    <artifactId>apache-jsieve-core</artifactId>
    <version>0.8</version>
</dependency>
```

### Why Apache jSieve?

1. **Native Java** - Direct Maven integration, no JNI or subprocess calls
2. **Production-tested** - Used in Apache James email server
3. **RFC 5228 compliant** - Core Sieve specification support
4. **Offline validation** - Works without server connection
5. **Simple API** - `SieveFactory.parse()` validates scripts with line numbers

### Supported Extensions

- RFC 5228 (Core Sieve)
- RFC 5173 (Body Extension)
- RFC 5229 (Variables Extension - partial)

### Integration Example

```java
import org.apache.jsieve.SieveFactory;
import org.apache.jsieve.exception.SieveException;

public class SieveValidator {
    public ValidationResult validate(String script) {
        try {
            SieveFactory factory = SieveFactory.getInstance();
            factory.parse(new ByteArrayInputStream(script.getBytes()));
            return ValidationResult.success();
        } catch (SieveException e) {
            return ValidationResult.error(e.getLineNumber(), e.getMessage());
        }
    }
}
```

### Alternatives Considered

| Library | Language | Local Validation | Maven Available | Verdict |
|---------|----------|------------------|-----------------|---------|
| **jSieve** | Java | Yes | Yes | **Recommended** |
| check-sieve | C++ | Yes | No | Best features, wrong language |
| ms4j | Java | No (server only) | No | Incomplete |
| sieve-test | C | Yes | N/A | External binary |

### Future Enhancement

For comprehensive extension support (27+ RFCs), consider optionally calling `check-sieve` as an external tool via `ProcessBuilder` for users who have it installed.

---

## Template Variable Systems for Java

**Research Date:** November 2025
**Purpose:** Find simple variable substitution for Sieve templates.

### Recommendation: Apache Commons Text StringSubstitutor

**Maven Coordinates:**

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-text</artifactId>
    <version>1.10.0</version>
</dependency>
```

### Why StringSubstitutor?

1. **Perfect fit** - Designed specifically for `${variable}` substitution
2. **Zero configuration** - Works immediately out of the box
3. **Default values** - Supports `${var:-defaultValue}` syntax
4. **Lightweight** - ~180KB dependency
5. **Trusted source** - Apache Commons library

### Implementation Example

```java
import org.apache.commons.text.StringSubstitutor;
import java.util.Map;

public class TemplateVariableService {

    public String substituteVariables(String template, Map<String, String> variables) {
        StringSubstitutor substitutor = new StringSubstitutor(variables);
        substitutor.setEnableUndefinedVariableException(false);
        return substitutor.replace(template);
    }
}
```

### User Flow

```text
User selects template → Dialog prompts for variables → Variables substituted → Template inserted
```

### Alternatives Considered

| Library | Simplicity | Dependencies | Syntax | Use Case |
|---------|------------|--------------|--------|----------|
| **StringSubstitutor** | 10/10 | 1 | `${var}` | **Simple substitution** |
| Custom Regex | 8/10 | 0 | `${var}` | No dependencies |
| Mustache.java | 6/10 | 0 | `{{var}}` | Logic needed |
| FreeMarker | 5/10 | 1 | `${var}` | Complex templates |

### Implementation Complexity

- StringSubstitutor: 30 minutes
- Custom Regex: 45 minutes
- Mustache.java: 2-3 hours
- FreeMarker: 3-4 hours

### Future Enhancement

If template complexity grows, consider migrating to Mustache.java for conditional logic support (e.g., optional sections based on email type).

---

## Common Sieve Patterns Research

**Research Date:** November 2025
**Purpose:** Identify useful templates for the Insert menu.

### Implemented Templates (14 total)

#### Basic Filtering

1. **Spam Filter to Folder** - Route spam-flagged emails
2. **Fileinto by Subject** - Route by subject keywords
3. **Fileinto by Sender** - Route by sender address
4. **Filter by Domain** - Route by sender domain

#### Automation

1. **Vacation Auto-Reply** - Out-of-office responses
2. **Notification Filter** - Separate automated emails

#### Organization

1. **Mailing List Filter** - Route by List-Id header
2. **Priority Flagging** - Flag important messages
3. **Multiple Conditions** - Complex AND/OR rules

#### Advanced

1. **Reject by Size** - Reject oversized messages
2. **Discard by Sender** - Silently block senders
3. **Duplicate Detection** - Handle duplicate messages
4. **Subaddress Routing** - Route by plus-address tag
5. **Complete Starter Script** - Full production template

### Template Directory

User templates location (XDG-compliant):

- **Linux:** `~/.local/share/sieveeditor/templates/`
- **Windows:** `%LOCALAPPDATA%/febrildur/sieveeditor/templates/`
- **macOS:** `~/Library/Application Support/sieveeditor/templates/`

### Extension Reference

| Extension | RFC | Common Actions |
|-----------|-----|----------------|
| fileinto | 5228 | File to folder |
| imap4flags | 5232 | Set/add/remove flags |
| variables | 5229 | Store/reuse values |
| vacation | 5230 | Auto-replies |
| duplicate | Dovecot | Detect duplicates |
| subaddress | 5233 | Plus-addressing |

---

## Implementation Status

### Completed Features

- [x] Local File Load/Save (Ctrl+L, Ctrl+Shift+S)
- [x] Template Insertion Menu (14 built-in templates)
- [x] User Template Support (XDG-compliant directory)
- [x] Research: Sieve validation libraries
- [x] Research: Template variable systems

### Future Enhancements

- [ ] Integrate Apache jSieve for local validation
- [ ] Add variable substitution to templates
- [ ] Real-time syntax checking in editor
- [ ] Template wizard for guided creation
