package de.febrildur.sieveeditor.actions;

import java.util.List;

public class ConnectionDialogModel {

    private String server;
    private int port;
    private String username;
    private String password;
    private String selectedProfile;
    private List<String> availableProfiles;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSelectedProfile() {
        return selectedProfile;
    }

    public void setSelectedProfile(String selectedProfile) {
        this.selectedProfile = selectedProfile;
    }

    public List<String> getAvailableProfiles() {
        return availableProfiles;
    }

    public void setAvailableProfiles(List<String> availableProfiles) {
        this.availableProfiles = availableProfiles;
    }
}
