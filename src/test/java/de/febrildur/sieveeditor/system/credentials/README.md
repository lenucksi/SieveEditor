# Credential Storage Backend Tests

## Current Status (2025-12-02)

### Active Backends

- **Manual Password Entry** (`UserPromptMasterKeyProvider`)
  - Status: ACTIVE ✓
  - Tests: None (prompts user via GUI, not suitable for automated testing)

### Deactivated Backends

- **KeePassXC** (`KeePassXCMasterKeyProvider`)
  - Status: DEACTIVATED at factory level ❌
  - Reason: KeepassProxyAccess library has known issues with delayed association
    responses (KeePassXC issue #7099) - cannot be fixed from this project
  - Improvements made: Configurable retry/timeout, null safety, proper cleanup,
    reconnection support, better error messages
  - Tests: 19 tests covering identity, lifecycle, configuration, state
    management, input validation, and default constants
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

1. Fix the underlying KeepassProxyAccess library (or switch to a different
   KeePassXC integration approach) to resolve delayed association responses
   (KeePassXC issue #7099)
2. Uncomment deactivated code in `MasterKeyProviderFactory.java`:
   - `showBackendSelectionDialog()` method
   - `createProviderByName()` method
   - `createProviderByBackendArg()` method
3. Update this README and `dev-docs/CREDENTIAL-BACKENDS-STATUS.md`

## Testing Notes

### Challenges with Automated Testing

All credential backends are difficult to test automatically because:

- **KeePassXC**: Requires KeePassXC application running, database unlocked, browser integration enabled
- **OS Keychain**: Requires platform-specific credential manager (varies by OS)
- **Manual Entry**: Requires user interaction (GUI dialogs)

### Test Strategy

- KeePassXC tests use no mocking (KeepassProxyAccess is concrete). Tests
  focus on identity, lifecycle, state management, configuration, and
  input validation
- Tests avoid triggering GUI dialogs (ensureConnected path) by not calling
  getMasterKey()/setMasterKey() on unconnected providers in unit tests
- isAvailable() is tested as a side-effect-free boolean check
- MasterKeyProviderFactory tests use TestMasterKeyProvider for non-interactive
  testing of the factory logic
- Use `MasterKeyProviderFactory.setTestMode()` for testing applications that use the factory

## See Also

- `dev-docs/CREDENTIAL-BACKENDS-STATUS.md` - Detailed status and technical notes
- `MasterKeyProviderFactory.java` - Factory implementation with deactivation comments
