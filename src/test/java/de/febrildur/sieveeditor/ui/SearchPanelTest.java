package de.febrildur.sieveeditor.ui;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.border.TitledBorder;

import java.awt.*;

import static org.assertj.core.api.Assertions.*;

class SearchPanelTest {

    private SearchPanel searchPanel;

    @BeforeEach
    void setUp() {
        searchPanel = new SearchPanel();
    }

    @Test
    void shouldCreateSearchPanelWithBorder() {
        assertThat(searchPanel.getBorder()).isNotNull();
        assertThat(searchPanel.getBorder()).isInstanceOf(TitledBorder.class);
        TitledBorder border = (TitledBorder) searchPanel.getBorder();
        assertThat(border.getTitle()).isEqualTo("Find & Replace");
    }

    @Test
    void shouldUseBorderLayout() {
        assertThat(searchPanel.getLayout()).isInstanceOf(BorderLayout.class);
    }

    @Test
    void shouldSetTargetEditor() {
        RSyntaxTextArea editor = new RSyntaxTextArea();
        searchPanel.setTargetEditor(editor);
    }

    @Test
    void shouldNotThrowWhenSetTargetEditorWithValidEditor() {
        RSyntaxTextArea editor = new RSyntaxTextArea("test");
        searchPanel.setTargetEditor(editor);
    }

    @Test
    void shouldFocusSearchField() {
        searchPanel.focusSearchField();
    }

    @Test
    void shouldHandleMultipleTargetChanges() {
        RSyntaxTextArea first = new RSyntaxTextArea();
        RSyntaxTextArea second = new RSyntaxTextArea();
        searchPanel.setTargetEditor(first);
        searchPanel.setTargetEditor(second);
    }

    @Test
    void shouldContainContentPanel() {
        Component contentPanel = ((BorderLayout) searchPanel.getLayout())
                .getLayoutComponent(BorderLayout.NORTH);
        assertThat(contentPanel).isNotNull();
        assertThat(contentPanel).isInstanceOf(Container.class);
    }
}
