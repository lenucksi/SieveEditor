package de.febrildur.sieveeditor.system;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.io.FileInputStream;
import javax.net.ssl.TrustManagerFactory;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

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

		// resp = client.starttls();
		resp = client.starttls(getInsecureSSLFactory(), false);
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
	 * Returns an SSLSocketFactory that trusts CA certificates from system or,
	 * optionally, a specified self-signed certificate.
	 * 
	 * @param certificatePath the path to a specific certificate to trust, or null to use system CAs
	 * @return SSLSocketFactory configured with trusted certificates, or null if something fails
	 */
	public static SSLSocketFactory getSecureSSLSocketFactory(String certificatePath) {
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			TrustManagerFactory tmf;
			if (certificatePath != null) {
				// Load custom certificate into KeyStore
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
		} catch (Exception ex) {
			return null;
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
