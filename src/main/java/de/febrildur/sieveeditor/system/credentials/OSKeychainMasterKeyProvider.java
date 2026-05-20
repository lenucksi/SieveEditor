package de.febrildur.sieveeditor.system.credentials;
// SPDX-FileCopyrightText: 2025 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class OSKeychainMasterKeyProvider implements MasterKeyProvider {

	private static final Logger LOGGER = Logger.getLogger(OSKeychainMasterKeyProvider.class.getName());
	private static final String SERVICE_NAME = "SieveEditor";
	private static final String ACCOUNT_NAME = "master-encryption-key";

	private Keyring keyring;

	@Override
	public boolean isAvailable() {
		try {
			Keyring testKeyring = createKeyring();
			return testKeyring != null;
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "OS keychain not available: {0}", e.getMessage());
			return false;
		}
	}

	@Override
	public String getMasterKey() throws CredentialException {
		ensureKeyring();

		try {
			String password = keyring.getPassword(SERVICE_NAME, ACCOUNT_NAME);
			if (password != null && !password.isEmpty()) {
				LOGGER.log(Level.INFO, "Retrieved master key from OS keychain");
				return password;
			}
		} catch (PasswordAccessException e) {
			if (e.getMessage() != null && e.getMessage().contains("No stored credentials match")) {
				LOGGER.log(Level.INFO, "Master key not found in OS keychain (first run), will generate new key");
				return null;
			}
			throw new CredentialException("Failed to retrieve master key from OS keychain", e);
		}

		LOGGER.log(Level.INFO, "Master key not found in OS keychain (first run), will generate new key");
		return null;
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
		return detectOSName();
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

	@Override
	public void close() {
		if (keyring != null) {
			try {
				keyring.close();
			} catch (Exception e) {
				LOGGER.log(Level.FINE, "Error closing OS keychain", e);
			}
			keyring = null;
		}
	}

	Keyring createKeyring() throws BackendNotSupportedException {
		return Keyring.create();
	}

	private void ensureKeyring() throws CredentialException {
		if (keyring != null) {
			return;
		}

		try {
			keyring = createKeyring();
			if (keyring == null) {
				throw new CredentialException("Failed to initialize OS keychain. " + getPlatformGuidance());
			}
		} catch (BackendNotSupportedException e) {
			throw new CredentialException("Failed to initialize OS keychain. " + getPlatformGuidance(), e);
		} catch (Exception e) {
			throw new CredentialException("Failed to initialize OS keychain. " + getPlatformGuidance(), e);
		}
	}

	private static String getPlatformGuidance() {
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.contains("win")) {
			return "Please ensure Windows Credential Manager service is running.";
		} else if (osName.contains("mac")) {
			return "Please ensure macOS Keychain is accessible (try 'security unlock-keychain' in Terminal).";
		} else if (osName.contains("nux")) {
			return "Please ensure a Secret Service provider is running (e.g., gnome-keyring, kwallet, or keepassxc).";
		} else {
			return "Please ensure your system's credential manager is available.";
		}
	}

	private static String detectOSName() {
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
}
