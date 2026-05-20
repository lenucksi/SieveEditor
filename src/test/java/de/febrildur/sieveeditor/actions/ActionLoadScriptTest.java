package de.febrildur.sieveeditor.actions;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.junit.jupiter.api.Test;

import javax.swing.Action;

import static org.assertj.core.api.Assertions.*;

class ActionLoadScriptTest {

    @Test
    void shouldHaveCorrectPackage() {
        assertThat(ActionLoadScript.class.getPackageName())
                .isEqualTo("de.febrildur.sieveeditor.actions");
    }

    @Test
    void shouldSetNameOnConstruction() {
        var action = new ActionLoadScript(null);
        assertThat(action.getValue(Action.NAME))
                .isEqualTo("Load...");
    }
}
