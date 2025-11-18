package de.febrildur.sieveeditor.system.credentials;

import org.purejava.KeepassProxyAccess;
import org.purejava.KeepassProxyAccessException;

import javax.swing.*;
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
	private boolean freshlyAssociated = false; // Track if we just did a new association

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
		LOGGER.log(Level.INFO, "getMasterKey() called");
		ensureConnected();

		// If we just did a fresh association, the entry doesn't exist yet - skip getLogins
		if (freshlyAssociated) {
			LOGGER.log(Level.INFO, "Freshly associated - no entry exists yet, returning null");
			return null;
		}

		LOGGER.log(Level.INFO, "Attempting to retrieve master key from KeePassXC...");
		try {
			// Try to get existing login
			Map<String, String> idKeyMap = Map.of("id", associationId, "key", publicKey);
			Map<String, Object> logins = kpa.getLogins(ENTRY_URL, null, true,
				java.util.List.of(idKeyMap));

			LOGGER.log(Level.INFO, "getLogins() returned: " + (logins != null ? "data" : "null"));

			if (logins != null && !logins.isEmpty()) {
				// Extract password from first entry
				@SuppressWarnings("unchecked")
				var entries = (java.util.List<Map<String, Object>>) logins.get("entries");
				LOGGER.log(Level.INFO, "Entries: " + (entries != null ? entries.size() + " entries" : "null"));
				if (entries != null && !entries.isEmpty()) {
					Map<String, Object> firstEntry = entries.get(0);
					String password = (String) firstEntry.get("password");
					if (password != null && !password.isEmpty()) {
						LOGGER.log(Level.INFO, "Retrieved master key from KeePassXC");
						return password;
					}
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Exception while retrieving master key: " + e.getClass().getName() + ": " + e.getMessage());
			// Entry probably doesn't exist - this is normal on first run
		}

		// Entry doesn't exist yet (first run) - return null so caller can generate and store one
		LOGGER.log(Level.INFO, "Master key entry not found in KeePassXC (first run), will generate new key");
		return null;
	}

	@Override
	public void setMasterKey(String masterKey) throws CredentialException {
		LOGGER.log(Level.INFO, "setMasterKey() called");
		ensureConnected();

		LOGGER.log(Level.INFO, "Attempting to store master key in KeePassXC...");
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

		LOGGER.log(Level.INFO, "setLogin() returned: " + success);

		if (!success) {
			throw new CredentialException("KeePassXC rejected the request to store master key");
		}

		// Entry now exists - clear freshlyAssociated flag
		freshlyAssociated = false;

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
	 * Retrieves association credentials that were automatically loaded by the library.
	 * The KeepassProxyAccess library automatically persists credentials to disk after
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

	/**
	 * Ensures KeePassXC is connected and the database is unlocked.
	 * Shows dialogs to guide the user through starting and unlocking KeePassXC if needed.
	 *
	 * Proper workflow:
	 * 1. Create KeepassProxyAccess (auto-loads saved credentials from disk)
	 * 2. Connect to KeePassXC
	 * 3. Check if database is locked (BEFORE association)
	 * 4. If locked, prompt user to unlock
	 * 5. Check if saved credentials exist and are valid
	 * 6. If invalid or missing, perform new association (auto-saves credentials)
	 *
	 * @throws CredentialException if connection fails or user cancels
	 */
	private void ensureConnected() throws CredentialException {
		if (kpa != null) {
			// Already connected
			return;
		}

		// Create KeepassProxyAccess instance - this automatically loads saved credentials
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

		// Step 3: Check if we have saved credentials (auto-loaded by library)
		if (hasValidSavedCredentials()) {
			// We have saved credentials - test if they're still valid
			LOGGER.log(Level.INFO, "Testing saved association credentials...");

			try {
				boolean valid = kpa.testAssociate(associationId, publicKey);

				if (valid) {
					LOGGER.log(Level.INFO, "Existing association is valid, no re-association needed");
					freshlyAssociated = false; // Existing association, entries may exist
					return;
				} else {
					LOGGER.log(Level.WARNING, "Saved association credentials are no longer valid, will re-associate");
					// Fall through to associate
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to test saved credentials: " + e.getMessage() + ", will re-associate");
				// Fall through to associate
			}
		}

		// Step 4: Associate with KeePassXC (either first time or credentials invalid)
		// Note: KeePassXC issue #7099 causes delayed association responses from Java apps
		// The library handles this by delaying the response lookup, but we need to retry
		associateWithRetry();
		// The library automatically saves credentials after successful association

		// Mark that we just did a fresh association - no entries exist yet
		freshlyAssociated = true;

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
