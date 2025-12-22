package de.febrildur.sieveeditor.actions;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.fluffypeople.managesieve.ParseException;

import de.febrildur.sieveeditor.Application;
import de.febrildur.sieveeditor.system.ConnectAndListScripts;
import de.febrildur.sieveeditor.system.PropertiesSieve;

public class ActionConnect extends AbstractAction {

	private Application parentFrame;

	public ActionConnect(Application parentFrame) {
		putValue("Name", "Connect...");
		this.parentFrame = parentFrame;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// Run migration on first use
		PropertiesSieve.migrateOldProperties();

		final JDialog frame = new JDialog(parentFrame, "Connection", true);

		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		GridLayout layout = new GridLayout(6, 2, 6, 6); // Changed from 5 to 6 rows
		panel.setLayout(layout);

		frame.getContentPane().add(panel);

		// Profile selector (NEW)
		panel.add(new JLabel("Profile:"));
		JPanel profilePanel = new JPanel(new BorderLayout(6, 0));

		List<String> profiles = PropertiesSieve.getAvailableProfiles();
		String lastUsed = PropertiesSieve.getLastUsedProfile();
		JComboBox<String> profileCombo = new JComboBox<>(profiles.toArray(new String[0]));
		profileCombo.setSelectedItem(lastUsed);
		profilePanel.add(profileCombo, BorderLayout.CENTER);

		JButton newProfileButton = new JButton("+");
		newProfileButton.setToolTipText("Create new profile");
		newProfileButton.addActionListener(ev -> {
			String newName = JOptionPane.showInputDialog(frame,
				"Enter new profile name:", "New Profile", JOptionPane.PLAIN_MESSAGE);
			if (newName != null && !newName.trim().isEmpty()) {
				newName = newName.trim().replaceAll("[^a-zA-Z0-9_-]", "");
				if (!newName.isEmpty() && !PropertiesSieve.profileExists(newName)) {
					profileCombo.addItem(newName);
					profileCombo.setSelectedItem(newName);
				} else if (PropertiesSieve.profileExists(newName)) {
					JOptionPane.showMessageDialog(frame, "Profile already exists!");
				}
			}
		});

		JButton deleteProfileButton = new JButton("-");
		deleteProfileButton.setToolTipText("Delete profile");

		JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 3, 0));
		buttonPanel.add(newProfileButton);
		buttonPanel.add(deleteProfileButton);
		profilePanel.add(buttonPanel, BorderLayout.EAST);
		panel.add(profilePanel);

		// Load properties for selected profile
		PropertiesSieve properties = new PropertiesSieve(
			(String) profileCombo.getSelectedItem());
		try {
			properties.load();
		} catch (IOException ex) {
			// Ignore, empty properties
		}

		JLabel labelServer = new JLabel("Server");
		panel.add(labelServer);
		JTextField tfServer = new JTextField(properties.getServer(), 20);
		panel.add(tfServer);

		JLabel labelPort = new JLabel("Port");
		panel.add(labelPort);
		JTextField tfPort = new JTextField(Integer.toString(properties.getPort()), 20);
		panel.add(tfPort);

		JLabel labelUsername = new JLabel("User");
		panel.add(labelUsername);
		JTextField tfUsername = new JTextField(properties.getUsername(), 20);
		panel.add(tfUsername);

		JLabel labelPassword = new JLabel("Password");
		panel.add(labelPassword);
		JPasswordField tfPassword = new JPasswordField(properties.getPassword(), 20);
		tfPassword.setEchoChar('•');
		panel.add(tfPassword);

		// Track the currently displayed profile to save its data when switching
		final String[] currentDisplayedProfile = {(String) profileCombo.getSelectedItem()};

		// Add delete button action listener (needs currentDisplayedProfile to be declared first)
		deleteProfileButton.addActionListener(ev -> {
			String selectedProfile = (String) profileCombo.getSelectedItem();
			if (selectedProfile == null) {
				return;
			}

			int confirm = JOptionPane.showConfirmDialog(frame,
				"Are you sure you want to delete profile '" + selectedProfile + "'?",
				"Delete Profile",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);

			if (confirm == JOptionPane.YES_OPTION) {
				boolean deleted = PropertiesSieve.deleteProfile(selectedProfile);
				if (deleted) {
					profileCombo.removeItem(selectedProfile);
					// Select "default" profile if available, or the first profile
					if (profileCombo.getItemCount() > 0) {
						String newSelection;
						if (profiles.contains("default")) {
							newSelection = "default";
							profileCombo.setSelectedItem(newSelection);
						} else {
							profileCombo.setSelectedIndex(0);
							newSelection = (String) profileCombo.getSelectedItem();
						}
						// Update the tracking variable to the newly selected profile
						currentDisplayedProfile[0] = newSelection;
					}
				} else {
					JOptionPane.showMessageDialog(frame,
						"Failed to delete profile: " + selectedProfile,
						"Delete Error",
						JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		// When profile changes, save current data then reload new properties
		profileCombo.addActionListener(ev -> {
			if (ev.getSource() != profileCombo) return;

			String newProfile = (String) profileCombo.getSelectedItem();
			if (newProfile == null || newProfile.equals(currentDisplayedProfile[0])) {
				return;
			}

			// Save current form data to the previously displayed profile
			PropertiesSieve oldProps = new PropertiesSieve(currentDisplayedProfile[0]);
			oldProps.setServer(tfServer.getText());
			try {
				oldProps.setPort(Integer.valueOf(tfPort.getText()));
			} catch (NumberFormatException ex) {
				oldProps.setPort(4190);
			}
			oldProps.setUsername(tfUsername.getText());
			oldProps.setPassword(new String(tfPassword.getPassword()));
			oldProps.write();

			// Load and display new profile
			PropertiesSieve newProps = new PropertiesSieve(newProfile);
			try {
				newProps.load();
			} catch (IOException ex) {
				// Ignore, empty properties
			}

			// Update form fields
			tfServer.setText(newProps.getServer());
			tfPort.setText(String.valueOf(newProps.getPort()));
			tfUsername.setText(newProps.getUsername());
			tfPassword.setText(newProps.getPassword());

			// Update tracking variable
			currentDisplayedProfile[0] = newProfile;
		});

		JButton buttonOK = new JButton("OK");
		buttonOK.addActionListener((event) -> {
			String selectedProfile = (String) profileCombo.getSelectedItem();
			PropertiesSieve propsToSave = new PropertiesSieve(selectedProfile);

			ConnectAndListScripts server = new ConnectAndListScripts();
			server.setParentComponent(frame); // Enable interactive certificate validation
			parentFrame.setServer(server);
			try {
				propsToSave.setServer(tfServer.getText());
				propsToSave.setPort(Integer.valueOf(tfPort.getText()));
				propsToSave.setUsername(tfUsername.getText());
				propsToSave.setPassword(new String(tfPassword.getPassword()));
				parentFrame.getServer().connect(propsToSave);
				propsToSave.write();

				// Save last used profile
				PropertiesSieve.saveLastUsedProfile(selectedProfile);

				// Update parent frame's properties to the current profile
				parentFrame.setProp(propsToSave);

				parentFrame.updateStatus();
				frame.setVisible(false);

				// Auto-load script if there's only one available
				try {
					java.util.List<com.fluffypeople.managesieve.SieveScript> scripts =
						parentFrame.getServer().getListScripts();
					if (scripts != null && scripts.size() == 1) {
						parentFrame.setScript(scripts.get(0));
						parentFrame.updateStatus();
					}
				} catch (Exception autoLoadEx) {
					// Silently ignore auto-load failures - user can manually load
					System.err.println("Auto-load failed: " + autoLoadEx.getMessage());
				}
			} catch (NumberFormatException | IOException | ParseException e1) {
				JOptionPane.showMessageDialog(frame, e1.getClass().getName() + ": " + e1.getMessage());
			}
		});
		panel.add(buttonOK);

		// Size dialog to fit contents and center on parent
		frame.pack();
		frame.setLocationRelativeTo(parentFrame);
		frame.setVisible(true);
	}

}
