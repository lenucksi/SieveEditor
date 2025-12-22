# Credential Storage Backend Tests

## Current Status (2025-12-02)

### Active Backends

- **Manual Password Entry** (`UserPromptMasterKeyProvider`)
  - Status: ACTIVE ✓
  - Tests: None (prompts user via GUI, not suitable for automated testing)

### Deactivated Backends

- **KeePassXC** (`KeePassXCMasterKeyProvider`)
  - Status: DEACTIVATED ❌
  - Reason: Backend is currently broken and difficult to test
  - Tests: All tests disabled with `@Disabled` annotation
  - Test file: `KeePassXCMasterKeyProviderTest.java`

- **OS Keychain** (`OSKeychainMasterKeyProvider`)
  - Status: DEACTIVATED ❌
  - Reason: Backend is currently broken and difficult to test
  - Tests: None implemented

## Infrastructure

The credential storage infrastructure remains in place for all backends:

- `MasterKeyProvider.java` - Interface for all backends
- `MasterKeyProviderFactory.java` - Factory that currently only returns `UserPromptMasterKeyProvider`
- `KeePassXCMasterKeyProvider.java` - Implementation exists but deactivated
- `OSKeychainMasterKeyProvider.java` - Implementation exists but deactivated
- `UserPromptMasterKeyProvider.java` - Implementation active and used

## Re-enabling Backends

When backends are fixed in the future:

1. Update implementation classes to fix the issues
2. Uncomment deactivated code in `MasterKeyProviderFactory.java`:
   - `showBackendSelectionDialog()` method
   - `createProviderByName()` method
   - `createProviderByBackendArg()` method
3. Remove `@Disabled` annotation from test classes
4. Update this README and `dev-docs/CREDENTIAL-BACKENDS-STATUS.md`

## Testing Notes

### Challenges with Automated Testing

All credential backends are difficult to test automatically because:

- **KeePassXC**: Requires KeePassXC application running, database unlocked, browser integration enabled
- **OS Keychain**: Requires platform-specific credential manager (varies by OS)
- **Manual Entry**: Requires user interaction (GUI dialogs)

### Test Strategy

When backends are re-enabled, tests should:

- Focus on unit testing internal logic with mocks
- Use `@Disabled` for integration tests that require external systems
- Document expected behavior even if tests can't run in CI
- Consider using `MasterKeyProviderFactory.setTestMode()` for testing applications that use the factory

## See Also

- `dev-docs/CREDENTIAL-BACKENDS-STATUS.md` - Detailed status and technical notes
- `MasterKeyProviderFactory.java` - Factory implementation with deactivation comments
