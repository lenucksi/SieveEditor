# Agent Instructions — Anti-Slop Guidelines

This project uses `aislop` to detect AI-generated code patterns that compile and pass tests but reduce code quality. Score target: **≥80/100**.

## Mandatory checks before completing any task

```bash
bun x aislop scan
```

Fix or suppress all findings. The score must not regress from the baseline.

## Patterns to never introduce

### Swallowed exceptions

```java
// ❌ NEVER — empty catch hides failures
try { ... } catch (Exception e) {}

// ✅ ALWAYS — log with context
try { ... } catch (Exception e) {
    LOGGER.log(Level.WARNING, "message: {0}", param);
}
```

### Bare except (Python)

```python
# ❌ NEVER
except:

# ✅ ALWAYS
except ValueError:
```

### Narrative comments

```java
// ❌ NEVER — decorative section headers that restate the obvious
// --- Match-Type Tags ---

// ✅ OK — actual rationale that isn't obvious from the code
// Must flush before reconnect to avoid stale state
```

### Trivial comments

```java
// ❌ NEVER — restating what the code does
String lastProfile = PropertiesSieve.getLastUsedProfile(); // Load last used profile

// ✅ NO COMMENT NEEDED — code is self-explanatory
```

### System.err.println for debug logging

```java
// ❌ NEVER
System.err.println("Loaded profile: " + name);

// ✅ ALWAYS — use java.util.logging.Logger
LOGGER.log(Level.FINE, "Loaded profile: {0}", name);
```

### TODO without issue reference

```java
// ❌ NEVER
// TODO: Re-enable when fixed

// ✅ ALWAYS — with ticket number
// TODO(#117): Re-enable when fixed
```

### Generic names

```java
// ❌ NEVER
var data = fetch();
processData(data);

// ✅ ALWAYS — descriptive
var scriptContent = fetchActiveScript();
validateScriptSyntax(scriptContent);
```

## Suppress directives (only when justified)

Use inline suppress when the code is intentionally correct but aislop can't know:

```java
// aislop-ignore-next-line complexity/function-too-long -- Swing GUI builder, inherently long
// aislop-ignore-file complexity/function-too-long -- Tokenizer state machine
```

Always add a `-- reason` comment. Never suppress swallowed-exception, bare-except, or security rules.
