---
description: Interactive helper for creating conventional commit messages
---

# Conventional Commits Assistant

Help the developer create a well-formed conventional commit message.

## Process

1. **Ask about the change type**:
   - What type of change is this?
     - `feat` - New feature (minor version bump)
     - `fix` - Bug fix (patch version bump)
     - `perf` - Performance improvement (patch bump)
     - `security` - Security fix (patch bump)
     - `deps` - Dependency update (patch bump)
     - `docs` - Documentation only (no version bump)
     - `test` - Adding/updating tests (no bump)
     - `chore` - Maintenance tasks (no bump)
     - `refactor` - Code refactoring (no bump)
     - `build` - Build system changes (no bump)
     - `ci` - CI/CD configuration (no bump)

2. **Ask about the scope** (optional):
   - What component does this affect?
   - Common scopes:
     - `profiles` - Profile management
     - `actions` - UI action handlers
     - `tokenizer` - Syntax highlighting
     - `connection` - ManageSieve protocol
     - `ui` - User interface components
     - `security` - Security-related
     - `tests` - Test infrastructure
     - `build` - Build configuration
     - `release` - Release automation

3. **Ask for subject line**:
   - Briefly describe the change (imperative mood, lowercase, no period)
   - Example: "add multi-profile support" not "Added multi-profile support."
   - Keep under 80 characters

4. **Ask if breaking change**:
   - Does this break existing functionality?
   - If yes, use `!` after type/scope: `feat!:` or `feat(profiles)!:`

5. **Ask for body** (optional):
   - Provide more details about what changed and why
   - Wrap at 72 characters per line
   - Use bullet points if multiple changes

6. **Ask for footer** (optional):
   - Reference issues: `Fixes #123`, `Closes #45`, `Refs #67`
   - Note breaking changes: `BREAKING CHANGE: description`

7. **Generate and display the commit message**:

```
<type>(<scope>): <subject>

<body>

<footer>
```

8. **Validate the message**:
   - Check format matches conventional commits spec
   - Verify subject length <= 80 chars
   - Ensure type is valid
   - Confirm breaking change notation if needed

9. **Provide git command**:
   ```bash
   git commit -m "<generated message>"
   ```

   Or for multi-line commits:
   ```bash
   git commit -m "<subject>" -m "<body>" -m "<footer>"
   ```

10. **Remind about the git hook**:
    - The commit-msg hook will validate this format
    - If hook is not enabled: `git config core.hooksPath .githooks`

## Examples to Show

### Simple Feature
```
feat(profiles): add profile import functionality
```

### Bug Fix with Issue Reference
```
fix(connection): prevent timeout on slow networks

Add configurable timeout parameter (default 30s) to avoid
connection failures on slow or unreliable networks.

Fixes #57
```

### Breaking Change
```
feat!: enforce SSL certificate validation

BREAKING CHANGE: Self-signed certificates are now rejected by default.
Users must either import CA certificates into the system trust store
or use valid certificates signed by a trusted CA.

This improves security by preventing MITM attacks.

Closes #23
```

### Security Fix
```
security: remove hardcoded encryption key

Move encryption key to environment variable SIEVE_ENCRYPTION_KEY.
Existing profiles will need manual migration using the provided
migration script.

Fixes CVE-2025-XXXXX
```

### Dependency Update
```
deps: update rsyntaxtextarea to 3.6.0

Includes bug fixes and improved syntax highlighting for Sieve scripts.
```

## Validation Rules

- Type must be one of the allowed types
- Subject line must be lowercase (except acronyms)
- Subject must use imperative mood ("add" not "added")
- Subject must not end with a period
- Subject should be <= 80 characters
- Body lines should wrap at 72 characters
- Breaking changes must include `!` and/or `BREAKING CHANGE:` footer

## Error Messages to Help With

If the git hook rejects a commit, help the developer understand:
- What's wrong with the format
- How to fix it
- Provide the corrected version
- Explain why conventional commits are important (automated releases, changelogs)

---

After helping create the commit message, ask if they'd like to proceed with the commit or make changes.
