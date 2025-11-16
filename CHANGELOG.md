# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Conventional Commits enforcement via git commit-msg hook
- Claude Code development harness with slash commands and skills
- Comprehensive developer documentation (CLAUDE.md)
- Multi-platform packaging support (JAR, DEB, RPM, MSI, DMG, Flatpak)
- Automated release workflow with Release Please
- SLSA Level 3 provenance attestations for all artifacts

### Changed
- Updated CONTRIBUTING.md with git hooks setup instructions
- Enhanced commit message validation and enforcement

## [0.0.1] - Initial Development

### Added
- Initial project structure
- ManageSieve client integration (ManageSieveJ 0.3.3)
- Swing-based GUI with RSyntaxTextArea for Sieve script editing
- Jasypt-based password encryption for profile storage
- JUnit 5 test framework with Mockito and AssertJ
- JaCoCo code coverage reporting
- Multi-module Maven build system

[Unreleased]: https://github.com/lenucksi/SieveEditor/compare/v0.0.1...HEAD
[0.0.1]: https://github.com/lenucksi/SieveEditor/releases/tag/v0.0.1
