package de.febrildur.sieveeditor.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.properties.EncryptableProperties;

public class PropertiesSieve {

	private final StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
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
		File profilesDir = new File(System.getProperty("user.home"), ".sieveprofiles");
		if (!profilesDir.exists()) {
			profilesDir.mkdirs();
		}
		this.propFileName = new File(profilesDir, profileName + ".properties").getAbsolutePath();
	}
	
	public void load() throws IOException {
		File propFile = new File(propFileName);
		propFile.createNewFile();
		try (InputStream input = new FileInputStream(propFileName)) {
			encryptor.setPassword("KNQ4VnqF24WLe4HZJ9fB9Sth");
			Properties prop = new EncryptableProperties(encryptor);  

			prop.load(input);

			server = prop.getProperty("sieve.server", "");
			port = Integer.valueOf(prop.getProperty("sieve.port", "4190"));
			username = prop.getProperty("sieve.user", "");
			try {
				password = prop.getProperty("sieve.password", "");
			} catch (EncryptionOperationNotPossibleException e) {
				password = "";
			}
		}
	}

	public void write() {
		try (OutputStream output = new FileOutputStream(propFileName)) {
			encryptor.setPassword("KNQ4VnqF24WLe4HZJ9fB9Sth");
			Properties prop = new EncryptableProperties(encryptor);

			// set the properties value
			prop.setProperty("sieve.server", server);
			prop.setProperty("sieve.port", Integer.toString(port));
			prop.setProperty("sieve.user", username);
			prop.setProperty("sieve.password", String.format("ENC(%s)", encryptor.encrypt(password)));

			prop.store(output, null);

		} catch (IOException io) {
			io.printStackTrace();
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
			return Files.readString(lastUsedFile.toPath()).trim();
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
