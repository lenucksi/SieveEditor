package de.febrildur.sieveeditor.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.properties.EncryptableProperties;

public class PropertiesSieve {

	private static final Logger LOGGER = Logger.getLogger(PropertiesSieve.class.getName());
	private static final String ENCRYPTION_ALGORITHM = "PBEWITHHMACSHA512ANDAES_256";
	private static final int KEY_OBTENTION_ITERATIONS = 10000;

	private final StandardPBEStringEncryptor encryptor;
	private String server;
	private int port;
	private String username;
	private String password;

	private String profileName;
	private String propFileName;

	public PropertiesSieve() {
		this("default");
	}

	public PropertiesSieve(String profileName) {
		this.profileName = profileName;
		this.encryptor = createEncryptor();
		File profilesDir = new File(System.getProperty("user.home"), ".sieveprofiles");
		if (!profilesDir.exists()) {
			profilesDir.mkdirs();
			setDirectoryPermissions(profilesDir.toPath());
		}
		this.propFileName = new File(profilesDir, profileName + ".properties").getAbsolutePath();
	}

	/**
	 * Creates a configured encryptor with strong algorithm and machine-specific key.
	 *
	 * @return configured StandardPBEStringEncryptor
	 */
	private StandardPBEStringEncryptor createEncryptor() {
		StandardPBEStringEncryptor enc = new StandardPBEStringEncryptor();
		enc.setAlgorithm(ENCRYPTION_ALGORITHM);
		enc.setKeyObtentionIterations(KEY_OBTENTION_ITERATIONS);
		enc.setPassword(getMachineSpecificEncryptionKey());
		return enc;
	}

	/**
	 * Generates a machine-specific encryption key based on username, hostname,
	 * and hardware MAC address. This provides better security than a hardcoded key
	 * while remaining deterministic for the same machine.
	 *
	 * @return Base64-encoded SHA-256 hash of machine-specific data
	 */
	private String getMachineSpecificEncryptionKey() {
		try {
			StringBuilder keyMaterial = new StringBuilder();

			// Add username
			keyMaterial.append(System.getProperty("user.name", "unknown"));

			// Add hostname
			try {
				keyMaterial.append(InetAddress.getLocalHost().getHostName());
			} catch (UnknownHostException e) {
				LOGGER.log(Level.WARNING, "Could not get hostname, using fallback", e);
				keyMaterial.append("localhost");
			}

			// Add MAC address for hardware binding
			try {
				InetAddress localHost = InetAddress.getLocalHost();
				NetworkInterface network = NetworkInterface.getByInetAddress(localHost);

				if (network != null) {
					byte[] mac = network.getHardwareAddress();
					if (mac != null) {
						for (byte b : mac) {
							keyMaterial.append(String.format("%02X", b));
						}
					}
				} else {
					// Fallback: iterate through network interfaces to find one with MAC
					Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
					while (interfaces.hasMoreElements()) {
						NetworkInterface ni = interfaces.nextElement();
						byte[] mac = ni.getHardwareAddress();
						if (mac != null && mac.length > 0) {
							for (byte b : mac) {
								keyMaterial.append(String.format("%02X", b));
							}
							break;
						}
					}
				}
			} catch (SocketException | UnknownHostException e) {
				LOGGER.log(Level.WARNING, "Could not get MAC address, using fallback", e);
				keyMaterial.append("NO-MAC-ADDRESS");
			}

			// Hash the key material to create consistent-length key
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(keyMaterial.toString().getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash);

		} catch (NoSuchAlgorithmException e) {
			LOGGER.log(Level.SEVERE, "SHA-256 not available, using fallback", e);
			// This should never happen on modern JVMs, but provide a fallback
			return "FALLBACK-KEY-" + System.getProperty("user.name");
		}
	}

	/**
	 * Sets secure permissions on the profiles directory (700 - owner only).
	 * On non-POSIX systems, this will be a no-op.
	 *
	 * @param dirPath path to the directory
	 */
	private void setDirectoryPermissions(Path dirPath) {
		try {
			Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwx------");
			Files.setPosixFilePermissions(dirPath, perms);
			LOGGER.log(Level.FINE, "Set directory permissions to 700 for: {0}", dirPath);
		} catch (UnsupportedOperationException e) {
			// Non-POSIX system (e.g., Windows) - permissions handled by OS
			LOGGER.log(Level.FINE, "POSIX permissions not supported on this OS");
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to set directory permissions", e);
		}
	}

	/**
	 * Sets secure permissions on the profile file (600 - owner read/write only).
	 * On non-POSIX systems, this will be a no-op.
	 *
	 * @param filePath path to the file
	 */
	private void setFilePermissions(Path filePath) {
		try {
			Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
			Files.setPosixFilePermissions(filePath, perms);
			LOGGER.log(Level.FINE, "Set file permissions to 600 for: {0}", filePath);
		} catch (UnsupportedOperationException e) {
			// Non-POSIX system (e.g., Windows) - permissions handled by OS
			LOGGER.log(Level.FINE, "POSIX permissions not supported on this OS");
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to set file permissions", e);
		}
	}

	public void load() throws IOException {
		File propFile = new File(propFileName);
		if (propFile.createNewFile()) {
			// New file created - set permissions
			setFilePermissions(propFile.toPath());
		}

		try (InputStream input = new FileInputStream(propFileName)) {
			// Encryptor is already configured with machine-specific key in constructor
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
			// Encryptor is already configured with machine-specific key in constructor
			Properties prop = new EncryptableProperties(encryptor);

			// set the properties value - handle null values
			prop.setProperty("sieve.server", server != null ? server : "");
			prop.setProperty("sieve.port", Integer.toString(port));
			prop.setProperty("sieve.user", username != null ? username : "");
			prop.setProperty("sieve.password", password != null ?
				String.format("ENC(%s)", encryptor.encrypt(password)) : "");

			prop.store(output, null);

			// Ensure file has secure permissions after writing
			setFilePermissions(Paths.get(propFileName));

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

	// Profile management methods
	public static List<String> getAvailableProfiles() {
		File profilesDir = new File(System.getProperty("user.home"), ".sieveprofiles");
		if (!profilesDir.exists() || profilesDir.listFiles() == null) {
			return Arrays.asList("default");
		}

		List<String> profiles = Arrays.stream(profilesDir.listFiles())
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
		File lastUsedFile = new File(System.getProperty("user.home"),
			".sieveprofiles/.lastused");
		if (!lastUsedFile.exists()) {
			return "default";
		}
		try {
			String profile = Files.readString(lastUsedFile.toPath()).trim();
			return profile.isEmpty() ? "default" : profile;
		} catch (IOException e) {
			return "default";
		}
	}

	public static void saveLastUsedProfile(String profileName) {
		File lastUsedFile = new File(System.getProperty("user.home"),
			".sieveprofiles/.lastused");
		try {
			Files.writeString(lastUsedFile.toPath(), profileName);
		} catch (IOException e) {
			// Ignore - not critical
		}
	}

	public static boolean profileExists(String profileName) {
		File profileFile = new File(System.getProperty("user.home"),
			".sieveprofiles/" + profileName + ".properties");
		return profileFile.exists();
	}

	public static void migrateOldProperties() {
		// Check if old ~/.sieveproperties exists
		File oldFile = new File(System.getProperty("user.home"), ".sieveproperties");
		if (!oldFile.exists()) {
			return; // Nothing to migrate
		}

		// Create new profiles directory
		File profilesDir = new File(System.getProperty("user.home"), ".sieveprofiles");
		profilesDir.mkdirs();

		// Move old file to default.properties
		File newFile = new File(profilesDir, "default.properties");
		if (!newFile.exists()) {
			try {
				Files.copy(oldFile.toPath(), newFile.toPath());
				System.out.println("Migrated old properties to default profile");
			} catch (IOException e) {
				System.err.println("Failed to migrate: " + e.getMessage());
			}
		}
	}

}
