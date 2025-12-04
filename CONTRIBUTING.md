# Contributing to SieveEditor

Thank you for your interest in contributing to SieveEditor! This document provides guidelines for contributing to the project.

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [Development Workflow](#development-workflow)
4. [Commit Message Convention](#commit-message-convention)
5. [Pull Request Process](#pull-request-process)
6. [Testing](#testing)
7. [Security](#security)

---

## Code of Conduct

Be respectful, constructive, and professional in all interactions.

---

## Getting Started

### Prerequisites

- **Java 21 LTS** (or higher)
- **Maven 3.6+**
- **Git**

### Clone Repository

```bash
git clone https://github.com/lenucksi/SieveEditor.git
cd SieveEditor
```

### Build and Run Tests

```bash
# Run local test suite
./scripts/test-local.sh

# Or manually:
mvn clean test
```

### Run Application

```bash
mvn package
java -jar target/SieveEditor-jar-with-dependencies.jar
```

---

## Development Workflow

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/my-feature`
3. **Make** your changes
4. **Test** your changes: `mvn clean verify`
5. **Commit** using [Conventional Commits](#commit-message-convention)
6. **Push** to your fork: `git push origin feature/my-feature`
7. **Create** a Pull Request

## Commit Message Convention

We use [Conventional Commits](https://www.conventionalcommits.org/) for automated changelog generation and semantic versioning.

### Format

```text
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

| Type | Description | Version Bump |
|------|-------------|--------------|
| `feat` | New feature | Minor |
| `fix` | Bug fix | Patch |
| `feat!` or `fix!` | Breaking change | Major |
| `docs` | Documentation only | None |
| `test` | Adding tests | None |
| `chore` | Maintenance | None |
| `refactor` | Code refactoring | None |
| `perf` | Performance improvement | Patch |
| `security` | Security fix | Patch |
| `deps` | Dependency update | Patch |
| `build` | Build system changes | None |
| `ci` | CI/CD configuration | None |

**Need help?** If you're using Claude Code, invoke the `conventional-commits` skill for an interactive commit message builder.

### Examples

**Feature:**

```text
feat(profiles): add multi-profile support

Allow users to manage multiple ManageSieve server profiles
with separate credentials stored in ~/.sieveprofiles/

Closes #42
```

**Bug Fix:**

```text
fix(actions): prevent array index out of bounds

Check if row is selected before accessing table data.

Fixes #57
```

**Breaking Change:**

```text
feat!: enforce SSL certificate validation

BREAKING CHANGE: Self-signed certificates are now rejected.
Import CA certificates into system trust store or use
valid certificates.

Closes #23
```

**Security Fix:**

```text
security: remove hardcoded encryption key

Move encryption key to environment variable SIEVE_ENCRYPTION_KEY.
Existing profiles will need manual migration.

Fixes #24
```

## Pull Request Process

### Before Submitting

1. ✅ Tests pass locally: `mvn clean verify`
2. ✅ Code follows existing style
3. ✅ Commit messages follow Conventional Commits
4. ✅ Update documentation if needed
5. ✅ Add tests for new features

### PR Description Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update

## Testing
Describe testing performed

## Checklist
- [ ] Tests pass locally
- [ ] Commit messages follow Conventional Commits
- [ ] Documentation updated
- [ ] No merge conflicts
```

### Review Process

1. **Automated Checks:** CI must pass (tests on Linux, Windows, macOS)
2. **Code Review:** At least one maintainer approval required
3. **Security Scan:** OpenSSF Scorecard and OWASP checks
4. **Merge:** Squash and merge (preserves clean commit history)

---

## Testing

### Running Tests

**All tests:**

```bash
mvn test
```

**Specific test class:**

```bash
mvn test -Dtest=PropertiesSieveTest
```

**Specific test method:**

```bash
mvn test -Dtest=PropertiesSieveTest#shouldSaveAndLoadProperties
```

### Writing Tests

- Use JUnit 5 with AssertJ assertions
- Follow AAA pattern (Arrange-Act-Assert)
- Use descriptive test names: `shouldDoSomethingWhenCondition()`
- See [README-TESTS.md](README-TESTS.md) for detailed guidelines

**Example:**

```java
@Test
void shouldEncryptPasswordWhenSaving() {
    // Arrange
    PropertiesSieve config = new PropertiesSieve();
    config.setPassword("plaintextpassword");

    // Act
    config.write();

    // Assert
    String rawContent = Files.readString(profileFile.toPath());
    assertThat(rawContent).doesNotContain("plaintextpassword");
    assertThat(rawContent).contains("ENC(");
}
```

### Test Coverage Goals

- **Overall:** 70%+
- **Critical components:** 80%+
- See [README-TESTS.md](README-TESTS.md) for component-specific targets

---

## Security

### Reporting Vulnerabilities

**DO NOT** open public issues for security vulnerabilities.

Instead:

1. Email security issues to project maintainers (see `SECURITY.md`)
2. Include:
   - Description of vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

### Security Best Practices

- ✅ Never commit secrets, API keys, or passwords
- ✅ Use GitHub Secrets for sensitive data in workflows
- ✅ Validate all user inputs
- ✅ Use parameterized queries (prevent injection)
- ✅ Follow OWASP Top 10 guidelines

### Dependencies

- Keep dependencies up-to-date
- Review Dependabot PRs promptly
- Check for known vulnerabilities: `mvn org.owasp:dependency-check-maven:check`

---

## Build and Package Locally

### JAR (All Platforms)

```bash
mvn clean package
java -jar target/SieveEditor-jar-with-dependencies.jar
```

### Linux (DEB)

```bash
# Prerequisites: fakeroot
sudo apt-get install fakeroot

# Build JAR first
mvn clean package

# Create DEB package
jpackage --input target \
  --main-jar SieveEditor-jar-with-dependencies.jar \
  --main-class de.febrildur.sieveeditor.Application \
  --name SieveEditor \
  --app-version 0.9.3 \
  --type deb \
  --linux-shortcut
```

### Windows (MSI)

```powershell
# Prerequisites: WiX Toolset 3.0+

# Build JAR first
mvn clean package

# Create MSI
jpackage --input target `
  --main-jar SieveEditor-jar-with-dependencies.jar `
  --main-class de.febrildur.sieveeditor.Application `
  --name SieveEditor `
  --app-version 0.9.3 `
  --type msi `
  --win-menu `
  --win-shortcut
```

### macOS (DMG)

```bash
# Prerequisites: Xcode Command Line Tools

# Build JAR first
mvn clean package

# Create DMG
jpackage --input target \
  --main-jar SieveEditor-jar-with-dependencies.jar \
  --main-class de.febrildur.sieveeditor.Application \
  --name SieveEditor \
  --app-version 0.9.3 \
  --type dmg
```

---

## Documentation

- **Test Strategy:** See [README-TESTS.md](README-TESTS.md)
- **Security Policy:** See [SECURITY.md](SECURITY.md)
- **API Docs:** Generate with `mvn javadoc:javadoc`

---

## Questions?

- Open a [Discussion](https://github.com/lenucksi/SieveEditor/discussions)
- Check existing [Issues](https://github.com/lenucksi/SieveEditor/issues)
- Read the [Wiki](https://github.com/lenucksi/SieveEditor/wiki) (if available)

---

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.

---

**Thank you for contributing to SieveEditor!** 🎉
