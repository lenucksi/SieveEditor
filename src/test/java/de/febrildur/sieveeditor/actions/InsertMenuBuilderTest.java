package de.febrildur.sieveeditor.actions;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.febrildur.sieveeditor.Application;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class InsertMenuBuilderTest {

    @Mock
    private Application application;

    private InsertMenuBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new InsertMenuBuilder(application);
    }

    @Test
    void shouldCreateInsertMenuWithBuiltinTemplates() {
        JMenu menu = builder.createInsertMenu();
        assertThat(menu.getText()).isEqualTo("Insert");
        assertThat(menu.getItemCount()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void shouldHaveBuiltinTemplatesSubMenu() {
        JMenu menu = builder.createInsertMenu();
        JMenuItem firstItem = menu.getItem(0);
        assertThat(firstItem).isInstanceOf(JMenu.class);
        JMenu builtinMenu = (JMenu) firstItem;
        assertThat(builtinMenu.getText()).isEqualTo("Built-in Templates");
        assertThat(builtinMenu.getItemCount()).isGreaterThan(0);
    }

    @Test
    void shouldHaveOpenTemplatesFolderItem() {
        JMenu menu = builder.createInsertMenu();
        JMenuItem lastItem = menu.getItem(menu.getItemCount() - 1);
        assertThat(lastItem).isInstanceOf(JMenuItem.class);
        assertThat(lastItem.getText()).isEqualTo("Open Templates Folder...");
    }

    @Test
    void shouldReturnTemplateService() {
        assertThat(builder.getTemplateService()).isNotNull();
    }
}
