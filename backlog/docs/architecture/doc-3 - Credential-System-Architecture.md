---
id: doc-3
title: Credential System Architecture
type: guide
created_date: '2026-05-20 14:58'
updated_date: '2026-05-20 14:59'
---
# Credential System Architecture

## Backends

1. **UserPromptMasterKeyProvider** — ACTIVE, prompts user via GUI for master password
2. **KeePassXCMasterKeyProvider** — DEACTIVATED (broken, tests @Disabled)
3. **OSKeychainMasterKeyProvider** — DEACTIVATED (broken, tests @Disabled)

## Design
- Encryption using Jasypt (password-based encryption)
- Commons Codec for Base64
- keepassxc-proxy-access for KeePassXC integration
- java-keyring for OS keychain

## Known Issues
- KeePassXC and OS Keychain backends need investigation
- Credential exposure in logs possible during debugging
- No password strength enforcement

## References
- dev-docs/CREDENTIAL-STORAGE-REFACTOR.md
- dev-docs/CREDENTIAL-BACKENDS-STATUS.md
- src/main/java/.../credentials/ (source implementation)
