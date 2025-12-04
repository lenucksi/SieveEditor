# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0](https://github.com/lenucksi/SieveEditor/compare/0.9.2...v1.0.0) (2025-12-04)


### ⚠ BREAKING CHANGES

* Existing encrypted passwords cannot be decrypted with new key. Users must re-enter passwords after upgrade.

### security

* replace hardcoded encryption key with locally entered secret.


### Features

* **file:** add local file load/save functionality ([6da6cf8](https://github.com/lenucksi/SieveEditor/commit/6da6cf8a45a35bd10e90397a493affa781409827))
* **flatpak:** add application screenshots to metainfo ([11a14ed](https://github.com/lenucksi/SieveEditor/commit/11a14ed937f231c743b3e8ac4712267b09e5e614))
* **flatpak:** add D-Bus Secret Service permission and XDG paths ([09a1746](https://github.com/lenucksi/SieveEditor/commit/09a1746ff271d1a3e9abb569c49b0b5fc1cefa76))
* **launcher:** add auto-build and improve sieveeditor.sh ([b6bb0eb](https://github.com/lenucksi/SieveEditor/commit/b6bb0eb31391902356f0b8f46b6ac0f1add9a125))
* **profiles:** add profile deletion functionality ([ea12a69](https://github.com/lenucksi/SieveEditor/commit/ea12a697b6a09109ec0e60c829c19c36186f0863))
* **security:** add interactive certificate trust dialog with fingerprint verification ([850e257](https://github.com/lenucksi/SieveEditor/commit/850e25719fb0def21d939523716b35edc3fc837c))
* **templates:** add template insertion for common Sieve patterns ([66754cd](https://github.com/lenucksi/SieveEditor/commit/66754cdbb3b9d15b6ae1bf8ac1b7df4da29a4b1a))
* **ui:** add full CRUD operations to Manage Scripts dialog ([7f76289](https://github.com/lenucksi/SieveEditor/commit/7f762893cf63973fafe944c71ddbb228e52ca825))
* **ui:** add Linux desktop integration and remove obsolete 4K workarounds ([e710bbe](https://github.com/lenucksi/SieveEditor/commit/e710bbe7d0f4cb05853591c37dad7dc064c55520))
* **ui:** integrate FlatLaf for modern look-and-feel and HiDPI support ([943dad4](https://github.com/lenucksi/SieveEditor/commit/943dad43ba9818bb04917df5519c4fd6e5a69e60))

### Dependencies

* **deps:** bump com.formdev:flatlaf from 3.5.4 to 3.6.2 ([2851e52](https://github.com/lenucksi/SieveEditor/commit/2851e525850bd20a7e81e3332017ea4ed7362c09))

## [Unreleased]

### Added

- Multi-platform packaging support (JAR, DEB, RPM, MSI, DMG, Flatpak)
- Automated release workflow with Release Please
- SLSA Level 3 provenance attestations for all artifacts

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
