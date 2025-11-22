package de.febrildur.sieveeditor.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import de.febrildur.sieveeditor.system.CertificateStore;

/**
 * Dialog for displaying certificate information and requesting user trust decision.
 */
public class CertificateDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	public enum UserDecision {
		TRUST,    // Accept and store certificate as trusted
		REJECT,   // Reject and store certificate as permanently rejected
		CANCEL    // Abort connection without storing decision
	}

	private UserDecision decision = UserDecision.CANCEL;

	public CertificateDialog(JFrame parent, X509Certificate cert, String serverName) {
		super(parent, "Unknown Certificate", true);
		initComponents(cert, serverName);
	}

	private void initComponents(X509Certificate cert, String serverName) {
		setLayout(new BorderLayout(10, 10));

		// Header panel with warning
		JPanel headerPanel = new JPanel(new BorderLayout(5, 5));
		headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JLabel warningIcon = new JLabel("⚠️");
		warningIcon.setFont(new Font("Dialog", Font.PLAIN, 48));
		headerPanel.add(warningIcon, BorderLayout.WEST);

		JTextArea warningText = new JTextArea(
			"The server \"" + serverName + "\" presented a certificate that could not be verified.\n\n" +
			"This could mean:\n" +
			"• The server uses a self-signed certificate\n" +
			"• The certificate is signed by an unknown authority\n" +
			"• Someone may be intercepting your connection (MITM attack)\n\n" +
			"Please verify the certificate details below before proceeding."
		);
		warningText.setEditable(false);
		warningText.setOpaque(false);
		warningText.setWrapStyleWord(true);
		warningText.setLineWrap(true);
		warningText.setFont(warningText.getFont().deriveFont(12f));
		headerPanel.add(warningText, BorderLayout.CENTER);

		add(headerPanel, BorderLayout.NORTH);

		// Certificate details panel
		JPanel detailsPanel = new JPanel(new GridBagLayout());
		detailsPanel.setBorder(BorderFactory.createTitledBorder("Certificate Information"));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.anchor = GridBagConstraints.WEST;

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		// Subject
		addDetailRow(detailsPanel, gbc, 0, "Subject:",
			cert.getSubjectX500Principal().getName());

		// Issuer
		addDetailRow(detailsPanel, gbc, 1, "Issued By:",
			cert.getIssuerX500Principal().getName());

		// Valid from
		addDetailRow(detailsPanel, gbc, 2, "Valid From:",
			dateFormat.format(cert.getNotBefore()));

		// Valid until
		addDetailRow(detailsPanel, gbc, 3, "Valid Until:",
			dateFormat.format(cert.getNotAfter()));

		// Serial number
		addDetailRow(detailsPanel, gbc, 4, "Serial Number:",
			cert.getSerialNumber().toString(16).toUpperCase());

		// SHA-256 Fingerprint
		gbc.gridy = 5;
		gbc.gridx = 0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		JLabel fpLabel = new JLabel("SHA-256 Fingerprint:");
		fpLabel.setFont(fpLabel.getFont().deriveFont(Font.BOLD));
		detailsPanel.add(fpLabel, gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		JTextArea fingerprintArea = new JTextArea(formatFingerprintMultiline(
			CertificateStore.getFormattedFingerprint(cert)));
		fingerprintArea.setEditable(false);
		fingerprintArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
		fingerprintArea.setRows(3);
		fingerprintArea.setLineWrap(true);
		fingerprintArea.setWrapStyleWord(false);
		fingerprintArea.setBackground(detailsPanel.getBackground());
		JScrollPane fpScroll = new JScrollPane(fingerprintArea);
		fpScroll.setBorder(BorderFactory.createEmptyBorder());
		detailsPanel.add(fpScroll, gbc);

		JScrollPane detailsScroll = new JScrollPane(detailsPanel);
		add(detailsScroll, BorderLayout.CENTER);

		// Button panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

		JButton trustButton = new JButton("Trust & Connect");
		trustButton.setToolTipText("Accept this certificate permanently and connect");
		trustButton.addActionListener(e -> {
			decision = UserDecision.TRUST;
			dispose();
		});

		JButton rejectButton = new JButton("Reject");
		rejectButton.setToolTipText("Reject this certificate permanently");
		rejectButton.addActionListener(e -> {
			decision = UserDecision.REJECT;
			dispose();
		});

		JButton cancelButton = new JButton("Cancel");
		cancelButton.setToolTipText("Cancel connection without saving decision");
		cancelButton.addActionListener(e -> {
			decision = UserDecision.CANCEL;
			dispose();
		});

		buttonPanel.add(trustButton);
		buttonPanel.add(rejectButton);
		buttonPanel.add(cancelButton);

		add(buttonPanel, BorderLayout.SOUTH);

		// Make Cancel button the default
		getRootPane().setDefaultButton(cancelButton);

		// Size dialog to fit contents and center on parent
		pack();
		// Set minimum size for readability
		if (getWidth() < 550) {
			setSize(550, getHeight());
		}
		if (getHeight() < 450) {
			setSize(getWidth(), 450);
		}
		setLocationRelativeTo(getParent());
	}

	private void addDetailRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value) {
		gbc.gridy = row;
		gbc.gridx = 0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;

		JLabel labelComponent = new JLabel(label);
		labelComponent.setFont(labelComponent.getFont().deriveFont(Font.BOLD));
		panel.add(labelComponent, gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;

		JTextArea valueArea = new JTextArea(value);
		valueArea.setEditable(false);
		valueArea.setLineWrap(true);
		valueArea.setWrapStyleWord(false);
		valueArea.setFont(valueArea.getFont().deriveFont(11f));
		valueArea.setOpaque(false);
		valueArea.setBorder(BorderFactory.createEmptyBorder());
		panel.add(valueArea, gbc);
	}

	private String formatFingerprintMultiline(String fingerprint) {
		// Break fingerprint into groups of 8 hex pairs per line
		StringBuilder sb = new StringBuilder();
		String[] parts = fingerprint.split(":");
		for (int i = 0; i < parts.length; i++) {
			sb.append(parts[i]);
			if (i < parts.length - 1) {
				sb.append(":");
				if ((i + 1) % 8 == 0) {
					sb.append("\n");
				}
			}
		}
		return sb.toString();
	}

	public UserDecision getDecision() {
		return decision;
	}

	/**
	 * Shows the certificate dialog and waits for user decision.
	 *
	 * @param parent parent frame
	 * @param cert certificate to display
	 * @param serverName server name
	 * @return user's decision
	 */
	public static UserDecision showCertificateDialog(Component parent, X509Certificate cert, String serverName) {
		// Find the parent frame
		JFrame frame = null;
		if (parent != null) {
			frame = (JFrame) SwingUtilities.getWindowAncestor(parent);
		}

		CertificateDialog dialog = new CertificateDialog(frame, cert, serverName);
		dialog.setVisible(true);
		return dialog.getDecision();
	}
}
