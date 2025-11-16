package de.febrildur.sieveeditor.system;

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

import com.fluffypeople.managesieve.ManageSieveClient;
import com.fluffypeople.managesieve.ManageSieveResponse;
import com.fluffypeople.managesieve.ParseException;
import com.fluffypeople.managesieve.SieveScript;

public class ConnectAndListScripts {

	private ManageSieveClient client;

	public void connect(PropertiesSieve prop) throws IOException, ParseException {
		connect(prop.getServer(), prop.getPort(), prop.getUsername(), prop.getPassword());
	}
	
	public void connect(String server, int port, String username, String password) throws IOException, ParseException {
		client = new ManageSieveClient();
		ManageSieveResponse resp = client.connect(server, port);
		if (!resp.isOk()) {
			client = null;
			throw new IOException("Can't connect to server: " + resp.getMessage());
		}

		// Use secure SSL factory with system CA certificates
		// To use a custom certificate, call: getSecureSSLSocketFactory("/path/to/cert.pem")
		resp = client.starttls(getSecureSSLSocketFactory(null), false);
		if (!resp.isOk()) {
			client = null;
			throw new IOException("Can't start SSL:" + resp.getMessage());
		}

		resp = client.authenticate(username, password);
		if (!resp.isOk()) {
			client = null;
			throw new IOException("Could not authenticate: " + resp.getMessage());
		}
	}

	public void putScript(String scriptName, String scriptBody) throws IOException, ParseException {
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
		List<SieveScript> scripts = new ArrayList<SieveScript>();
		ManageSieveResponse resp = client.listscripts(scripts);
		if (!resp.isOk()) {
			throw new IOException("Could not get list of scripts: " + resp.getMessage());
		}
		return scripts;
	}

	public void logout() throws IOException, ParseException {
		ManageSieveResponse resp = client.logout();
		if (!resp.isOk()) {
			throw new IOException("Can't logout: " + resp.getMessage());
		}
		client = null;
	}

	public String getScript(SieveScript ss) throws IOException, ParseException {
		ManageSieveResponse resp = client.getScript(ss);
		if (!resp.isOk()) {
			throw new IOException("Could not get body of script [" + ss.getName() + "]: " + resp.getMessage());
		}
		return ss.getBody();
	}
	
	public String checkScript(String script) throws IOException, ParseException {
		ManageSieveResponse resp = client.checkscript(script);
		return resp.getMessage();
	}
	
	public boolean isLoggedIn() {
		return client != null;
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

	public void activateScript(String script) throws IOException, ParseException {
		ManageSieveResponse resp = client.setactive(script);
		if (!resp.isOk()) {
			throw new IOException(resp.getMessage()); 
		}
	}
	
	public void deactivateScript() throws IOException, ParseException {
		ManageSieveResponse resp = client.setactive("");
		if (!resp.isOk()) {
			throw new IOException(resp.getMessage()); 
		}
	}

	public void rename(String script, String newName) throws IOException, ParseException {
		ManageSieveResponse resp = client.renamescript(script, newName);
		if (!resp.isOk()) {
			throw new IOException(resp.getMessage()); 
		}
	}
}
