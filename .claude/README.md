# Claude Code Harness for SieveEditor

This directory contains Claude Code configuration for enhanced development support in the SieveEditor project.

## Directory Structure

```text
.claude/
├── README.md              # This file
├── commands/              # Slash commands for common tasks
│   ├── build.md          # Build the project
│   ├── test.md           # Run tests
│   ├── clean.md          # Clean build artifacts
│   ├── package.md        # Package application
│   ├── coverage.md       # Generate coverage reports
│   └── verify.md         # Run full verification
├── hooks/                 # Session lifecycle hooks
│   └── SessionStart.md   # Auto-run on session start
└── skills/                # Reusable skills
    └── conventional-commits.md  # Conventional commits helper
```

## Quick Start

### Available Commands

Execute these commands in Claude Code chat:

- **`/build`** - Compile the project using Maven
- **`/test`** - Run JUnit test suite with coverage
- **`/clean`** - Remove all build artifacts
- **`/package`** - Create JAR distribution
- **`/coverage`** - Generate and display JaCoCo coverage report
- **`/verify`** - Run complete Maven verification (clean + test + package)

### Session Initialization

The `SessionStart` hook automatically:

- Verifies Java 21 and Maven installation
- Checks project structure (pom.xml)
- Displays available commands
- Configures git hooks for conventional commits

### Skills

- **`conventional-commits`** - Interactive helper for creating well-formed commit messages

## Project Information

**SieveEditor** is a Java 21 desktop application for managing ManageSieve server configurations.

### Tech Stack

- **Language:** Java 21 LTS
- **Build:** Maven 3.6+
- **Testing:** JUnit 5 + Mockito + AssertJ
- **Coverage:** JaCoCo
- **UI:** Swing with RSyntaxTextArea
- **Packaging:** jpackage (multi-platform)

### Build Targets

- JAR (cross-platform)
- DEB (Debian/Ubuntu)
- RPM (Fedora/RHEL)
- MSI (Windows)
- DMG (macOS)
- Flatpak (Universal Linux)

## Development Workflow

1. **Start Session** - Hooks auto-run, verify environment
2. **Build** - `/build` or `mvn clean compile`
3. **Test** - `/test` or `mvn test`
4. **Coverage** - `/coverage` to check test coverage
5. **Package** - `/package` to create JAR
6. **Commit** - Use conventional commits (enforced by git hook)
7. **Verify** - `/verify` before submitting PR

## Conventional Commits

This project uses [Conventional Commits](https://www.conventionalcommits.org/) for automated changelog generation and semantic versioning.

### Enforcement

Commit messages are validated by a git hook. To enable:

```bash
git config core.hooksPath .githooks
```

### Format

```text
<type>(<scope>): <subject>

<optional body>

<optional footer>
```

### Types

| Type | Description | Version Impact |
|------|-------------|----------------|
| `feat` | New feature | Minor (0.x.0) |
| `fix` | Bug fix | Patch (0.0.x) |
| `feat!` / `fix!` | Breaking change | Major (x.0.0) |
| `perf` | Performance | Patch |
| `security` | Security fix | Patch |
| `deps` | Dependencies | Patch |
| `docs` | Documentation | None |
| `test` | Tests | None |
| `chore` | Maintenance | None |
| `refactor` | Code refactoring | None |
| `build` | Build system | None |
| `ci` | CI/CD | None |

### Examples

```bash
feat(profiles): add multi-profile support
fix(connection): prevent timeout on slow networks
docs: update installation instructions
feat!: enforce SSL certificate validation
security: remove hardcoded encryption key
deps: update rsyntaxtextarea to 3.6.0
```

## Recommended Development Practices

1. **Run tests before commits** - Ensure `mvn test` passes
2. **Check coverage** - Aim for 70%+ overall, 80%+ on critical components
3. **Use conventional commits** - Required for automated releases
4. **Update tests** - Add tests for new features
5. **Verify locally** - Run `/verify` before pushing
6. **Follow security best practices** - Never commit secrets, validate inputs

## Resources

- **Contributing Guide:** `CONTRIBUTING.md`
- **Development Guide:** `CLAUDE.md`
- **Test Strategy:** `TEST-COVERAGE-ANALYSIS.md`
- **CI/CD Strategy:** `CI-CD-STRATEGY-2025.md`
- **API Docs:** Run `mvn javadoc:javadoc`

## Questions?

- Check [Discussions](https://github.com/lenucksi/SieveEditor/discussions)
- Review [Issues](https://github.com/lenucksi/SieveEditor/issues)
- Read existing documentation in `CONTRIBUTING.md`

---

**Happy Coding with Claude!** 🤖
