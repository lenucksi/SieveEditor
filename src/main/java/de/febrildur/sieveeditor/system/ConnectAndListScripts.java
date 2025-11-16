package de.febrildur.sieveeditor.system;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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

	public static SSLSocketFactory getInsecureSSLFactory() {
		try {
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}

				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} };

			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new SecureRandom());
			return sc.getSocketFactory();
		} catch (NoSuchAlgorithmException | KeyManagementException ex) {
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
