package de.febrildur.sieveeditor.actions;

import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.KeyStroke;

import java.awt.event.KeyEvent;

import static org.assertj.core.api.Assertions.*;

class ActionCheckScriptTest {

    @Test
    void shouldHaveCorrectPackage() {
        assertThat(ActionCheckScript.class.getPackageName())
                .isEqualTo("de.febrildur.sieveeditor.actions");
    }

    @Test
    void shouldSetNameOnConstruction() {
        var action = new ActionCheckScript(null);
        assertThat(action.getValue(Action.NAME))
                .isEqualTo("Check Script");
    }

    @Test
    void shouldSetAcceleratorKeyOnConstruction() {
        var action = new ActionCheckScript(null);
        assertThat(action.getValue(Action.ACCELERATOR_KEY))
                .isEqualTo(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK));
    }
}
