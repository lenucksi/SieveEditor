# SieveEditor - Development Guide for Claude Code

**SieveEditor** is a desktop Java application for editing and managing Sieve mail filter scripts on ManageSieve servers.

## Project Overview

### What is SieveEditor?

SieveEditor provides a graphical interface to:

- Connect to ManageSieve servers (RFC 5804)
- Edit Sieve scripts with syntax highlighting
- Manage multiple server profiles
- Upload/download scripts securely
- Store credentials with encryption

### Target Users

System administrators and power users who manage email filtering rules on mail servers supporting the ManageSieve protocol (Dovecot, Cyrus IMAP, etc.).

## Technical Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Java | 21 LTS |
| **Build System** | Maven | 3.6+ |
| **UI Framework** | Swing | Java Standard Library |
| **Syntax Highlighting** | RSyntaxTextArea | 3.6.0 |
| **ManageSieve Client** | ManageSieveJ | 0.3.3 (via JitPack) |
| **Password Encryption** | Jasypt | 1.9.3 |
| **Testing Framework** | JUnit 5 | 6.0.1 |
| **Mocking** | Mockito | 5.20.0 |
| **Assertions** | AssertJ | 3.27.6 |
| **Code Coverage** | JaCoCo | 0.8.13 |
| **Packaging** | jpackage | JDK 21 |

## Architecture

### Multi-Module Maven Project

```text
SieveEditor/
├── pom.xml                 # Parent POM (version: 0.0.1-SNAPSHOT)
└── app/
    ├── pom.xml            # Application module
    └── src/
        ├── main/java/de/febrildur/sieveeditor/
        │   ├── Application.java          # Main entry point
        │   ├── PropertiesSieve.java      # Configuration management
        │   ├── ActionListenerNewProfile.java
        │   ├── ActionListenerEditProfile.java
        │   └── ...
        └── test/java/
            └── de/febrildur/sieveeditor/
                └── PropertiesSieveTest.java
```

### Key Components

1. **Application.java** (Main Entry Point)
   - Initializes Swing UI
   - Sets up main window and menu bar
   - Handles application lifecycle

2. **PropertiesSieve.java** (Configuration Manager)
   - Loads/saves server profiles from `~/.sieve` properties file
   - Encrypts/decrypts passwords using Jasypt
   - Manages profile persistence
   - **Security**: Never stores plaintext passwords

3. **Action Listeners** (UI Controllers)
   - `ActionListenerNewProfile` - Create new server profile
   - `ActionListenerEditProfile` - Edit existing profile
   - `ActionListenerConnect` - Establish ManageSieve connection
   - Handle user interactions and update UI

4. **ManageSieve Integration**
   - Uses ManageSieveJ library (lenucksi fork)
   - Supports STARTTLS and SSL/TLS connections
   - Implements RFC 5804 protocol operations
   - Handles authentication (PLAIN, LOGIN)

### Dependencies

#### Core Dependencies

- **RSyntaxTextArea** (`com.fifesoft:rsyntaxtextarea:3.6.0`)
  - Syntax highlighting for Sieve scripts
  - Code folding, line numbers, syntax validation

- **ManageSieveJ** (`com.github.lenucksi:ManageSieveJ:managesievej-v0.3.3`)
  - Java 21 compatible ManageSieve client
  - Distributed via JitPack
  - Source: <https://github.com/lenucksi/ManageSieveJ>

- **Jasypt** (`org.jasypt:jasypt:1.9.3`)
  - Password-based encryption for stored credentials
  - PBE (Password-Based Encryption) algorithms

- **Commons Codec** (`commons-codec:commons-codec:1.20.0`)
  - Base64 encoding/decoding utilities

#### Test Dependencies

- **JUnit Jupiter** (`org.junit.jupiter:junit-jupiter:6.0.1`)
- **Mockito Core** (`org.mockito:mockito-core:5.20.0`)
- **Mockito JUnit Jupiter** (`org.mockito:mockito-junit-jupiter:5.20.0`)
- **AssertJ** (`org.assertj:assertj-core:3.27.6`)

## Development Workflow

### 1. Environment Setup

**Prerequisites:**

- Java 21 LTS (Temurin, OpenJDK, or Oracle)
- Maven 3.6+
- Git

**Clone and Build:**

```bash
git clone https://github.com/lenucksi/SieveEditor.git
cd SieveEditor
git config core.hooksPath .githooks  # Enable conventional commit validation
mvn clean install
```

**Run Application:**

```bash
cd app
mvn package
java -jar target/SieveEditor-jar-with-dependencies.jar
```

### 2. Claude Code Integration

This project includes a `.claude/` harness for enhanced development:

**Slash Commands:**

- `/build` - Compile project
- `/test` - Run tests with coverage
- `/clean` - Remove build artifacts
- `/package` - Create executable JAR
- `/coverage` - Generate coverage report
- `/verify` - Run complete verification

**Session Hooks:**

- `SessionStart` - Auto-verifies environment on session start

**Skills:**

- `conventional-commits` - Interactive commit message builder

### 3. Conventional Commits (REQUIRED)

This project uses [Conventional Commits](https://www.conventionalcommits.org/) for automated changelog generation and semantic versioning via Release Please.

**Setup Git Hook:**

```bash
git config core.hooksPath .githooks
```

The commit-msg hook will validate all commit messages.

**Format:**

```text
<type>(<scope>): <subject>

<optional body>

<optional footer>
```

**Types:**

- `feat` - New feature (minor version bump)
- `fix` - Bug fix (patch version bump)
- `perf` - Performance improvement (patch)
- `security` - Security fix (patch)
- `deps` - Dependency update (patch)
- `docs` - Documentation (no version bump)
- `test` - Tests (no bump)
- `chore` - Maintenance (no bump)
- `refactor` - Code refactoring (no bump)
- `build` - Build system (no bump)
- `ci` - CI/CD (no bump)

**Examples:**

```bash
feat(profiles): add multi-profile support
fix(connection): prevent timeout on slow networks
security: remove hardcoded encryption key
deps: update rsyntaxtextarea to 3.6.0
docs: update installation instructions
```

**Breaking Changes:**
Use `!` after type/scope:

```bash
feat!: enforce SSL certificate validation

BREAKING CHANGE: Self-signed certificates are now rejected.
```

### 4. Testing Strategy

**Coverage Goals:**

- Overall: 70%+
- Critical components: 80%+
  - `PropertiesSieve` (configuration/security)
  - ManageSieve connection handlers
  - Action listeners

**Running Tests:**

```bash
cd app
mvn test                                          # Run all tests
mvn test -Dtest=PropertiesSieveTest              # Specific test class
mvn test -Dtest=PropertiesSieveTest#shouldEncryptPassword  # Specific test
mvn jacoco:report                                # Generate coverage report
```

**Coverage Report Location:**

```text
target/site/jacoco/index.html
```

**Test Naming Convention:**

```java
@Test
void shouldDoSomethingWhenCondition() {
    // AAA pattern: Arrange, Act, Assert
}
```

**Example Test:**

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

See `TEST-COVERAGE-ANALYSIS.md` for detailed testing guidelines.

### 5. Security Considerations

**Critical Security Areas:**

1. **Password Storage**
   - NEVER store plaintext passwords
   - Always use Jasypt encryption (PBE)
   - Encrypted format: `ENC(base64data)`
   - Master password derived from username+hostname

2. **SSL/TLS Validation**
   - Validate server certificates
   - Use STARTTLS or direct SSL
   - Reject invalid/expired certificates (future enhancement)

3. **Input Validation**
   - Validate all user inputs
   - Sanitize data before file operations
   - Check bounds on array access

4. **Secrets Management**
   - Never commit passwords, API keys, or credentials
   - Use environment variables for sensitive data
   - `.gitignore` excludes `.sieve` profile files

**Security Testing:**

```bash
mvn org.owasp:dependency-check-maven:check
```

See `SECURITY.md` for vulnerability reporting.

### 6. Build and Packaging

**JAR (All Platforms):**

```bash
cd app
mvn clean package
# Output: target/SieveEditor-jar-with-dependencies.jar
```

**Platform-Specific Packages:**

GitHub Actions automatically builds:

- **DEB** (Debian/Ubuntu) - Uses `jpackage --type deb`
- **RPM** (Fedora/RHEL) - Uses `jpackage --type rpm`
- **MSI** (Windows) - Uses WiX Toolset
- **DMG** (macOS) - Uses `jpackage --type dmg`
- **Flatpak** (Universal Linux) - Uses Flatpak manifest

See `.github/workflows/package.yml` for build details.

**Local Packaging Example (DEB):**

```bash
cd app
mvn package
jpackage --input target \
  --main-jar SieveEditor-jar-with-dependencies.jar \
  --main-class de.febrildur.sieveeditor.Application \
  --name SieveEditor \
  --app-version 0.0.1 \
  --type deb
```

## Release Process

This project uses **Release Please** for automated releases.

### How It Works

1. **Conventional Commits** are pushed to `main` branch
2. **Release Please** analyzes commits and determines version bump:
   - `feat` → minor version bump (0.X.0)
   - `fix`, `perf`, `security`, `deps` → patch bump (0.0.X)
   - `feat!` or `BREAKING CHANGE:` → major bump (X.0.0)
3. **Release PR** is created/updated automatically with CHANGELOG
4. **Merge PR** → Release is published, artifacts are built and uploaded

### Configuration Files

- `.github/release-please-config.json` - Release Please settings
- `.release-please-manifest.json` - Current version tracking (auto-generated)
- `CHANGELOG.md` - Auto-generated changelog

### Version Synchronization

Release Please updates:

- `pom.xml` (parent version)
- `app/pom.xml` (child inherits version)
- `CHANGELOG.md`
- Git tags

### First Release Preparation

Before the first release:

1. Ensure `.release-please-manifest.json` exists with starting version
2. Use conventional commits consistently
3. Verify tests pass
4. Review generated changelog in release PR

## CI/CD Pipeline

### Workflows

1. **CI** (`.github/workflows/ci.yml`)
   - Runs on PR and push
   - Multi-OS testing (Linux, Windows, macOS)
   - Java 21 and 22 compatibility
   - Code coverage via JaCoCo
   - SLSA provenance attestations

2. **Release** (`.github/workflows/release.yml`)
   - Triggered on push to `main`
   - Runs Release Please
   - Builds multi-platform packages
   - Uploads artifacts to GitHub Release
   - Generates SHA256 checksums

3. **Package** (`.github/workflows/package.yml`)
   - Builds JAR, DEB, RPM, MSI, DMG, Flatpak
   - Reusable workflow called by release workflow

4. **CodeQL** (`.github/workflows/codeql-adv.yml`)
   - Security scanning
   - Detects vulnerabilities

5. **Dependency Review** (`.github/workflows/dependency-review.yml`)
   - Checks for vulnerable dependencies

See `CI-CD-STRATEGY-2025.md` for complete strategy.

## Important Development Practices

### DO

✅ Use conventional commits for all changes
✅ Run tests before committing (`mvn test`)
✅ Check coverage regularly (`/coverage` or `mvn jacoco:report`)
✅ Validate inputs and handle edge cases
✅ Encrypt passwords using Jasypt
✅ Follow existing code style
✅ Add tests for new features
✅ Update documentation when changing behavior
✅ Use descriptive variable/method names
✅ Reference issues in commit footers (`Fixes #123`)

### DON'T

❌ Commit without running tests
❌ Store plaintext passwords or secrets
❌ Ignore security warnings
❌ Skip conventional commit format
❌ Push directly to `main` without PR (external contributors)
❌ Merge PRs without CI passing
❌ Hard-code sensitive data
❌ Use deprecated APIs without justification
❌ Leave `TODO` comments without issues
❌ Decrease test coverage

## Common Development Tasks

### Adding a New Feature

1. Create feature branch: `git checkout -b feature/my-feature`
2. Implement feature with tests
3. Run tests: `mvn test`
4. Check coverage: `mvn jacoco:report`
5. Commit with conventional format: `feat(scope): description`
6. Push and create PR
7. Ensure CI passes
8. Request review

### Fixing a Bug

1. Create bug fix branch: `git checkout -b fix/issue-123`
2. Write failing test that reproduces bug
3. Fix the bug
4. Verify test now passes
5. Run full test suite
6. Commit: `fix(scope): description` with `Fixes #123` footer
7. Push and create PR

### Updating Dependencies

1. Update version in `app/pom.xml`
2. Run tests to verify compatibility
3. Check for breaking changes in dependency changelog
4. Commit: `deps: update library-name to X.Y.Z`
5. If breaking changes: `deps!: update library-name to X.Y.Z`

### Improving Test Coverage

1. Run coverage report: `mvn jacoco:report`
2. Open `target/site/jacoco/index.html`
3. Identify classes with < 70% coverage
4. Add tests for uncovered code paths
5. Re-run coverage to verify improvement
6. Commit: `test: improve coverage for ComponentName`

## Troubleshooting

### Build Fails

```bash
# Clean and rebuild
mvn clean install

# Skip tests temporarily to check compilation
mvn clean compile -DskipTests
```

### Tests Fail

```bash
# Run specific test with verbose output
mvn test -Dtest=ClassName#testMethod -X

# Check for resource leaks
lsof -p $(pgrep java)
```

### Git Hook Rejection

If commit-msg hook rejects your commit:

1. Check commit message format
2. Ensure type is valid: `feat`, `fix`, `docs`, etc.
3. Use imperative mood: "add feature" not "added feature"
4. Keep subject <= 80 characters
5. Use skill: Invoke `conventional-commits` skill in Claude Code

**Bypass hook (not recommended):**

```bash
git commit --no-verify -m "message"
```

### JitPack Dependency Issues

If ManageSieveJ fails to resolve:

1. Check JitPack status: <https://jitpack.io/#lenucksi/ManageSieveJ>
2. Verify version tag exists: `managesievej-v0.3.3`
3. Clear local Maven cache:

   ```bash
   rm -rf ~/.m2/repository/com/github/lenucksi/ManageSieveJ
   mvn clean install
   ```

## Resources

### Documentation

- **Contributing Guide:** `CONTRIBUTING.md`
- **Test Strategy:** `TEST-COVERAGE-ANALYSIS.md`
- **CI/CD Strategy:** `CI-CD-STRATEGY-2025.md`
- **Security Policy:** `SECURITY.md`
- **Claude Harness:** `.claude/README.md`

### External Resources

- [Conventional Commits](https://www.conventionalcommits.org/)
- [ManageSieve RFC 5804](https://datatracker.ietf.org/doc/html/rfc5804)
- [Sieve RFC 5228](https://datatracker.ietf.org/doc/html/rfc5228)
- [RSyntaxTextArea Docs](https://github.com/bobbylight/RSyntaxTextArea)
- [Release Please](https://github.com/googleapis/release-please)

### Support

- [Discussions](https://github.com/lenucksi/SieveEditor/discussions)
- [Issues](https://github.com/lenucksi/SieveEditor/issues)
- [Pull Requests](https://github.com/lenucksi/SieveEditor/pulls)

## Project Goals

### Short Term

- ✅ Establish automated release process (Release Please)
- ⏳ Achieve 70% overall test coverage
- ⏳ Multi-platform packaging (JAR, DEB, RPM, MSI, DMG)
- ⏳ Security hardening (input validation, cert validation)

### Long Term

- 📋 Multi-profile management UI
- 📋 Improved syntax highlighting (context-aware)
- 📋 Script validation before upload
- 📋 Backup/restore profiles
- 📋 Dark mode support
- 📋 Internationalization (i18n)

See issue tracker for current priorities.

---

## Quick Reference

**Build:** `mvn clean package`
**Test:** `mvn test`
**Coverage:** `mvn jacoco:report`
**Run:** `java -jar target/SieveEditor-jar-with-dependencies.jar`
**Hook Setup:** `git config core.hooksPath .githooks`
**Commit Format:** `type(scope): subject`

**Need Help?** Check `CONTRIBUTING.md` or open a Discussion.

---

**Happy coding with Claude!** 🤖 🚀
