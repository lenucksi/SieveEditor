package de.febrildur.sieveeditor.actions;

import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.KeyStroke;

import java.awt.event.KeyEvent;

import static org.assertj.core.api.Assertions.*;

class ActionReplaceTest {

    @Test
    void shouldHaveCorrectPackage() {
        assertThat(ActionReplace.class.getPackageName())
                .isEqualTo("de.febrildur.sieveeditor.actions");
    }

    @Test
    void shouldSetNameOnConstruction() {
        var action = new ActionReplace(null);
        assertThat(action.getValue(Action.NAME))
                .isEqualTo("Find/Replace");
    }

    @Test
    void shouldSetAcceleratorKeyOnConstruction() {
        var action = new ActionReplace(null);
        assertThat(action.getValue(Action.ACCELERATOR_KEY))
                .isEqualTo(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
    }
}
