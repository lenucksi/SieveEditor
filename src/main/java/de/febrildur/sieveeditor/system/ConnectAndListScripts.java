package de.febrildur.sieveeditor.system;

import java.awt.Component;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.fluffypeople.managesieve.ManageSieveClient;
import com.fluffypeople.managesieve.ManageSieveResponse;
import com.fluffypeople.managesieve.ParseException;
import com.fluffypeople.managesieve.SieveScript;

public class ConnectAndListScripts {

	private ManageSieveClient client;
	private java.util.Timer keepAliveTimer;
	private static final long KEEP_ALIVE_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes
	private boolean keepAliveEnabled = true;
	private Component parentComponent;

	// Connection state tracking for auto-reconnect
	private String lastServer;
	private int lastPort;
	private String lastUsername;
	private String lastPassword;
	private boolean allowInteractiveCertValidation = true;
	private static final Logger LOGGER = Logger.getLogger(ConnectAndListScripts.class.getName());

	/**
	 * Sets the parent component for showing certificate dialogs.
	 *
	 * @param parent the parent component
	 */
	public void setParentComponent(Component parent) {
		this.parentComponent = parent;
	}

	public void connect(PropertiesSieve prop) throws IOException, ParseException {
		connect(prop.getServer(), prop.getPort(), prop.getUsername(), prop.getPassword());
	}

	public void connect(String server, int port, String username, String password) throws IOException, ParseException {
		connect(server, port, username, password, true);
	}

	/**
	 * Connects to a ManageSieve server with optional interactive certificate validation.
	 *
	 * @param server the server hostname
	 * @param port the server port
	 * @param username the username
	 * @param password the password
	 * @param allowInteractiveCertValidation if true, shows dialog for unknown certificates
	 * @throws IOException if connection fails
	 * @throws ParseException if protocol parsing fails
	 */
	public void connect(String server, int port, String username, String password,
			boolean allowInteractiveCertValidation) throws IOException, ParseException {
		// Store connection parameters for auto-reconnect
		this.lastServer = server;
		this.lastPort = port;
		this.lastUsername = username;
		this.lastPassword = password;
		this.allowInteractiveCertValidation = allowInteractiveCertValidation;

		client = new ManageSieveClient();
		ManageSieveResponse resp = client.connect(server, port);
		if (!resp.isOk()) {
			client = null;
			throw new IOException("Can't connect to server: " + resp.getMessage());
		}

		// Use interactive SSL factory that prompts user for unknown certificates
		SSLSocketFactory sslFactory;
		if (allowInteractiveCertValidation && parentComponent != null) {
			sslFactory = getInteractiveSSLSocketFactory(server, parentComponent);
		} else {
			// Fallback to strict validation without user interaction
			sslFactory = getSecureSSLSocketFactory(null);
		}

		resp = client.starttls(sslFactory, false);
		if (!resp.isOk()) {
			client = null;
			throw new IOException("Can't start SSL:" + resp.getMessage());
		}

		resp = client.authenticate(username, password);
		if (!resp.isOk()) {
			client = null;
			throw new IOException("Could not authenticate: " + resp.getMessage());
		}

		LOGGER.log(Level.INFO, "Successfully connected to ManageSieve server: {0}:{1}",
			new Object[]{server, port});

		// Start keep-alive timer to prevent connection timeout
		startKeepAlive();
	}

	public void putScript(String scriptName, String scriptBody) throws IOException, ParseException {
		ensureConnection();
		ManageSieveResponse resp = client.putscript(scriptName, scriptBody);
		if (!resp.isOk()) {
			throw new IOException("Can't upload script to server: " + resp.getMessage());
		}

		resp = client.setactive(scriptName);
		if (!resp.isOk()) {
			throw new IOException("Can't set script [" + scriptName + "] to active: " + resp.getMessage());
		}
	}

	public List<SieveScript> getListScripts() throws IOException, ParseException {
		ensureConnection();
		List<SieveScript> scripts = new ArrayList<>();
		ManageSieveResponse resp = client.listscripts(scripts);
		if (!resp.isOk()) {
			throw new IOException("Can't get script list from server.");
		}
		return scripts;
	}

	public void logout() throws IOException, ParseException {
		// Stop keep-alive timer before logout
		stopKeepAlive();

		ManageSieveResponse resp = client.logout();
		if (!resp.isOk()) {
			throw new IOException("Can't logout: " + resp.getMessage());
		}
		client = null;
		// Clear connection state to prevent auto-reconnect after explicit logout
		clearConnectionState();
		LOGGER.log(Level.INFO, "Logged out from ManageSieve server");
	}

	public String getScript(SieveScript ss) throws IOException, ParseException {
		ensureConnection();
		ManageSieveResponse resp = client.getScript(ss);
		if (!resp.isOk()) {
			throw new IOException("Could not get body of script [" + ss.getName() + "]: " + resp.getMessage());
		}
		return ss.getBody();
	}

	public String checkScript(String script) throws IOException, ParseException {
		ensureConnection();
		ManageSieveResponse resp = client.checkscript(script);
		return resp.getMessage();
	}

	public boolean isLoggedIn() {
		return client != null;
	}

	/**
	 * Ensures the connection is alive and attempts auto-reconnect if needed.
	 * This method checks if the client is connected and attempts to reconnect
	 * using stored credentials if the connection was lost.
	 *
	 * @throws IOException if reconnection fails
	 * @throws ParseException if protocol parsing fails during reconnect
	 */
	private void ensureConnection() throws IOException, ParseException {
		// If client is null, we never connected - cannot auto-reconnect
		if (client == null && lastServer == null) {
			throw new IOException("Not connected to server. Please connect first.");
		}

		// If client exists, check if it's still connected
		if (client != null && client.isConnected()) {
			// Connection seems alive, no action needed
			return;
		}

		// Connection lost - attempt auto-reconnect
		if (lastServer != null && lastUsername != null && lastPassword != null) {
			LOGGER.log(Level.WARNING, "Connection lost. Attempting auto-reconnect to {0}:{1}",
				new Object[]{lastServer, lastPort});

			try {
				connect(lastServer, lastPort, lastUsername, lastPassword, allowInteractiveCertValidation);

				// Notify user of successful reconnection
				if (parentComponent != null) {
					javax.swing.JOptionPane.showMessageDialog(
						parentComponent,
						"Connection was lost and has been automatically restored.",
						"Reconnected",
						javax.swing.JOptionPane.INFORMATION_MESSAGE
					);
				}

				LOGGER.log(Level.INFO, "Auto-reconnect successful");
			} catch (IOException | ParseException e) {
				LOGGER.log(Level.SEVERE, "Auto-reconnect failed", e);
				// Clear stored credentials to prevent repeated failed attempts
				clearConnectionState();
				throw new IOException("Connection lost and auto-reconnect failed: " + e.getMessage(), e);
			}
		} else {
			throw new IOException("Connection lost and cannot auto-reconnect (no stored credentials)");
		}
	}

	/**
	 * Clears stored connection state. Used when auto-reconnect fails
	 * or when explicitly disconnecting.
	 */
	private void clearConnectionState() {
		lastServer = null;
		lastPort = 0;
		lastUsername = null;
		lastPassword = null;
	}

	/**
	 * Starts the keep-alive timer that periodically sends NOOP commands
	 * to prevent connection timeout.
	 */
	private void startKeepAlive() {
		if (!keepAliveEnabled) {
			return;
		}

		stopKeepAlive(); // Stop any existing timer

		keepAliveTimer = new java.util.Timer("ManageSieve-KeepAlive", true);
		keepAliveTimer.scheduleAtFixedRate(new java.util.TimerTask() {
			@Override
			public void run() {
				try {
					if (client != null && client.isConnected()) {
						client.noop("keep-alive");
						LOGGER.log(Level.FINE, "Keep-alive NOOP sent successfully");
					}
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Keep-alive NOOP failed: {0}", e.getMessage());
					// Don't stop timer - ensureConnection() will handle reconnect on next operation
				}
			}
		}, KEEP_ALIVE_INTERVAL_MS, KEEP_ALIVE_INTERVAL_MS);

		LOGGER.log(Level.INFO, "Keep-alive timer started (interval: {0}ms)", KEEP_ALIVE_INTERVAL_MS);
	}

	/**
	 * Stops the keep-alive timer.
	 */
	private void stopKeepAlive() {
		if (keepAliveTimer != null) {
			keepAliveTimer.cancel();
			keepAliveTimer = null;
			LOGGER.log(Level.INFO, "Keep-alive timer stopped");
		}
	}

	/**
	 * Enables or disables the keep-alive mechanism.
	 *
	 * @param enabled true to enable keep-alive, false to disable
	 */
	public void setKeepAliveEnabled(boolean enabled) {
		this.keepAliveEnabled = enabled;
		if (!enabled) {
			stopKeepAlive();
		} else if (client != null && client.isConnected()) {
			startKeepAlive();
		}
	}

	/**
	 * Checks if keep-alive is currently enabled.
	 *
	 * @return true if keep-alive is enabled
	 */
	public boolean isKeepAliveEnabled() {
		return keepAliveEnabled;
	}

	/**
	 * Returns an SSLSocketFactory with proper certificate validation enabled.
	 * This method is deprecated - use {@link #getSecureSSLSocketFactory(String)} instead.
	 *
	 * @deprecated The name "insecure" was misleading. This method now provides
	 *             secure certificate validation. Use {@link #getSecureSSLSocketFactory(String)}
	 *             with null parameter for the same behavior.
	 * @return SSLSocketFactory with system CA certificate validation
	 * @throws RuntimeException if SSL initialization fails
	 */
	@Deprecated
	public static SSLSocketFactory getInsecureSSLFactory() {
		return getSecureSSLSocketFactory(null);
	}

	/**
	 * Returns an SSLSocketFactory that validates certificates using system CAs or,
	 * optionally, a specified custom certificate.
	 *
	 * <p>This method enables proper SSL/TLS certificate validation to prevent
	 * man-in-the-middle attacks. By default (when certificatePath is null), it uses
	 * the system's trusted CA certificates.
	 *
	 * @param certificatePath the path to a custom certificate to trust (e.g., self-signed),
	 *                        or null to use only system CA certificates
	 * @return SSLSocketFactory configured with certificate validation enabled
	 * @throws RuntimeException if SSL initialization fails (wraps underlying exceptions)
	 */
	public static SSLSocketFactory getSecureSSLSocketFactory(String certificatePath) {
		try {
			SSLContext sc = SSLContext.getInstance("TLSv1.3");
			TrustManagerFactory tmf;

			if (certificatePath != null) {
				// Load custom certificate into KeyStore
				Logger.getLogger(ConnectAndListScripts.class.getName())
					.log(Level.INFO, "Loading custom certificate from: {0}", certificatePath);

				KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
				keyStore.load(null, null);

				try (FileInputStream fis = new FileInputStream(certificatePath)) {
					X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
							.generateCertificate(fis);
					keyStore.setCertificateEntry("custom", certificate);
				}

				tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init(keyStore);
			} else {
				// Use default trust store (system CAs)
				tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init((KeyStore) null);
			}

			sc.init(null, tmf.getTrustManagers(), new SecureRandom());
			return sc.getSocketFactory();

		} catch (NoSuchAlgorithmException ex) {
			// TLSv1.3 not available, fall back to TLSv1.2
			Logger.getLogger(ConnectAndListScripts.class.getName())
				.log(Level.WARNING, "TLSv1.3 not available, falling back to TLSv1.2");
			try {
				SSLContext sc = SSLContext.getInstance("TLSv1.2");
				TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init((KeyStore) null);
				sc.init(null, tmf.getTrustManagers(), new SecureRandom());
				return sc.getSocketFactory();
			} catch (Exception fallbackEx) {
				Logger.getLogger(ConnectAndListScripts.class.getName())
					.log(Level.SEVERE, "Failed to initialize SSL with TLSv1.2 fallback", fallbackEx);
				throw new RuntimeException("SSL initialization failed: " + fallbackEx.getMessage(), fallbackEx);
			}

		} catch (KeyStoreException | CertificateException | IOException | KeyManagementException ex) {
			Logger.getLogger(ConnectAndListScripts.class.getName())
				.log(Level.SEVERE, "Failed to create secure SSL socket factory", ex);
			throw new RuntimeException("SSL initialization failed: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Returns an SSLSocketFactory with interactive certificate validation.
	 * When an unknown certificate is encountered, the user is prompted to accept or reject it.
	 *
	 * @param serverName the server name (for display in dialogs)
	 * @param parentComponent parent component for showing dialogs
	 * @return SSLSocketFactory with interactive certificate validation
	 * @throws RuntimeException if SSL initialization fails
	 */
	public static SSLSocketFactory getInteractiveSSLSocketFactory(String serverName, Component parentComponent) {
		try {
			// Get the default trust manager
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init((KeyStore) null);
			X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];

			// Wrap it with our interactive trust manager
			InteractiveTrustManager interactiveTrustManager =
				new InteractiveTrustManager(defaultTrustManager, serverName, parentComponent);

			// Create SSL context with TLS 1.3
			SSLContext sc = SSLContext.getInstance("TLSv1.3");
			sc.init(null, new X509TrustManager[]{interactiveTrustManager}, new SecureRandom());
			return sc.getSocketFactory();

		} catch (NoSuchAlgorithmException ex) {
			// TLSv1.3 not available, fall back to TLSv1.2
			Logger.getLogger(ConnectAndListScripts.class.getName())
				.log(Level.WARNING, "TLSv1.3 not available, falling back to TLSv1.2");
			try {
				TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init((KeyStore) null);
				X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];

				InteractiveTrustManager interactiveTrustManager =
					new InteractiveTrustManager(defaultTrustManager, serverName, parentComponent);

				SSLContext sc = SSLContext.getInstance("TLSv1.2");
				sc.init(null, new X509TrustManager[]{interactiveTrustManager}, new SecureRandom());
				return sc.getSocketFactory();
			} catch (Exception fallbackEx) {
				Logger.getLogger(ConnectAndListScripts.class.getName())
					.log(Level.SEVERE, "Failed to initialize SSL with TLSv1.2 fallback", fallbackEx);
				throw new RuntimeException("SSL initialization failed: " + fallbackEx.getMessage(), fallbackEx);
			}

		} catch (KeyStoreException | KeyManagementException ex) {
			Logger.getLogger(ConnectAndListScripts.class.getName())
				.log(Level.SEVERE, "Failed to create interactive SSL socket factory", ex);
			throw new RuntimeException("SSL initialization failed: " + ex.getMessage(), ex);
		}
	}

	public void activateScript(String script) throws IOException, ParseException {
		ensureConnection();
		ManageSieveResponse resp = client.setactive(script);
		if (!resp.isOk()) {
			throw new IOException(resp.getMessage());
		}
	}

	public void deactivateScript() throws IOException, ParseException {
		ensureConnection();
		ManageSieveResponse resp = client.setactive("");
		if (!resp.isOk()) {
			throw new IOException(resp.getMessage());
		}
	}

	public void rename(String script, String newName) throws IOException, ParseException {
		ensureConnection();
		ManageSieveResponse resp = client.renamescript(script, newName);
		if (!resp.isOk()) {
			throw new IOException(resp.getMessage());
		}
	}

	public void deleteScript(String scriptName) throws IOException, ParseException {
		ensureConnection();
		ManageSieveResponse resp = client.deletescript(scriptName);
		if (!resp.isOk()) {
			throw new IOException(resp.getMessage());
		}
	}
}
