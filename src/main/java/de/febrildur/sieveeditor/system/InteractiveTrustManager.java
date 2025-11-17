package de.febrildur.sieveeditor.system;

import java.awt.Component;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.X509TrustManager;

import de.febrildur.sieveeditor.ui.CertificateDialog;

/**
 * Custom TrustManager that prompts the user to accept or reject unknown certificates.
 * This allows users to trust self-signed certificates while maintaining security
 * for certificates signed by known CAs.
 */
public class InteractiveTrustManager implements X509TrustManager {

	private static final Logger LOGGER = Logger.getLogger(InteractiveTrustManager.class.getName());

	private final X509TrustManager defaultTrustManager;
	private final CertificateStore certificateStore;
	private final String serverName;
	private final Component parentComponent;

	/**
	 * Creates an interactive trust manager.
	 *
	 * @param defaultTrustManager the system default trust manager for checking CA-signed certs
	 * @param serverName the server name (for display in dialogs)
	 * @param parentComponent parent component for showing dialogs
	 */
	public InteractiveTrustManager(X509TrustManager defaultTrustManager, String serverName, Component parentComponent) {
		this.defaultTrustManager = defaultTrustManager;
		this.certificateStore = new CertificateStore();
		this.serverName = serverName;
		this.parentComponent = parentComponent;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		// We don't validate client certificates in this application
		defaultTrustManager.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		if (chain == null || chain.length == 0) {
			throw new CertificateException("Certificate chain is empty");
		}

		X509Certificate serverCert = chain[0];

		// Check if certificate is explicitly rejected
		if (certificateStore.isRejected(serverCert)) {
			LOGGER.log(Level.WARNING, "Certificate for {0} was previously rejected by user", serverName);
			throw new CertificateException("Certificate was rejected by user");
		}

		// Check if certificate is explicitly trusted
		if (certificateStore.isTrusted(serverCert)) {
			LOGGER.log(Level.INFO, "Certificate for {0} was previously trusted by user", serverName);
			return; // Trusted by user, no further checks needed
		}

		// Try to validate with system CAs
		try {
			defaultTrustManager.checkServerTrusted(chain, authType);
			LOGGER.log(Level.INFO, "Certificate for {0} validated by system CA", serverName);
			return; // Validated by system CA
		} catch (CertificateException e) {
			// Certificate not trusted by system CAs
			LOGGER.log(Level.INFO, "Certificate for {0} not trusted by system CAs: {1}",
				new Object[]{serverName, e.getMessage()});

			// Ask user for decision
			CertificateDialog.UserDecision decision = askUserForDecision(serverCert);

			switch (decision) {
				case TRUST:
					certificateStore.trustCertificate(serverCert, serverName);
					LOGGER.log(Level.INFO, "User trusted certificate for {0}", serverName);
					return; // User accepted

				case REJECT:
					certificateStore.rejectCertificate(serverCert, serverName);
					LOGGER.log(Level.WARNING, "User rejected certificate for {0}", serverName);
					throw new CertificateException("Certificate rejected by user");

				case CANCEL:
				default:
					LOGGER.log(Level.INFO, "User cancelled certificate verification for {0}", serverName);
					throw new CertificateException("Certificate verification cancelled by user");
			}
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		// Return accepted issuers from system trust manager
		return defaultTrustManager.getAcceptedIssuers();
	}

	/**
	 * Asks the user whether to trust the certificate.
	 *
	 * @param cert the certificate to verify
	 * @return the user's decision
	 */
	private CertificateDialog.UserDecision askUserForDecision(X509Certificate cert) {
		// Show dialog on EDT if we're not already on it
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			return CertificateDialog.showCertificateDialog(parentComponent, cert, serverName);
		} else {
			final CertificateDialog.UserDecision[] decision = new CertificateDialog.UserDecision[1];
			try {
				javax.swing.SwingUtilities.invokeAndWait(() -> {
					decision[0] = CertificateDialog.showCertificateDialog(parentComponent, cert, serverName);
				});
				return decision[0];
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to show certificate dialog", e);
				return CertificateDialog.UserDecision.CANCEL;
			}
		}
	}
}
