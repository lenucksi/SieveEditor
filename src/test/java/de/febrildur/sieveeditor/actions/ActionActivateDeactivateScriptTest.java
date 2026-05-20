package de.febrildur.sieveeditor.actions;
// SPDX-FileCopyrightText: 2025 Claude
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ActionActivateDeactivateScriptTest {

    @Test
    void shouldExist() {
        assertThat(ActionActivateDeactivateScript.class).isNotNull();
    }

    @Test
    void shouldHaveCorrectName() {
        ActionActivateDeactivateScript action = new ActionActivateDeactivateScript(null);
        assertThat(action.getValue(javax.swing.Action.NAME)).isEqualTo("Manage Scripts...");
    }

    @Test
    void shouldHaveAcceleratorKey() {
        ActionActivateDeactivateScript action = new ActionActivateDeactivateScript(null);
        assertThat(action.getValue(javax.swing.Action.ACCELERATOR_KEY)).isNotNull();
    }

    @Test
    void shouldHaveScriptManagementViewInterface() {
        assertThat(ScriptManagementView.class).isNotNull();
        assertThat(ScriptManagementView.class.isInterface()).isTrue();
    }

    @Test
    void shouldHaveScriptManagementPresenterClass() {
        assertThat(ScriptManagementPresenter.class).isNotNull();
    }
}
