package de.febrildur.sieveeditor.system;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.ListCellRenderer;

import java.awt.Component;

import static org.assertj.core.api.Assertions.*;

class ToStringListCellRendererTest {

    @Test
    void shouldImplementListCellRenderer() {
        ListCellRenderer<?> original = new DefaultListCellRenderer();
        ToString toString = Object::toString;
        ToStringListCellRenderer renderer = new ToStringListCellRenderer(original, toString);
        assertThat(renderer).isInstanceOf(ListCellRenderer.class);
    }

    @Test
    void shouldRenderUsingToString() {
        DefaultListCellRenderer original = new DefaultListCellRenderer();
        ToString toString = obj -> "custom:" + obj;
        ToStringListCellRenderer renderer = new ToStringListCellRenderer(original, toString);

        JList<?> list = new JList<>(new String[]{"test"});
        Component result = renderer.getListCellRendererComponent(list, "hello", 0, false, false);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldDelegateToOriginalRenderer() {
        DefaultListCellRenderer original = new DefaultListCellRenderer();
        ToString toString = obj -> "delegated:" + obj;
        ToStringListCellRenderer renderer = new ToStringListCellRenderer(original, toString);

        JList<?> list = new JList<>(new String[]{"test"});
        Component result = renderer.getListCellRendererComponent(list, "value", 0, true, true);
        assertThat(result).isNotNull();
    }
}
