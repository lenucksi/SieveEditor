package de.febrildur.sieveeditor.actions;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import java.io.IOException;
import java.util.List;

import com.fluffypeople.managesieve.ParseException;
import de.febrildur.sieveeditor.Application;
import de.febrildur.sieveeditor.system.ConnectAndListScripts;
import de.febrildur.sieveeditor.system.PropertiesSieve;

public class ConnectionDialogPresenter {

    private final ConnectionDialogView view;
    private final Application application;
    private String currentDisplayedProfile;

    public ConnectionDialogPresenter(ConnectionDialogView view, Application application) {
        this.view = view;
        this.application = application;
        this.currentDisplayedProfile = null;
    }

    public void init() {
        List<String> profiles = PropertiesSieve.getAvailableProfiles();
        String lastUsed = PropertiesSieve.getLastUsedProfile();
        if (!profiles.contains(lastUsed)) {
            lastUsed = profiles.get(0);
        }
        currentDisplayedProfile = lastUsed;
        view.refreshProfileList(profiles, currentDisplayedProfile);

        if (currentDisplayedProfile != null) {
            PropertiesSieve props = new PropertiesSieve(currentDisplayedProfile);
            try {
                props.load();
            } catch (IOException e) {
                // empty
            }
            ConnectionDialogModel model = new ConnectionDialogModel();
            model.setServer(props.getServer());
            model.setPort(props.getPort());
            model.setUsername(props.getUsername());
            model.setPassword(props.getPassword());
            view.setFieldValues(model);
        }
    }

    public void handleOk() {
        String selectedProfile = view.getSelectedProfile();
        ConnectionDialogModel model = view.getFieldValues();

        if (model.getPort() <= 0 || model.getPort() > 65535) {
            view.showError("Invalid port number. Please enter a valid port (1-65535).");
            return;
        }

        PropertiesSieve propsToSave = new PropertiesSieve(selectedProfile);
        ConnectAndListScripts server = createConnectAndListScripts();
        server.setParentComponent(application);
        application.setServer(server);
        try {
            propsToSave.setServer(model.getServer());
            propsToSave.setPort(model.getPort());
            propsToSave.setUsername(model.getUsername());
            propsToSave.setPassword(model.getPassword());
            application.getServer().connect(propsToSave);
            propsToSave.write();
            PropertiesSieve.saveLastUsedProfile(selectedProfile);
            application.setProp(propsToSave);
            application.updateStatus();
            view.close();

            try {
                List<com.fluffypeople.managesieve.SieveScript> scripts =
                        application.getServer().getListScripts();
                if (scripts != null && scripts.size() == 1) {
                    application.setScript(scripts.get(0));
                    application.updateStatus();
                }
            } catch (Exception autoLoadEx) {
                System.err.println("Auto-load failed: " + autoLoadEx.getMessage());
            }
        } catch (IOException | ParseException e) {
            view.showError(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public void handleProfileChange(String newProfile) {
        if (newProfile == null || newProfile.equals(currentDisplayedProfile)) {
            return;
        }

        ConnectionDialogModel currentModel = view.getFieldValues();
        PropertiesSieve oldProps = new PropertiesSieve(currentDisplayedProfile);
        oldProps.setServer(currentModel.getServer());
        oldProps.setPort(currentModel.getPort());
        oldProps.setUsername(currentModel.getUsername());
        oldProps.setPassword(currentModel.getPassword());
        oldProps.write();

        PropertiesSieve newProps = new PropertiesSieve(newProfile);
        try {
            newProps.load();
        } catch (IOException e) {
            // empty
        }

        ConnectionDialogModel newModel = new ConnectionDialogModel();
        newModel.setServer(newProps.getServer());
        newModel.setPort(newProps.getPort());
        newModel.setUsername(newProps.getUsername());
        newModel.setPassword(newProps.getPassword());
        view.setFieldValues(newModel);

        currentDisplayedProfile = newProfile;
    }

    public void handleNewProfile(String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        String sanitized = name.trim().replaceAll("[^a-zA-Z0-9_-]", "");
        if (sanitized.isEmpty()) {
            return;
        }

        if (PropertiesSieve.profileExists(sanitized)) {
            view.showError("Profile '" + sanitized + "' already exists!");
            return;
        }

        PropertiesSieve profile = new PropertiesSieve(sanitized);
        profile.write();

        List<String> profiles = PropertiesSieve.getAvailableProfiles();
        currentDisplayedProfile = sanitized;
        view.refreshProfileList(profiles, sanitized);
        ConnectionDialogModel model = new ConnectionDialogModel();
        model.setPort(4190);
        view.setFieldValues(model);
    }

    public void handleDeleteProfile(String profile) {
        if (profile == null) {
            return;
        }

        boolean deleted = PropertiesSieve.deleteProfile(profile);
        if (deleted) {
            List<String> profiles = PropertiesSieve.getAvailableProfiles();
            String newSelection = profiles.contains("default") ? "default" : profiles.get(0);
            currentDisplayedProfile = newSelection;
            view.refreshProfileList(profiles, newSelection);

            PropertiesSieve newProps = new PropertiesSieve(newSelection);
            try {
                newProps.load();
            } catch (IOException e) {
                // empty
            }
            ConnectionDialogModel model = new ConnectionDialogModel();
            model.setServer(newProps.getServer());
            model.setPort(newProps.getPort());
            model.setUsername(newProps.getUsername());
            model.setPassword(newProps.getPassword());
            view.setFieldValues(model);
        } else {
            view.showError("Failed to delete profile: " + profile);
        }
    }

    public void handleRenameProfile(String oldName, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return;
        }
        String sanitized = newName.trim().replaceAll("[^a-zA-Z0-9_-]", "");
        if (sanitized.isEmpty()) {
            view.showError("Profile name cannot be empty after removing invalid characters.");
            return;
        }

        if (sanitized.equals(oldName)) {
            return;
        }

        if (PropertiesSieve.profileExists(sanitized)) {
            view.showError("A profile with the name '" + sanitized + "' already exists!");
            return;
        }

        boolean renamed = PropertiesSieve.renameProfile(oldName, sanitized);
        if (renamed) {
            List<String> profiles = PropertiesSieve.getAvailableProfiles();
            currentDisplayedProfile = sanitized;
            view.refreshProfileList(profiles, sanitized);
        } else {
            view.showError("Failed to rename profile. See logs for details.");
        }
    }

    protected ConnectAndListScripts createConnectAndListScripts() {
        return new ConnectAndListScripts();
    }
}
