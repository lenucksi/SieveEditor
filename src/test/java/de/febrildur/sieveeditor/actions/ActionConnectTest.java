package de.febrildur.sieveeditor.actions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

import static org.assertj.core.api.Assertions.assertThat;

class ActionConnectTest {

    private ActionConnect action;

    @BeforeEach
    void setUp() {
        action = new ActionConnect(null);
    }

    @Test
    void shouldHaveNameConnect() {
        assertThat(action.getValue(AbstractAction.NAME))
            .isEqualTo("Connect...");
    }

    @Test
    void shouldHaveAcceleratorKey() {
        assertThat(action.getValue(AbstractAction.ACCELERATOR_KEY))
            .isNotNull();
    }

    @Test
    void shouldHaveCorrectAcceleratorKeyCombination() {
        KeyStroke keyStroke = (KeyStroke) action.getValue(AbstractAction.ACCELERATOR_KEY);

        assertThat(keyStroke.getKeyCode()).isEqualTo(KeyEvent.VK_C);
        assertThat(keyStroke.getModifiers() & KeyEvent.CTRL_DOWN_MASK)
            .isEqualTo(KeyEvent.CTRL_DOWN_MASK);
        assertThat(keyStroke.getModifiers() & KeyEvent.SHIFT_DOWN_MASK)
            .isEqualTo(KeyEvent.SHIFT_DOWN_MASK);
    }
}
