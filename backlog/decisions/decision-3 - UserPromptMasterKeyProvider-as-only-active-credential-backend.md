---
id: decision-3
title: UserPromptMasterKeyProvider as only active credential backend
date: '2026-05-20 14:58'
status: accepted
---
## Context

Three credential backends existed: UserPromptMasterKeyProvider, KeePassXCMasterKeyProvider, OSKeychainMasterKeyProvider. KeePassXC and OS Keychain were broken with no clear path to fix.

## Decision

UserPromptMasterKeyProvider is the only active backend. KeePassXC and OS Keychain are @Disabled and deactivated. Future effort may revive them, but not currently prioritized.

## Consequences

Working credential storage via GUI prompt. Users can't use KeePassXC integration or OS keychain. All credential tests @Disabled for inactive backends.

