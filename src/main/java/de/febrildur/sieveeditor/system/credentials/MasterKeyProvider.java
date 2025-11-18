package de.febrildur.sieveeditor.system.credentials;

/**
 * Interface for different master key storage backends.
 * The master key is used to encrypt/decrypt server passwords stored in profiles.
 *
 * Implementations:
 * - KeePassXCMasterKeyProvider: Stores master key in KeePassXC database
 * - OSKeychainMasterKeyProvider: Uses OS credential manager (Windows Credential Manager, macOS Keychain, Linux Secret Service)
 * - UserPromptMasterKeyProvider: Prompts user for master password on each startup
 */
public interface MasterKeyProvider {

	/**
	 * Checks if this provider is available on the current system.
	 * For example, KeePassXC provider would check if KeePassXC is running.
	 *
	 * @return true if the provider can be used
	 */
	boolean isAvailable();

	/**
	 * Gets the master key from the credential store.
	 *
	 * @return the master key string
	 * @throws CredentialException if the key cannot be retrieved
	 */
	String getMasterKey() throws CredentialException;

	/**
	 * Stores the master key in the credential store.
	 *
	 * @param masterKey the master key to store
	 * @throws CredentialException if the key cannot be stored
	 */
	void setMasterKey(String masterKey) throws CredentialException;

	/**
	 * Gets a human-readable name for this provider.
	 *
	 * @return provider name (e.g., "KeePassXC", "System Keychain", "User Prompt")
	 */
	String getName();

	/**
	 * Gets a description of this provider for display to the user.
	 *
	 * @return provider description
	 */
	String getDescription();

	/**
	 * Closes any resources used by this provider.
	 */
	default void close() {
		// Default: no cleanup needed
	}
}
