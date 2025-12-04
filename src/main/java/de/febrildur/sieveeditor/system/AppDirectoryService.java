package de.febrildur.sieveeditor.system;

import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides platform-specific application directories following OS standards:
 * - Linux: XDG Base Directory Specification (~/.local/share, ~/.config,
 * ~/.cache)
 * - Windows: AppData (%LOCALAPPDATA%)
 * - macOS: Application Support (~/Library/Application Support)
 */
public class AppDirectoryService {

	private static final Logger LOGGER = Logger.getLogger(AppDirectoryService.class.getName());
	private static final String APP_NAME = "sieveeditor";
	private static final String APP_AUTHOR = "febrildur";
	private static final String APP_VERSION = null; // Don't include version in path

	private static final String SIEVEEDITOR_TEST_DIR = "sieveeditor.test.dir";

	private static final AppDirs APP_DIRS = AppDirsFactory.getInstance();

	/**
	 * Gets the user data directory for storing profiles and application data.
	 *
	 * Platform-specific paths:
	 * - Linux: ~/.local/share/sieveeditor/
	 * - Windows: C:\Users\<User>\AppData\Local\febrildur\sieveeditor\
	 * - macOS: ~/Library/Application Support/sieveeditor/
	 *
	 * @return Path to user data directory
	 */
	public static Path getUserDataDir() {
		Path path = null;
		if (System.getProperties().containsKey(SIEVEEDITOR_TEST_DIR)) {
			path = Path.of(System.getProperty(SIEVEEDITOR_TEST_DIR));
			LOGGER.log(Level.WARNING,
					"AppDirectoryService.getUserDataDir: Using explicit properties file location. Should only be used in testing. BE CAREFUL! \n Using:"
							+ path);
		} else {
			String dataDir = APP_DIRS.getUserDataDir(APP_NAME, APP_VERSION, APP_AUTHOR);
			path = Paths.get(dataDir);
		}
		ensureDirectoryExists(path, "700");
		return path;
	}

	/**
	 * Gets the profiles directory for storing server connection profiles.
	 *
	 * @return Path to profiles directory (inside user data dir)
	 */
	public static Path getProfilesDir() {
		Path profilesDir = getUserDataDir().resolve("profiles");
		ensureDirectoryExists(profilesDir, "700");
		return profilesDir;
	}

	/**
	 * Gets the config directory for storing application settings.
	 *
	 * Platform-specific paths:
	 * - Linux: ~/.config/sieveeditor/
	 * - Windows: C:\Users\<User>\AppData\Local\febrildur\sieveeditor\
	 * - macOS: ~/Library/Preferences/sieveeditor/
	 *
	 * @return Path to config directory
	 */
	public static Path getUserConfigDir() {
		String configDir = APP_DIRS.getUserConfigDir(APP_NAME, APP_VERSION, APP_AUTHOR);
		Path path = Paths.get(configDir);
		ensureDirectoryExists(path, "700");
		return path;
	}

	/**
	 * Gets the legacy profile directory (~/.sieveprofiles) for migration.
	 *
	 * @return Path to old profiles directory
	 */
	public static Path getLegacyProfilesDir() {
		return Paths.get(System.getProperty("user.home"), ".sieveprofiles");
	}

	/**
	 * Checks if legacy profiles directory exists and contains files.
	 *
	 * @return true if migration is needed
	 */
	public static boolean needsMigration() {
		Path legacyDir = getLegacyProfilesDir();
		if (!Files.exists(legacyDir)) {
			return false;
		}

		try {
			return Files.list(legacyDir)
					.anyMatch(p -> p.toString().endsWith(".properties"));
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to check legacy directory", e);
			return false;
		}
	}

	/**
	 * Ensures a directory exists with secure permissions (700 on POSIX systems).
	 *
	 * @param dir        Path to directory
	 * @param posixPerms POSIX permissions string (e.g., "700", "600")
	 */
	private static void ensureDirectoryExists(Path dir, String posixPerms) {
		if (Files.exists(dir)) {
			return;
		}

		try {
			Files.createDirectories(dir);
			LOGGER.log(Level.INFO, "Created directory: {0}", dir);

			// Set permissions on POSIX systems (Linux, macOS)
			try {
				Set<PosixFilePermission> perms = PosixFilePermissions.fromString(
						"rwx------".substring(0, posixPerms.length() == 3 ? 9 : 6));
				Files.setPosixFilePermissions(dir, perms);
				LOGGER.log(Level.FINE, "Set directory permissions to {0} for: {1}",
						new Object[] { posixPerms, dir });
			} catch (UnsupportedOperationException e) {
				// Non-POSIX system (Windows) - permissions handled by OS
				LOGGER.log(Level.FINE, "POSIX permissions not supported on this OS");
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to create directory: " + dir, e);
			throw new RuntimeException("Failed to create application directory: " + dir, e);
		}
	}

	/**
	 * Sets secure permissions on a file (600 on POSIX systems).
	 *
	 * @param filePath path to the file
	 */
	public static void setSecureFilePermissions(Path filePath) {
		try {
			Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
			Files.setPosixFilePermissions(filePath, perms);
			LOGGER.log(Level.FINE, "Set file permissions to 600 for: {0}", filePath);
		} catch (UnsupportedOperationException e) {
			// Non-POSIX system (Windows) - permissions handled by OS
			LOGGER.log(Level.FINE, "POSIX permissions not supported on this OS");
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to set file permissions", e);
		}
	}
}
