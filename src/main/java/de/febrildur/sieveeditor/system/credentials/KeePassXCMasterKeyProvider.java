package de.febrildur.sieveeditor.system.credentials;

import org.purejava.KeepassProxyAccess;
import org.purejava.KeepassProxyAccessException;

import javax.swing.*;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Master key provider that uses KeePassXC for secure storage.
 *
 * Stores the master encryption key as an entry in the user's KeePassXC database.
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

	private KeepassProxyAccess kpa;
	private String associationId;
	private String publicKey;

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

		// Try to get existing login
		Map<String, String> idKeyMap = Map.of("id", associationId, "key", publicKey);
		Map<String, Object> logins = kpa.getLogins(ENTRY_URL, null, true,
			java.util.List.of(idKeyMap));

		if (logins != null && !logins.isEmpty()) {
			// Extract password from first entry
			@SuppressWarnings("unchecked")
			var entries = (java.util.List<Map<String, Object>>) logins.get("entries");
			if (entries != null && !entries.isEmpty()) {
				Map<String, Object> firstEntry = entries.get(0);
				String password = (String) firstEntry.get("password");
				if (password != null && !password.isEmpty()) {
					LOGGER.log(Level.INFO, "Retrieved master key from KeePassXC");
					return password;
				}
			}
		}

		throw new CredentialException("Master key not found in KeePassXC. " +
			"Please create an entry with Title='" + ENTRY_TITLE + "' and URL='" + ENTRY_URL + "'");
	}

	@Override
	public void setMasterKey(String masterKey) throws CredentialException {
		ensureConnected();

		// Create or update the entry in KeePassXC
		boolean success = kpa.setLogin(
			ENTRY_URL,          // url
			null,               // submitUrl
			associationId,      // id
			ENTRY_USERNAME,     // login
			masterKey,          // password
			null,               // group (let user choose)
			null,               // groupUuid
			null                // uuid (create new if null)
		);

		if (!success) {
			throw new CredentialException("KeePassXC rejected the request to store master key");
		}

		LOGGER.log(Level.INFO, "Stored master key in KeePassXC");
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
			kpa.closeConnection();
		}
	}

	/**
	 * Ensures KeePassXC is connected and the database is unlocked.
	 * Shows dialogs to guide the user through starting and unlocking KeePassXC if needed.
	 *
	 * Proper workflow:
	 * 1. Connect to KeePassXC
	 * 2. Check if database is locked (BEFORE association)
	 * 3. If locked, prompt user to unlock
	 * 4. Associate with KeePassXC
	 *
	 * @throws CredentialException if connection fails or user cancels
	 */
	private void ensureConnected() throws CredentialException {
		if (kpa != null) {
			// Already connected
			return;
		}

		kpa = new KeepassProxyAccess();

		// Step 1: Connect to KeePassXC
		boolean connected = connectWithRetry();
		if (!connected) {
			throw new CredentialException("Failed to connect to KeePassXC");
		}

		// Step 2: Check if database is locked BEFORE trying to associate
		// Association will fail silently if database is locked
		if (kpa.isDatabaseLocked()) {
			LOGGER.log(Level.INFO, "KeePassXC database is locked, prompting user to unlock");

			// Show dialog to user
			JOptionPane.showMessageDialog(
				null,
				"Your KeePassXC database is locked.\n\n" +
					"Please unlock your database in KeePassXC, then click OK to continue.",
				"Unlock KeePassXC Database",
				JOptionPane.INFORMATION_MESSAGE
			);

			// Try to trigger unlock prompt in KeePassXC
			var hash = kpa.getDatabasehash(true);

			// Check if database is still locked
			if (!hash.isPresent() || kpa.isDatabaseLocked()) {
				throw new CredentialException("KeePassXC database is still locked. " +
					"Please unlock your database and try again.");
			}

			LOGGER.log(Level.INFO, "KeePassXC database unlocked successfully");
		}

		// Step 3: Associate with KeePassXC (now that database is unlocked)
		// Note: KeePassXC issue #7099 causes delayed association responses from Java apps
		// The library handles this by delaying the response lookup, but we need to retry
		associateWithRetry();

		LOGGER.log(Level.INFO, "Successfully connected and associated with KeePassXC");
	}

	/**
	 * Attempts to associate with KeePassXC, handling delayed responses.
	 *
	 * KeePassXC issue #7099: Association dialog responses are delayed when
	 * triggered from Java applications. The keepassxc-proxy-access library
	 * handles this by delaying the response lookup, but we need to retry
	 * if the first attempt returns false.
	 *
	 * @throws CredentialException if association fails after retries
	 */
	private void associateWithRetry() throws CredentialException {
		final int MAX_ATTEMPTS = 3;
		final int DELAY_MS = 2000; // 2 seconds between attempts

		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			LOGGER.log(Level.FINE, "Association attempt {0}/{1}", new Object[]{attempt, MAX_ATTEMPTS});

			boolean associated = kpa.associate();

			if (associated) {
				// Successfully associated - save credentials
				Map<String, String> connection = kpa.exportConnection();
				this.associationId = connection.get("id");
				this.publicKey = connection.get("key");

				LOGGER.log(Level.INFO, "Association successful on attempt {0}", attempt);
				return;
			}

			// Association returned false - might be due to delayed response
			LOGGER.log(Level.FINE, "Association returned false on attempt {0}, checking for credentials", attempt);

			// Try to get connection info anyway - might have succeeded despite false return
			try {
				Map<String, String> connection = kpa.exportConnection();
				String id = connection.get("id");
				String key = connection.get("key");

				if (id != null && !id.isEmpty() && key != null && !key.isEmpty()) {
					// We got credentials! Association actually succeeded
					this.associationId = id;
					this.publicKey = key;
					LOGGER.log(Level.INFO, "Association succeeded (exportConnection returned credentials despite false return)");
					return;
				}
			} catch (Exception e) {
				LOGGER.log(Level.FINE, "exportConnection failed: {0}", e.getMessage());
			}

			// If not last attempt, wait before retrying
			if (attempt < MAX_ATTEMPTS) {
				LOGGER.log(Level.INFO, "Waiting {0}ms before retry (KeePassXC issue #7099 - delayed response)", DELAY_MS);
				try {
					Thread.sleep(DELAY_MS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new CredentialException("Association interrupted", e);
				}
			}
		}

		// All attempts failed
		throw new CredentialException("Failed to associate with KeePassXC after " + MAX_ATTEMPTS + " attempts. " +
			"Please ensure you click 'Allow' when KeePassXC shows the association request dialog. " +
			"If you don't see a dialog, check if KeePassXC is running in the background.");
	}

	/**
	 * Attempts to connect to KeePassXC with user retry options.
	 *
	 * @return true if connected, false if user cancelled
	 * @throws CredentialException if user requests fallback or exits
	 */
	private boolean connectWithRetry() throws CredentialException {
		final int MAX_ATTEMPTS = 3;
		int attempts = 0;

		while (attempts < MAX_ATTEMPTS) {
			try {
				boolean connected = kpa.connect();
				if (connected) {
					return true;
				}

				// Show dialog asking user to start KeePassXC
				int result = JOptionPane.showOptionDialog(
					null,
					"KeePassXC is not running.\n\n" +
						"Please start KeePassXC, then click Retry.\n" +
						"If you prefer to use a different storage method, click 'Use Fallback'.",
					"KeePassXC Not Running",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE,
					null,
					new Object[]{"Retry", "Use Fallback", "Exit"},
					"Retry"
				);

				if (result == 0) { // Retry
					attempts++;
					Thread.sleep(500); // Give user time to start KeePassXC
					continue;
				} else if (result == 1) { // Use Fallback
					throw new CredentialException("User chose to use fallback storage method");
				} else { // Exit
					throw new CredentialException("User cancelled KeePassXC connection");
				}

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new CredentialException("Connection interrupted", e);
			}
		}

		return false; // Max attempts reached
	}
}
