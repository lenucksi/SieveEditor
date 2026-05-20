package de.febrildur.sieveeditor.actions;

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
