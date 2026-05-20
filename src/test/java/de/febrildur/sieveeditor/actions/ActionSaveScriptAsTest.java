package de.febrildur.sieveeditor.actions;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.KeyStroke;

import java.awt.event.KeyEvent;

import static org.assertj.core.api.Assertions.*;

class ActionSaveScriptAsTest {

    @Test
    void shouldHaveCorrectPackage() {
        assertThat(ActionSaveScriptAs.class.getPackageName())
                .isEqualTo("de.febrildur.sieveeditor.actions");
    }

    @Test
    void shouldSetNameOnConstruction() {
        var action = new ActionSaveScriptAs(null);
        assertThat(action.getValue(Action.NAME))
                .isEqualTo("Save as...");
    }

    @Test
    void shouldSetAcceleratorKeyOnConstruction() {
        var action = new ActionSaveScriptAs(null);
        assertThat(action.getValue(Action.ACCELERATOR_KEY))
                .isEqualTo(KeyStroke.getKeyStroke(
                        KeyEvent.VK_S,
                        KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
    }
}
