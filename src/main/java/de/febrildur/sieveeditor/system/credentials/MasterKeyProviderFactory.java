package de.febrildur.sieveeditor.system.credentials;

import de.febrildur.sieveeditor.system.AppDirectoryService;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for creating MasterKeyProvider instances.
 *
 * Tries providers in this order:
 * 1. User's saved preference (if exists)
 * 2. KeePassXC (if available and user accepts)
 * 3. OS Keychain (if available and user accepts)
 * 4. User Prompt (always available, ultimate fallback)
 *
 * Shows a selection dialog on first run to let user choose their preferred backend.
 */
public class MasterKeyProviderFactory {

	private static final Logger LOGGER = Logger.getLogger(MasterKeyProviderFactory.class.getName());
	private static final String PREFERENCE_FILE = ".storage-backend";

	/**
	 * Creates a MasterKeyProvider instance based on user preference or auto-detection.
	 *
	 * @return configured MasterKeyProvider instance
	 * @throws CredentialException if no provider can be created
	 */
	public static MasterKeyProvider create() throws CredentialException {
		return create(null);
	}

	/**
	 * Creates a MasterKeyProvider instance with explicit backend selection.
	 *
	 * @param forcedBackend backend to use (keepassxc, keychain, prompt), or null for auto-detection
	 * @return configured MasterKeyProvider instance
	 * @throws CredentialException if no provider can be created
	 */
	public static MasterKeyProvider create(String forcedBackend) throws CredentialException {
		// If backend is explicitly specified via command-line, use it
		if (forcedBackend != null) {
			LOGGER.log(Level.INFO, "Using forced backend from command-line: {0}", forcedBackend);
			MasterKeyProvider provider = createProviderByBackendArg(forcedBackend);
			if (provider.isAvailable()) {
				return provider;
			} else {
				LOGGER.log(Level.WARNING, "Forced backend {0} is not available, falling back to selection dialog", forcedBackend);
			}
		}
		// Check if user has a saved preference
		String savedBackend = loadSavedBackend();
		if (savedBackend != null) {
			try {
				MasterKeyProvider provider = createProviderByName(savedBackend);
				if (provider.isAvailable()) {
					LOGGER.log(Level.INFO, "Using saved backend preference: {0}", savedBackend);
					return provider;
				} else {
					LOGGER.log(Level.WARNING, "Saved backend {0} is no longer available, showing selection dialog", savedBackend);
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to create saved backend " + savedBackend, e);
			}
		}

		// No saved preference or it's not available - show selection dialog
		return showBackendSelectionDialog();
	}

	/**
	 * Shows a dialog letting the user choose which backend to use.
	 *
	 * @return selected and configured MasterKeyProvider
	 * @throws CredentialException if user cancels or no provider can be created
	 */
	private static MasterKeyProvider showBackendSelectionDialog() throws CredentialException {
		List<MasterKeyProvider> availableProviders = new ArrayList<>();
		MasterKeyProvider userPromptProvider = null;

		// Try KeePassXC
		KeePassXCMasterKeyProvider keepassProvider = new KeePassXCMasterKeyProvider();
		if (keepassProvider.isAvailable()) {
			availableProviders.add(keepassProvider);
		}

		// Try OS Keychain
		OSKeychainMasterKeyProvider keychainProvider = new OSKeychainMasterKeyProvider();
		if (keychainProvider.isAvailable()) {
			availableProviders.add(keychainProvider);
		}

		// User Prompt is always available
		userPromptProvider = new UserPromptMasterKeyProvider();
		availableProviders.add(userPromptProvider);

		// Build dialog
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(new JLabel("How should SieveEditor store your master password?"));
		panel.add(new JLabel(" "));
		panel.add(new JLabel("The master password encrypts your server passwords."));
		panel.add(new JLabel("Choose the most convenient and secure option for you:"));
		panel.add(new JLabel(" "));

		ButtonGroup group = new ButtonGroup();
		List<JRadioButton> buttons = new ArrayList<>();
		List<MasterKeyProvider> correspondingProviders = new ArrayList<>();

		for (MasterKeyProvider provider : availableProviders) {
			JRadioButton button = new JRadioButton(
				"<html><b>" + provider.getName() + "</b><br>" +
					"<small>" + provider.getDescription() + "</small></html>"
			);
			button.setAlignmentX(Component.LEFT_ALIGNMENT);
			group.add(button);
			buttons.add(button);
			correspondingProviders.add(provider);
			panel.add(button);
			panel.add(Box.createVerticalStrut(10));
		}

		// Pre-select first option (KeePassXC if available, else OS Keychain, else User Prompt)
		if (!buttons.isEmpty()) {
			buttons.get(0).setSelected(true);
		}

		int result = JOptionPane.showConfirmDialog(
			null,
			panel,
			"Choose Master Password Storage",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE
		);

		if (result != JOptionPane.OK_OPTION) {
			throw new CredentialException("User cancelled backend selection");
		}

		// Find which button was selected
		for (int i = 0; i < buttons.size(); i++) {
			if (buttons.get(i).isSelected()) {
				MasterKeyProvider selected = correspondingProviders.get(i);
				LOGGER.log(Level.INFO, "User selected backend: {0}", selected.getName());

				// Save preference
				saveBackendPreference(selected.getName());

				return selected;
			}
		}

		// Fallback to user prompt (should never reach here)
		LOGGER.log(Level.WARNING, "No backend selected, falling back to user prompt");
		return userPromptProvider;
	}

	/**
	 * Creates a provider instance by name.
	 *
	 * @param name provider name
	 * @return provider instance
	 */
	private static MasterKeyProvider createProviderByName(String name) {
		return switch (name) {
			case "KeePassXC" -> new KeePassXCMasterKeyProvider();
			case "Windows Credential Manager", "macOS Keychain", "Linux Secret Service", "System Keychain" ->
				new OSKeychainMasterKeyProvider();
			case "Manual Password Entry" -> new UserPromptMasterKeyProvider();
			default -> {
				LOGGER.log(Level.WARNING, "Unknown backend name: {0}, falling back to user prompt", name);
				yield new UserPromptMasterKeyProvider();
			}
		};
	}

	/**
	 * Creates a provider instance by command-line argument.
	 *
	 * @param arg backend argument from command line (keepassxc, keychain, prompt)
	 * @return provider instance
	 */
	private static MasterKeyProvider createProviderByBackendArg(String arg) {
		return switch (arg.toLowerCase()) {
			case "keepassxc", "keepass" -> new KeePassXCMasterKeyProvider();
			case "keychain", "os", "system" -> new OSKeychainMasterKeyProvider();
			case "prompt", "manual", "password" -> new UserPromptMasterKeyProvider();
			default -> {
				LOGGER.log(Level.WARNING, "Unknown backend argument: {0}, falling back to user prompt", arg);
				yield new UserPromptMasterKeyProvider();
			}
		};
	}

	/**
	 * Loads the saved backend preference from config file.
	 *
	 * @return backend name or null if not saved
	 */
	private static String loadSavedBackend() {
		try {
			Path prefFile = AppDirectoryService.getUserConfigDir().resolve(PREFERENCE_FILE);
			if (Files.exists(prefFile)) {
				String content = Files.readString(prefFile).trim();
				if (!content.isEmpty()) {
					return content;
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to load saved backend preference", e);
		}
		return null;
	}

	/**
	 * Saves the backend preference to config file.
	 *
	 * @param backendName name of the backend
	 */
	private static void saveBackendPreference(String backendName) {
		try {
			Path prefFile = AppDirectoryService.getUserConfigDir().resolve(PREFERENCE_FILE);
			Files.writeString(prefFile, backendName);
			AppDirectoryService.setSecureFilePermissions(prefFile);
			LOGGER.log(Level.INFO, "Saved backend preference: {0}", backendName);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to save backend preference", e);
		}
	}

	/**
	 * Allows user to change their backend preference.
	 *
	 * @return new MasterKeyProvider instance
	 * @throws CredentialException if selection fails
	 */
	public static MasterKeyProvider changeBackend() throws CredentialException {
		return showBackendSelectionDialog();
	}
}
