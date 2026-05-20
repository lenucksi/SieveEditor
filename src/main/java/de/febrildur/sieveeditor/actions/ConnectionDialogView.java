package de.febrildur.sieveeditor.actions;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

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
