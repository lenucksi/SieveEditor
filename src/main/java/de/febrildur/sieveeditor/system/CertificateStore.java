package de.febrildur.sieveeditor.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages storage and retrieval of user certificate trust decisions.
 * Stores SHA-256 fingerprints of certificates that users have explicitly
 * trusted or rejected.
 */
public class CertificateStore {

	private static final Logger LOGGER = Logger.getLogger(CertificateStore.class.getName());
	private static final String STORE_FILE = "certificates.properties";
	private static final String TRUSTED_PREFIX = "trusted.";
	private static final String REJECTED_PREFIX = "rejected.";

	private final File storeFile;
	private final Properties properties;

	public CertificateStore() {
		File profilesDir = new File(System.getProperty("user.home"), ".sieveprofiles");
		if (!profilesDir.exists()) {
			profilesDir.mkdirs();
			setDirectoryPermissions(profilesDir.toPath());
		}

		this.storeFile = new File(profilesDir, STORE_FILE);
		this.properties = new Properties();

		if (storeFile.exists()) {
			load();
		} else {
			try {
				if (storeFile.createNewFile()) {
					setFilePermissions(storeFile.toPath());
				}
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Failed to create certificate store file", e);
			}
		}
	}

	/**
	 * Calculates the SHA-256 fingerprint of a certificate.
	 *
	 * @param cert the certificate
	 * @return Base64-encoded SHA-256 fingerprint
	 * @throws CertificateEncodingException if certificate encoding fails
	 */
	public static String getFingerprint(Certificate cert) throws CertificateEncodingException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] encoded = cert.getEncoded();
			byte[] digest = md.digest(encoded);
			return Base64.getEncoder().encodeToString(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 not available", e);
		}
	}

	/**
	 * Formats a fingerprint in human-readable colon-separated hex format.
	 *
	 * @param cert the certificate
	 * @return fingerprint like "AA:BB:CC:DD:..."
	 */
	public static String getFormattedFingerprint(X509Certificate cert) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(cert.getEncoded());
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < digest.length; i++) {
				if (i > 0)
					sb.append(":");
				sb.append(String.format("%02X", digest[i]));
			}
			return sb.toString();
		} catch (Exception e) {
			return "Unable to calculate fingerprint";
		}
	}

	/**
	 * Checks if a certificate has been explicitly trusted by the user.
	 *
	 * @param cert the certificate to check
	 * @return true if trusted, false otherwise
	 */
	public synchronized boolean isTrusted(Certificate cert) {
		try {
			String fingerprint = getFingerprint(cert);
			String key = TRUSTED_PREFIX + fingerprint;
			return properties.containsKey(key);
		} catch (CertificateEncodingException e) {
			LOGGER.log(Level.WARNING, "Failed to get certificate fingerprint", e);
			return false;
		}
	}

	/**
	 * Checks if a certificate has been explicitly rejected by the user.
	 *
	 * @param cert the certificate to check
	 * @return true if rejected, false otherwise
	 */
	public synchronized boolean isRejected(Certificate cert) {
		try {
			String fingerprint = getFingerprint(cert);
			String key = REJECTED_PREFIX + fingerprint;
			return properties.containsKey(key);
		} catch (CertificateEncodingException e) {
			LOGGER.log(Level.WARNING, "Failed to get certificate fingerprint", e);
			return false;
		}
	}

	/**
	 * Marks a certificate as trusted by the user.
	 *
	 * @param cert       the certificate to trust
	 * @param serverName the server name (for display purposes)
	 */
	public synchronized void trustCertificate(X509Certificate cert, String serverName) {
		try {
			String fingerprint = getFingerprint(cert);
			String key = TRUSTED_PREFIX + fingerprint;
			String value = String.format("%s|%s|%s",
					serverName,
					cert.getSubjectX500Principal().getName(),
					System.currentTimeMillis());
			properties.setProperty(key, value);

			// Remove from rejected list if present
			properties.remove(REJECTED_PREFIX + fingerprint);

			save();
			LOGGER.log(Level.INFO, "Certificate trusted for server: {0}", serverName);
		} catch (CertificateEncodingException e) {
			LOGGER.log(Level.SEVERE, "Failed to trust certificate", e);
		}
	}

	/**
	 * Marks a certificate as rejected by the user.
	 *
	 * @param cert       the certificate to reject
	 * @param serverName the server name (for display purposes)
	 */
	public synchronized void rejectCertificate(X509Certificate cert, String serverName) {
		try {
			String fingerprint = getFingerprint(cert);
			String key = REJECTED_PREFIX + fingerprint;
			String value = String.format("%s|%s|%s",
					serverName,
					cert.getSubjectX500Principal().getName(),
					System.currentTimeMillis());
			properties.setProperty(key, value);

			// Remove from trusted list if present
			properties.remove(TRUSTED_PREFIX + fingerprint);

			save();
			LOGGER.log(Level.INFO, "Certificate rejected for server: {0}", serverName);
		} catch (CertificateEncodingException e) {
			LOGGER.log(Level.SEVERE, "Failed to reject certificate", e);
		}
	}

	/**
	 * Removes a certificate decision (trusted or rejected).
	 *
	 * @param cert the certificate
	 */
	public synchronized void removeCertificate(Certificate cert) {
		try {
			String fingerprint = getFingerprint(cert);
			properties.remove(TRUSTED_PREFIX + fingerprint);
			properties.remove(REJECTED_PREFIX + fingerprint);
			save();
		} catch (CertificateEncodingException e) {
			LOGGER.log(Level.WARNING, "Failed to remove certificate", e);
		}
	}

	/**
	 * Clears all stored certificate decisions.
	 */
	public synchronized void clear() {
		properties.clear();
		save();
		LOGGER.log(Level.INFO, "Cleared all certificate decisions");
	}

	private void load() {
		try (FileInputStream fis = new FileInputStream(storeFile)) {
			properties.load(fis);
			LOGGER.log(Level.FINE, "Loaded certificate store: {0} entries", properties.size());
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to load certificate store", e);
		}
	}

	private void save() {
		try (FileOutputStream fos = new FileOutputStream(storeFile)) {
			properties.store(fos, "SieveEditor Certificate Trust Store");
			setFilePermissions(storeFile.toPath());
			LOGGER.log(Level.FINE, "Saved certificate store: {0} entries", properties.size());
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to save certificate store", e);
		}
	}

	private void setFilePermissions(Path filePath) {
		try {
			Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
			Files.setPosixFilePermissions(filePath, perms);
		} catch (UnsupportedOperationException e) {
			// Non-POSIX system (Windows) - permissions handled by OS
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to set file permissions", e);
		}
	}

	private void setDirectoryPermissions(Path dirPath) {
		try {
			Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwx------");
			Files.setPosixFilePermissions(dirPath, perms);
		} catch (UnsupportedOperationException e) {
			// Non-POSIX system (Windows) - permissions handled by OS
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to set directory permissions", e);
		}
	}
}
