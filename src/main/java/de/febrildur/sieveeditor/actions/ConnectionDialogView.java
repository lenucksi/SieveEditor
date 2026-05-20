package de.febrildur.sieveeditor.actions;

import java.util.List;

public interface ConnectionDialogView {

    void show();

    void close();

    void showError(String message);

    ConnectionDialogModel getFieldValues();

    void setFieldValues(ConnectionDialogModel model);

    void refreshProfileList(List<String> profiles, String selected);

    String getSelectedProfile();

    void setSelectedProfile(String profile);
}
