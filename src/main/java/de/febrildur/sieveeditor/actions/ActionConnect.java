package de.febrildur.sieveeditor.actions;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
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

import de.febrildur.sieveeditor.Application;
import de.febrildur.sieveeditor.system.PropertiesSieve;

public class ActionConnect extends AbstractAction {

    private Application parentFrame;

    public ActionConnect(Application parentFrame) {
        putValue(NAME, "Connect...");
        putValue(ACCELERATOR_KEY, javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_C,
                java.awt.event.KeyEvent.CTRL_DOWN_MASK | java.awt.event.KeyEvent.SHIFT_DOWN_MASK));
        this.parentFrame = parentFrame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PropertiesSieve.migrateOldProperties();
        SwingConnectionDialog dialog = new SwingConnectionDialog(parentFrame);
        ConnectionDialogPresenter presenter = new ConnectionDialogPresenter(dialog, parentFrame);
        dialog.setPresenter(presenter);
        presenter.init();
        dialog.setVisible(true);
    }

    private static class SwingConnectionDialog extends JDialog implements ConnectionDialogView {

        private final JComboBox<String> profileCombo = new JComboBox<>();
        private final JTextField tfServer = new JTextField(20);
        private final JTextField tfPort = new JTextField(20);
        private final JTextField tfUsername = new JTextField(20);
        private final JPasswordField tfPassword = new JPasswordField(20);
        private final JButton buttonOK = new JButton("OK");
        private final JButton btnNew = new JButton("+");
        private final JButton btnDel = new JButton("-");
        private final JButton btnRen = new JButton("\u270E");
        private ConnectionDialogPresenter presenter;

        SwingConnectionDialog(Application parent) {
            super(parent, "Connection", true);
            tfPassword.setEchoChar('\u2022');

            JPanel panel = new JPanel(new GridLayout(6, 2, 6, 6));
            panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            getContentPane().add(panel);

            panel.add(new JLabel("Profile:"));
            JPanel profilePanel = new JPanel(new BorderLayout(6, 0));
            profilePanel.add(profileCombo, BorderLayout.CENTER);
            JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 3, 0));
            buttonPanel.add(btnNew);
            buttonPanel.add(btnRen);
            buttonPanel.add(btnDel);
            btnNew.setToolTipText("Create new profile");
            btnDel.setToolTipText("Delete profile");
            btnRen.setToolTipText("Rename profile");
            profilePanel.add(buttonPanel, BorderLayout.EAST);
            panel.add(profilePanel);

            panel.add(new JLabel("Server"));
            panel.add(tfServer);
            panel.add(new JLabel("Port"));
            panel.add(tfPort);
            panel.add(new JLabel("User"));
            panel.add(tfUsername);
            panel.add(new JLabel("Password"));
            panel.add(tfPassword);

            JPanel okPanel = new JPanel();
            okPanel.add(buttonOK);
            panel.add(new JLabel());
            panel.add(okPanel);

            profileCombo.addActionListener(ev -> {
                if (ev.getSource() == profileCombo && presenter != null) {
                    presenter.handleProfileChange(getSelectedProfile());
                }
            });

            btnNew.addActionListener(ev -> {
                String name = JOptionPane.showInputDialog(this,
                        "Enter new profile name:", "New Profile", JOptionPane.PLAIN_MESSAGE);
                if (name != null && !name.trim().isEmpty()) {
                    if (presenter != null) {
                        presenter.handleNewProfile(name.trim());
                    }
                }
            });

            btnDel.addActionListener(ev -> {
                String selected = getSelectedProfile();
                if (selected != null && presenter != null) {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "Are you sure you want to delete profile '" + selected + "'?",
                            "Delete Profile", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirm == JOptionPane.YES_OPTION) {
                        presenter.handleDeleteProfile(selected);
                    }
                }
            });

            btnRen.addActionListener(ev -> {
                String selected = getSelectedProfile();
                if (selected != null && presenter != null) {
                    String newName = JOptionPane.showInputDialog(this,
                            "Enter new name for profile '" + selected + "':",
                            "Rename Profile", JOptionPane.PLAIN_MESSAGE);
                    if (newName != null && !newName.trim().isEmpty()) {
                        presenter.handleRenameProfile(selected, newName.trim());
                    }
                }
            });

            buttonOK.addActionListener(ev -> {
                if (presenter != null) {
                    presenter.handleOk();
                }
            });

            pack();
            setLocationRelativeTo(parent);
        }

        void setPresenter(ConnectionDialogPresenter presenter) {
            this.presenter = presenter;
        }

        @Override
        public void show() {
            setVisible(true);
        }

        @Override
        public void close() {
            setVisible(false);
        }

        @Override
        public void showError(String message) {
            JOptionPane.showMessageDialog(this, message);
        }

        @Override
        public ConnectionDialogModel getFieldValues() {
            ConnectionDialogModel model = new ConnectionDialogModel();
            model.setServer(tfServer.getText());
            try {
                model.setPort(Integer.parseInt(tfPort.getText()));
            } catch (NumberFormatException e) {
                model.setPort(4190);
            }
            model.setUsername(tfUsername.getText());
            model.setPassword(new String(tfPassword.getPassword()));
            model.setSelectedProfile(getSelectedProfile());
            return model;
        }

        @Override
        public void setFieldValues(ConnectionDialogModel model) {
            tfServer.setText(model.getServer());
            tfPort.setText(String.valueOf(model.getPort()));
            tfUsername.setText(model.getUsername());
            tfPassword.setText(model.getPassword());
        }

        @Override
        public void refreshProfileList(List<String> profiles, String selected) {
            profileCombo.removeAllItems();
            for (String p : profiles) {
                profileCombo.addItem(p);
            }
            if (selected != null) {
                profileCombo.setSelectedItem(selected);
            }
        }

        @Override
        public String getSelectedProfile() {
            return (String) profileCombo.getSelectedItem();
        }

        @Override
        public void setSelectedProfile(String profile) {
            profileCombo.setSelectedItem(profile);
        }
    }
}
