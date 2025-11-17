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
		frame.setSize(350, 250);
		frame.setLocationRelativeTo(parentFrame);

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
		profilePanel.add(newProfileButton, BorderLayout.EAST);
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
		JTextField tfServer = new JTextField(properties.getServer(), 15);
		panel.add(tfServer);

		JLabel labelPort = new JLabel("Port");
		panel.add(labelPort);
		JTextField tfPort = new JTextField(Integer.toString(properties.getPort()), 15);
		panel.add(tfPort);

		JLabel labelUsername = new JLabel("User");
		panel.add(labelUsername);
		JTextField tfUsername = new JTextField(properties.getUsername(), 15);
		panel.add(tfUsername);

		JLabel labelPassword = new JLabel("Password");
		panel.add(labelPassword);
		JPasswordField tfPassword = new JPasswordField(properties.getPassword(), 15);
		tfPassword.setEchoChar('•');
		panel.add(tfPassword);

		// Track the currently displayed profile to save its data when switching
		final String[] currentDisplayedProfile = {(String) profileCombo.getSelectedItem()};

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
			} catch (NumberFormatException | IOException | ParseException e1) {
				JOptionPane.showMessageDialog(frame, e1.getClass().getName() + ": " + e1.getMessage());
			}
		});
		panel.add(buttonOK);


		frame.setVisible(true);
	}

}
