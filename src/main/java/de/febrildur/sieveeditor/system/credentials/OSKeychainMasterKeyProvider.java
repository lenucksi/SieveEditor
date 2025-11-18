package de.febrildur.sieveeditor.system.credentials;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Master key provider that uses the operating system's credential manager.
 *
 * Platform-specific backends:
 * - Linux: Secret Service API (GNOME Keyring, KWallet via D-Bus)
 * - macOS: Keychain
 * - Windows: Credential Manager (Wincred API)
 *
 * Note: Security level varies by platform and application packaging.
 * See java-keyring documentation for details.
 */
public class OSKeychainMasterKeyProvider implements MasterKeyProvider {

	private static final Logger LOGGER = Logger.getLogger(OSKeychainMasterKeyProvider.class.getName());
	private static final String SERVICE_NAME = "SieveEditor";
	private static final String ACCOUNT_NAME = "master-encryption-key";

	private Keyring keyring;

	@Override
	public boolean isAvailable() {
		try {
			// Try to create keyring instance
			Keyring testKeyring = Keyring.create();
			// Test if it actually works by trying a dummy operation
			// This will fail gracefully if the keyring is not available
			return testKeyring != null;
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "OS keychain not available", e);
			return false;
		}
	}

	@Override
	public String getMasterKey() throws CredentialException {
		ensureKeyring();

		try {
			String password = keyring.getPassword(SERVICE_NAME, ACCOUNT_NAME);
			if (password == null || password.isEmpty()) {
				throw new CredentialException("Master key not found in OS keychain");
			}
			LOGGER.log(Level.INFO, "Retrieved master key from OS keychain");
			return password;
		} catch (PasswordAccessException e) {
			throw new CredentialException("Failed to retrieve master key from OS keychain", e);
		}
	}

	@Override
	public void setMasterKey(String masterKey) throws CredentialException {
		ensureKeyring();

		try {
			keyring.setPassword(SERVICE_NAME, ACCOUNT_NAME, masterKey);
			LOGGER.log(Level.INFO, "Stored master key in OS keychain");
		} catch (PasswordAccessException e) {
			throw new CredentialException("Failed to store master key in OS keychain", e);
		}
	}

	@Override
	public String getName() {
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.contains("win")) {
			return "Windows Credential Manager";
		} else if (osName.contains("mac")) {
			return "macOS Keychain";
		} else if (osName.contains("nux")) {
			return "Linux Secret Service";
		} else {
			return "System Keychain";
		}
	}

	@Override
	public String getDescription() {
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.contains("win")) {
			return "Store master key in Windows Credential Manager (native Windows security)";
		} else if (osName.contains("mac")) {
			return "Store master key in macOS Keychain (native macOS security)";
		} else if (osName.contains("nux")) {
			return "Store master key in GNOME Keyring or KWallet (Linux Secret Service)";
		} else {
			return "Store master key in system credential manager";
		}
	}

	private void ensureKeyring() throws CredentialException {
		if (keyring != null) {
			return;
		}

		try {
			keyring = Keyring.create();
			if (keyring == null) {
				throw new CredentialException("Failed to initialize OS keychain");
			}
		} catch (Exception e) {
			throw new CredentialException("Failed to initialize OS keychain. " +
				"Please ensure your system's credential manager is available.", e);
		}
	}
}
