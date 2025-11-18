package de.febrildur.sieveeditor.system.credentials;

import javax.swing.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Master key provider that prompts the user for a password on each startup.
 *
 * This is the fallback option when no secure storage is available.
 * The password is kept in memory for the application session only.
 *
 * Security notes:
 * - Password is not stored persistently
 * - User must enter password each time application starts
 * - Password is hashed with SHA-256 to create the master key
 */
public class UserPromptMasterKeyProvider implements MasterKeyProvider {

	private static final Logger LOGGER = Logger.getLogger(UserPromptMasterKeyProvider.class.getName());
	private String cachedMasterKey;

	@Override
	public boolean isAvailable() {
		// Always available as the ultimate fallback
		return true;
	}

	@Override
	public String getMasterKey() throws CredentialException {
		if (cachedMasterKey != null) {
			return cachedMasterKey;
		}

		// Prompt user for password
		JPasswordField passwordField = new JPasswordField(20);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(new JLabel("Enter your master password:"));
		panel.add(new JLabel(" "));
		panel.add(new JLabel("This password encrypts your stored server passwords."));
		panel.add(new JLabel("You must remember this password - it cannot be recovered!"));
		panel.add(new JLabel(" "));
		panel.add(passwordField);

		int result = JOptionPane.showConfirmDialog(
			null,
			panel,
			"SieveEditor Master Password",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE
		);

		if (result != JOptionPane.OK_OPTION) {
			throw new CredentialException("User cancelled password prompt");
		}

		char[] password = passwordField.getPassword();
		if (password == null || password.length == 0) {
			throw new CredentialException("Password cannot be empty");
		}

		try {
			// Hash the password to create a consistent-length master key
			String passwordStr = new String(password);
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(passwordStr.getBytes());
			cachedMasterKey = Base64.getEncoder().encodeToString(hash);

			// Clear password from memory
			java.util.Arrays.fill(password, '\0');

			LOGGER.log(Level.INFO, "Master key created from user password");
			return cachedMasterKey;

		} catch (NoSuchAlgorithmException e) {
			throw new CredentialException("SHA-256 algorithm not available", e);
		}
	}

	@Override
	public void setMasterKey(String masterKey) throws CredentialException {
		// For user prompt provider, we don't store the key persistently
		// We just cache it in memory for this session
		this.cachedMasterKey = masterKey;
		LOGGER.log(Level.INFO, "Master key cached in memory for this session");
	}

	@Override
	public String getName() {
		return "Manual Password Entry";
	}

	@Override
	public String getDescription() {
		return "Enter master password manually each time you start SieveEditor (not stored)";
	}

	@Override
	public void close() {
		// Clear cached password from memory
		if (cachedMasterKey != null) {
			cachedMasterKey = null;
		}
	}
}
