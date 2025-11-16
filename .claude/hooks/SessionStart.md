---
description: Auto-run environment verification and setup on session start
---

Welcome to SieveEditor development! 🚀

Verify the development environment and configure git hooks:

1. **Check Java version** - Run `java -version` (need Java 21+)
2. **Check Maven version** - Run `mvn -version` (need Maven 3.6+)
3. **Verify project structure** - Check that `pom.xml` and `app/pom.xml` exist
4. **Configure git hooks** - Run `git config core.hooksPath .githooks` to enable conventional commit validation
5. **Display project info**:
   - Project: SieveEditor (ManageSieve editor)
   - Java: 21 LTS
   - Build: Maven multi-module
   - Tests: JUnit 5 + Mockito
   - Current version: Check `pom.xml`

6. **List available commands**:
   - `/build` - Compile project
   - `/test` - Run tests
   - `/clean` - Clean artifacts
   - `/package` - Create JAR
   - `/coverage` - Coverage report
   - `/verify` - Full verification

7. **Remind about conventional commits**:
   - This project requires Conventional Commits
   - Git hook is now active (if configured above)
   - Use `feat:`, `fix:`, `docs:`, etc.
   - See `CONTRIBUTING.md` for examples

If any checks fail, provide troubleshooting guidance.

After setup, ask the developer what they'd like to work on today.
