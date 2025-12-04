package de.febrildur.sieveeditor.system;

import de.febrildur.sieveeditor.system.credentials.CredentialException;
import de.febrildur.sieveeditor.system.credentials.MasterKeyProvider;
import de.febrildur.sieveeditor.system.credentials.MasterKeyProviderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.properties.EncryptableProperties;

public class PropertiesSieve {

	private static final Logger LOGGER = Logger.getLogger(PropertiesSieve.class.getName());
	// Encryption algorithms in order of preference
	// AES-based (strongest, require IV generator)
	private static final String[] AES_ALGORITHMS = {
			"PBEWITHHMACSHA512ANDAES_256", // Strongest
			"PBEWITHHMACSHA256ANDAES_256" // Strong
	};
	// TripleDES-based (strong, no IV needed, widely compatible)
	private static final String[] TRIPLEDES_ALGORITHMS = {
			"PBEWithSHA1AndDESede", // TripleDES with SHA1
			"PBEWithMD5AndTripleDES" // TripleDES with MD5
	};
	// DES-based (weak but universally compatible, last resort)
	private static final String[] DES_ALGORITHMS = {
			"PBEWithMD5AndDES" // Original algorithm (weak)
	};
	private static final int KEY_OBTENTION_ITERATIONS = 10000;

	private final StandardPBEStringEncryptor encryptor;
	private final MasterKeyProvider masterKeyProvider;
	private String server = "";
	private int port = 4190; // Default ManageSieve port
	private String username = "";
	private String password = "";

	private String profileName;
	private String propFileName;

	public PropertiesSieve() {
		this("default");
	}

	public PropertiesSieve(String profileName) {
		this(profileName, null);
	}

	public PropertiesSieve(String profileName, String forcedBackend) {
		this.profileName = profileName;

		// Initialize master key provider (may show UI dialogs for user
		// selection/authentication)
		try {
			this.masterKeyProvider = MasterKeyProviderFactory.create(forcedBackend);
		} catch (CredentialException e) {
			LOGGER.log(Level.SEVERE, "Failed to initialize master key provider", e);
			throw new RuntimeException("Failed to initialize secure credential storage. " +
					"Please ensure you have a supported credential manager available.", e);
		}

		this.encryptor = createEncryptor();

		// Use platform-specific directory paths
		Path profilesDir = AppDirectoryService.getProfilesDir();
		this.propFileName = profilesDir.resolve(profileName + ".properties").toString();

		// Check if we need to migrate from old location
		if (AppDirectoryService.needsMigration()) {
			LOGGER.log(Level.INFO, "Legacy profiles found, migration needed");
			// Migration will be handled by Application class on startup
		}
	}

	/**
	 * Creates a configured encryptor with strong algorithm and master key from
	 * secure storage.
	 * Tries algorithms in order of strength, falling back to more compatible
	 * options.
	 *
	 * Algorithm tiers:
	 * 1. AES-based (strongest, requires IV generator)
	 * 2. TripleDES-based (strong, widely compatible, no IV needed)
	 * 3. DES-based (weak but universal, last resort)
	 *
	 * @return configured StandardPBEStringEncryptor
	 */
	private StandardPBEStringEncryptor createEncryptor() {
		// Get master key from secure storage (KeePassXC, OS keychain, or user prompt)
		String masterKey;
		try {
			masterKey = masterKeyProvider.getMasterKey();
		} catch (CredentialException e) {
			LOGGER.log(Level.SEVERE, "Failed to retrieve master key", e);
			throw new RuntimeException("Failed to retrieve master encryption key. " +
					"Cannot proceed without encryption key.", e);
		}

		// If this is first time, generate and store a random master key
		if (masterKey == null || masterKey.isEmpty()) {
			masterKey = generateRandomMasterKey();
			try {
				masterKeyProvider.setMasterKey(masterKey);
				LOGGER.log(Level.INFO, "Generated and stored new master key");
			} catch (CredentialException e) {
				LOGGER.log(Level.SEVERE, "Failed to store new master key", e);
				throw new RuntimeException("Failed to store master encryption key", e);
			}
		}

		Exception lastException = null;

		// Tier 1: Try AES algorithms (require IV generator)
		for (String algorithm : AES_ALGORITHMS) {
			try {
				StandardPBEStringEncryptor enc = new StandardPBEStringEncryptor();
				enc.setAlgorithm(algorithm);
				enc.setIvGenerator(new RandomIvGenerator()); // REQUIRED for AES algorithms
				enc.setKeyObtentionIterations(KEY_OBTENTION_ITERATIONS);
				enc.setPassword(masterKey);
				// Test the algorithm by encrypting a test string
				enc.encrypt("test");
				LOGGER.log(Level.INFO, "Using AES encryption algorithm: {0}", algorithm);
				return enc;
			} catch (Exception e) {
				LOGGER.log(Level.FINE, "AES algorithm {0} not available: {1}",
						new Object[] { algorithm, e.getMessage() });
				lastException = e;
			}
		}

		// Tier 2: Try TripleDES algorithms (no IV generator needed)
		for (String algorithm : TRIPLEDES_ALGORITHMS) {
			try {
				StandardPBEStringEncryptor enc = new StandardPBEStringEncryptor();
				enc.setAlgorithm(algorithm);
				// No IV generator needed for TripleDES
				enc.setKeyObtentionIterations(KEY_OBTENTION_ITERATIONS);
				enc.setPassword(masterKey);
				// Test the algorithm by encrypting a test string
				enc.encrypt("test");
				LOGGER.log(Level.INFO, "Using TripleDES encryption algorithm: {0}", algorithm);
				return enc;
			} catch (Exception e) {
				LOGGER.log(Level.FINE, "TripleDES algorithm {0} not available: {1}",
						new Object[] { algorithm, e.getMessage() });
				lastException = e;
			}
		}

		// Tier 3: Last resort - DES algorithm (weak but universally compatible)
		for (String algorithm : DES_ALGORITHMS) {
			try {
				StandardPBEStringEncryptor enc = new StandardPBEStringEncryptor();
				enc.setAlgorithm(algorithm);
				// No IV generator needed for DES
				enc.setKeyObtentionIterations(KEY_OBTENTION_ITERATIONS);
				enc.setPassword(masterKey);
				// Test the algorithm by encrypting a test string
				enc.encrypt("test");
				LOGGER.log(Level.WARNING, "Using weak DES encryption algorithm: {0}. " +
						"Consider upgrading JCE for stronger encryption.", algorithm);
				return enc;
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Even DES algorithm {0} failed: {1}",
						new Object[] { algorithm, e.getMessage() });
				lastException = e;
			}
		}

		// None of the algorithms worked
		LOGGER.log(Level.SEVERE, "No encryption algorithm available", lastException);
		throw new RuntimeException("No suitable encryption algorithm available. " +
				"Please ensure Java Cryptography Extension (JCE) is properly configured.", lastException);
	}

	/**
	 * Generates a cryptographically random master key for first-time setup.
	 *
	 * @return Base64-encoded random key
	 */
	private String generateRandomMasterKey() {
		SecureRandom random = new SecureRandom();
		byte[] keyBytes = new byte[32]; // 256 bits
		random.nextBytes(keyBytes);
		String key = Base64.getEncoder().encodeToString(keyBytes);
		LOGGER.log(Level.INFO, "Generated new random master key");
		return key;
	}

	public void load() throws IOException {
		File propFile = new File(propFileName);
		if (propFile.createNewFile()) {
			// New file created - set permissions
			AppDirectoryService.setSecureFilePermissions(propFile.toPath());
		}

		try (InputStream input = new FileInputStream(propFileName)) {
			// Encryptor is already configured with master key from secure storage
			Properties prop = new EncryptableProperties(encryptor);

			prop.load(input);

			server = prop.getProperty("sieve.server", "");
			port = Integer.valueOf(prop.getProperty("sieve.port", "4190"));
			username = prop.getProperty("sieve.user", "");
			try {
				password = prop.getProperty("sieve.password", "");
			} catch (EncryptionOperationNotPossibleException e) {
				// Decryption failed - possibly encrypted with old key or corrupted
				LOGGER.log(Level.WARNING, "Failed to decrypt password - may need re-entry after key change", e);
				password = "";
			}
		}
	}

	public void write() {
		try (OutputStream output = new FileOutputStream(propFileName)) {
			// Encryptor is already configured with master key from secure storage
			Properties prop = new EncryptableProperties(encryptor);

			// set the properties value - handle null values
			prop.setProperty("sieve.server", server != null ? server : "");
			prop.setProperty("sieve.port", Integer.toString(port));
			prop.setProperty("sieve.user", username != null ? username : "");
			prop.setProperty("sieve.password",
					password != null ? String.format("ENC(%s)", encryptor.encrypt(password)) : "");

			prop.store(output, null);

			// Ensure file has secure permissions after writing
			AppDirectoryService.setSecureFilePermissions(Paths.get(propFileName));

		} catch (IOException io) {
			LOGGER.log(Level.SEVERE, "Failed to write properties file: " + propFileName, io);
		}
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public static List<String> getAvailableProfiles() {
		Path profilesDir = AppDirectoryService.getProfilesDir();
		File profilesDirFile = profilesDir.toFile();

		if (!profilesDirFile.exists() || profilesDirFile.listFiles() == null) {
			return Arrays.asList("default");
		}

		List<String> profiles = Arrays.stream(profilesDirFile.listFiles())
				.filter(f -> f.getName().endsWith(".properties"))
				.map(f -> f.getName().replace(".properties", ""))
				.sorted()
				.collect(Collectors.toList());

		if (profiles.isEmpty()) {
			return Arrays.asList("default");
		}
		return profiles;
	}

	public static String getLastUsedProfile() {
		Path lastUsedFile = AppDirectoryService.getUserConfigDir().resolve(".lastused");
		if (!Files.exists(lastUsedFile)) {
			return "default";
		}
		try {
			String profile = Files.readString(lastUsedFile).trim();
			return profile.isEmpty() ? "default" : profile;
		} catch (IOException e) {
			return "default";
		}
	}

	public static void saveLastUsedProfile(String profileName) {
		Path lastUsedFile = AppDirectoryService.getUserConfigDir().resolve(".lastused");
		try {
			Files.writeString(lastUsedFile, profileName);
			AppDirectoryService.setSecureFilePermissions(lastUsedFile);
		} catch (IOException e) {
			// Ignore - not critical
			LOGGER.log(Level.FINE, "Failed to save last used profile", e);
		}
	}

	public static boolean profileExists(String profileName) {
		Path lastUsedFile = AppDirectoryService.getUserConfigDir().resolve(".lastused");

		Path profileFile = AppDirectoryService.getProfilesDir().resolve(profileName + ".properties");
		return Files.exists(profileFile);
	}

	/**
	 * Deletes a profile by name.
	 *
	 * @param profileName the name of the profile to delete
	 * @return true if the profile was deleted, false if it didn't exist
	 */
	public static boolean deleteProfile(String profileName) {
		Path profileFile = AppDirectoryService.getProfilesDir().resolve(profileName + ".properties");
		try {
			boolean deleted = Files.deleteIfExists(profileFile);
			if (deleted) {
				LOGGER.log(Level.INFO, "Deleted profile: {0}", profileName);
			} else {
				LOGGER.log(Level.FINE, "Profile does not exist: {0}", profileName);
			}
			return deleted;
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to delete profile: " + profileName, e);
			return false;
		}
	}

	/**
	 * Migrates profiles from old ~/.sieveprofiles to new platform-specific
	 * locations.
	 */
	public static void migrateOldProperties() {
		Path legacyDir = AppDirectoryService.getLegacyProfilesDir();
		if (!Files.exists(legacyDir)) {
			return; // Nothing to migrate
		}

		Path newProfilesDir = AppDirectoryService.getProfilesDir();
		LOGGER.log(Level.INFO, "Migrating profiles from {0} to {1}",
				new Object[] { legacyDir, newProfilesDir });

		try {
			Files.list(legacyDir)
					.filter(p -> p.toString().endsWith(".properties"))
					.forEach(oldFile -> {
						try {
							Path newFile = newProfilesDir.resolve(oldFile.getFileName());
							if (!Files.exists(newFile)) {
								Files.copy(oldFile, newFile);
								AppDirectoryService.setSecureFilePermissions(newFile);
								LOGGER.log(Level.INFO, "Migrated profile: {0}", oldFile.getFileName());
							}
						} catch (IOException e) {
							LOGGER.log(Level.WARNING, "Failed to migrate profile: " + oldFile, e);
						}
					});

			// Also migrate .lastused file if it exists
			Path oldLastUsed = legacyDir.resolve(".lastused");
			if (Files.exists(oldLastUsed)) {
				Path newLastUsed = AppDirectoryService.getUserConfigDir().resolve(".lastused");
				if (!Files.exists(newLastUsed)) {
					Files.copy(oldLastUsed, newLastUsed);
					AppDirectoryService.setSecureFilePermissions(newLastUsed);
				}
			}

			LOGGER.log(Level.INFO, "Migration completed successfully");
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to migrate profiles", e);
		}
	}

}
