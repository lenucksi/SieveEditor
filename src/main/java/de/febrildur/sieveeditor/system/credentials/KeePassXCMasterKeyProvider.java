package de.febrildur.sieveeditor.system.credentials;
// SPDX-FileCopyrightText: 2025 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.purejava.KeepassProxyAccess;
import javax.swing.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Master key provider that uses KeePassXC for secure storage.
 *
 * Stores the master encryption key as an entry in the user's KeePassXC
 * database.
 * This is the most secure option as it leverages KeePassXC's strong encryption
 * and allows the master key to sync across devices if the database is synced.
 *
 * Requirements:
 * - KeePassXC 2.6.0+ must be installed and running
 * - Database must be unlocked
 * - Browser integration must be enabled in KeePassXC settings
 */
public class KeePassXCMasterKeyProvider implements MasterKeyProvider {

	private static final Logger LOGGER = Logger.getLogger(KeePassXCMasterKeyProvider.class.getName());
	private static final String ENTRY_TITLE = "SieveEditor Master Key";
	private static final String ENTRY_URL = "sieveeditor://master-key";
	private static final String ENTRY_USERNAME = "sieveeditor";

	static final int DEFAULT_MAX_ASSOCIATION_ATTEMPTS = 3;
	static final int DEFAULT_ASSOCIATION_DELAY_MS = 2000;
	static final int DEFAULT_MAX_CONNECT_ATTEMPTS = 3;
	static final int DEFAULT_CONNECT_RETRY_DELAY_MS = 500;
	static final int DEFAULT_RECONNECT_MAX_RETRIES = 2;

	private final int maxAssociationAttempts;
	private final int associationDelayMs;
	private final int maxConnectAttempts;
	private final int connectRetryDelayMs;
	private final int reconnectMaxRetries;

	private KeepassProxyAccess kpa;
	private String associationId;
	private String publicKey;
	private boolean freshlyAssociated = false;

	public KeePassXCMasterKeyProvider() {
		this(DEFAULT_MAX_ASSOCIATION_ATTEMPTS, DEFAULT_ASSOCIATION_DELAY_MS,
				DEFAULT_MAX_CONNECT_ATTEMPTS, DEFAULT_CONNECT_RETRY_DELAY_MS,
				DEFAULT_RECONNECT_MAX_RETRIES);
	}

	KeePassXCMasterKeyProvider(int maxAssociationAttempts, int associationDelayMs,
			int maxConnectAttempts, int connectRetryDelayMs,
			int reconnectMaxRetries) {
		this.maxAssociationAttempts = maxAssociationAttempts;
		this.associationDelayMs = associationDelayMs;
		this.maxConnectAttempts = maxConnectAttempts;
		this.connectRetryDelayMs = connectRetryDelayMs;
		this.reconnectMaxRetries = reconnectMaxRetries;
	}

	int getMaxAssociationAttempts() {
		return maxAssociationAttempts;
	}

	int getAssociationDelayMs() {
		return associationDelayMs;
	}

	int getMaxConnectAttempts() {
		return maxConnectAttempts;
	}

	int getConnectRetryDelayMs() {
		return connectRetryDelayMs;
	}

	int getReconnectMaxRetries() {
		return reconnectMaxRetries;
	}

	@Override
	public boolean isAvailable() {
		try {
			KeepassProxyAccess testKpa = new KeepassProxyAccess();
			boolean connected = testKpa.connect();
			testKpa.closeConnection();
			return connected;
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "KeePassXC not available", e);
			return false;
		}
	}

	@Override
	public String getMasterKey() throws CredentialException {
		ensureConnected();

		if (freshlyAssociated) {
			return null;
		}

		try {
			Map<String, String> idKeyMap = Map.of("id", associationId, "key", publicKey);
			Map<String, Object> logins = kpa.getLogins(ENTRY_URL, null, true,
					java.util.List.of(idKeyMap));

			if (logins != null && !logins.isEmpty()) {
				@SuppressWarnings("unchecked")
				var entries = (java.util.List<Map<String, Object>>) logins.get("entries");
				if (entries != null && !entries.isEmpty()) {
					Map<String, Object> firstEntry = entries.get(0);
					if (firstEntry != null) {
						String password = (String) firstEntry.get("password");
						if (password != null && !password.isEmpty()) {
							return password;
						}
					}
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING,
					"Exception while retrieving master key: " + e.getClass().getName() + ": " + e.getMessage());
		}

		return null;
	}

	@Override
	public void setMasterKey(String masterKey) throws CredentialException {
		if (masterKey == null) {
			throw new CredentialException("Cannot store null master key in KeePassXC");
		}
		ensureConnected();

		boolean success = kpa.setLogin(
				ENTRY_URL,
				null,
				associationId,
				ENTRY_USERNAME,
				masterKey,
				null,
				null,
				null
		);

		if (!success) {
			throw new CredentialException("KeePassXC rejected the request to store master key");
		}

		freshlyAssociated = false;
	}

	@Override
	public String getName() {
		return "KeePassXC";
	}

	@Override
	public String getDescription() {
		return "Store master key in your KeePassXC database (most secure, syncs across devices)";
	}

	@Override
	public void close() {
		if (kpa != null) {
			try {
				kpa.closeConnection();
			} catch (Exception e) {
				LOGGER.log(Level.FINE, "Exception while closing KeePassXC connection", e);
			}
			kpa = null;
		}
		associationId = null;
		publicKey = null;
		freshlyAssociated = false;
	}

	boolean isConnected() {
		return kpa != null;
	}

	void resetConnection() {
		if (kpa != null) {
			try {
				kpa.closeConnection();
			} catch (Exception e) {
				LOGGER.log(Level.FINE, "Exception while resetting KeePassXC connection", e);
			}
			kpa = null;
		}
		associationId = null;
		publicKey = null;
		freshlyAssociated = false;
	}

	/**
	 * Retrieves association credentials that were automatically loaded by the
	 * library.
	 * The KeepassProxyAccess library automatically persists credentials to disk
	 * after
	 * successful association and loads them on construction.
	 *
	 * @return true if valid credentials exist, false otherwise
	 */
	private boolean hasValidSavedCredentials() {
		try {
			String id = kpa.getAssociateId();
			String key = kpa.getIdKeyPairPublicKey();

			if (id != null && !id.isEmpty() && key != null && !key.isEmpty()) {
				this.associationId = id;
				this.publicKey = key;
				LOGGER.log(Level.INFO, "Found credentials auto-loaded by library");
				return true;
			}
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "No saved credentials available: {0}", e.getMessage());
		}

		return false;
	}

	private void ensureConnected() throws CredentialException {
		ensureConnected(0);
	}

	private void ensureConnected(int retryCount) throws CredentialException {
		if (kpa != null) {
			return;
		}

		kpa = new KeepassProxyAccess();

		boolean connected = connectWithRetry();
		if (!connected) {
			resetConnection();
			throw new CredentialException("Failed to connect to KeePassXC after " + maxConnectAttempts
					+ " attempts. Ensure KeePassXC is running and browser integration is enabled.");
		}

		if (kpa.isDatabaseLocked()) {
			JOptionPane.showMessageDialog(
					null,
					"Your KeePassXC database is locked.\n\n"
							+ "Please unlock your database in KeePassXC, then click OK to continue.",
					"Unlock KeePassXC Database",
					JOptionPane.INFORMATION_MESSAGE);

			var hash = kpa.getDatabasehash(true);

			if (!hash.isPresent() || kpa.isDatabaseLocked()) {
				resetConnection();
				throw new CredentialException("KeePassXC database is still locked. "
						+ "Please unlock your database and try again.");
			}
		}

		if (hasValidSavedCredentials()) {
			try {
				boolean valid = kpa.testAssociate(associationId, publicKey);

				if (valid) {
					freshlyAssociated = false;
					return;
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING,
						"Failed to test saved credentials: " + e.getMessage() + ", will re-associate");
			}
		}

		associateWithRetry();
		freshlyAssociated = true;
	}

	private void tryReconnect() throws CredentialException {
		resetConnection();
		for (int i = 0; i < reconnectMaxRetries; i++) {
			try {
				ensureConnected(i);
				return;
			} catch (CredentialException e) {
				if (i < reconnectMaxRetries - 1) {
					try {
						Thread.sleep(connectRetryDelayMs);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new CredentialException("Reconnection interrupted", ie);
					}
				}
			}
		}
		throw new CredentialException("Failed to reconnect to KeePassXC after " + reconnectMaxRetries + " attempts");
	}

	private void associateWithRetry() throws CredentialException {
		for (int attempt = 1; attempt <= maxAssociationAttempts; attempt++) {
			LOGGER.log(Level.FINE, "Association attempt {0}/{1}", new Object[] { attempt, maxAssociationAttempts });

			boolean associated = kpa.associate();

			if (associated) {
				Map<String, String> connection = kpa.exportConnection();
				this.associationId = connection.get("id");
				this.publicKey = connection.get("key");
				return;
			}

			try {
				Map<String, String> connection = kpa.exportConnection();
				String id = connection.get("id");
				String key = connection.get("key");

				if (id != null && !id.isEmpty() && key != null && !key.isEmpty()) {
					this.associationId = id;
					this.publicKey = key;
					return;
				}
			} catch (Exception e) {
				LOGGER.log(Level.FINE, "exportConnection failed: {0}", e.getMessage());
			}

			if (attempt < maxAssociationAttempts) {
				try {
					Thread.sleep(associationDelayMs);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					resetConnection();
					throw new CredentialException("Association interrupted", e);
				}
			}
		}

		resetConnection();
		throw new CredentialException("Failed to associate with KeePassXC after " + maxAssociationAttempts
				+ " attempts. Please ensure you click 'Allow' when KeePassXC shows the association request dialog. "
				+ "If you don't see a dialog, check if KeePassXC is running in the background.");
	}

	private boolean connectWithRetry() throws CredentialException {
		int attempts = 0;

		while (attempts < maxConnectAttempts) {
			try {
				boolean connected = kpa.connect();
				if (connected) {
					return true;
				}

				int result = JOptionPane.showOptionDialog(
						null,
						"KeePassXC is not running.\n\n"
								+ "Please start KeePassXC, then click Retry.\n"
								+ "If you prefer to use a different storage method, click 'Use Fallback'.",
						"KeePassXC Not Running",
						JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE,
						null,
						new Object[] { "Retry", "Use Fallback", "Exit" },
						"Retry");

				if (result == 0) {
					attempts++;
					Thread.sleep(connectRetryDelayMs);
					continue;
				} else if (result == 1) {
					resetConnection();
					throw new CredentialException("User chose to use fallback storage method");
				} else {
					resetConnection();
					throw new CredentialException("User cancelled KeePassXC connection");
				}

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				resetConnection();
				throw new CredentialException("Connection interrupted", e);
			}
		}

		return false;
	}
}
