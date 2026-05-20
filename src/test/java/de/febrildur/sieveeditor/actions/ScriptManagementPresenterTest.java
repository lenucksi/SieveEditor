package de.febrildur.sieveeditor.actions;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fluffypeople.managesieve.ParseException;
import com.fluffypeople.managesieve.SieveScript;

import de.febrildur.sieveeditor.system.ConnectAndListScripts;

@ExtendWith(MockitoExtension.class)
class ScriptManagementPresenterTest {

    @Mock
    private ScriptManagementView view;

    @Mock
    private ConnectAndListScripts server;

    private ScriptManagementPresenter presenter;

    @BeforeEach
    void setUp() {
        presenter = new ScriptManagementPresenter(view, server);
    }

    @Test
    void shouldActivateScriptWhenValidSelection() throws Exception {
        when(view.getSelectedRow()).thenReturn(0);
        when(view.getScriptNameAt(0)).thenReturn("myscript");

        presenter.handleActivate();

        verify(server).activateScript("myscript");
        verify(view).refreshTable(any());
    }

    @Test
    void shouldShowErrorWhenNoSelectionOnActivate() throws Exception {
        when(view.getSelectedRow()).thenReturn(-1);

        presenter.handleActivate();

        verify(server, never()).activateScript(any());
        verify(view).showError("Please select a script first.");
    }

    @Test
    void shouldShowErrorWhenNegativeRowOnActivate() throws Exception {
        when(view.getSelectedRow()).thenReturn(-2);

        presenter.handleActivate();

        verify(server, never()).activateScript(any());
        verify(view).showError("Please select a script first.");
    }

    @Test
    void shouldDeactivateScript() throws Exception {
        presenter.handleDeactivate();

        verify(server).deactivateScript();
        verify(view).refreshTable(any());
    }

    @Test
    void shouldDeleteScriptWhenConfirmed() throws Exception {
        when(view.getSelectedRow()).thenReturn(0);
        when(view.getScriptNameAt(0)).thenReturn("myscript");
        when(view.showConfirm(anyString(), anyString())).thenReturn(true);

        presenter.handleDelete();

        verify(server).deleteScript("myscript");
        verify(view).refreshTable(any());
    }

    @Test
    void shouldNotDeleteScriptWhenNotConfirmed() throws Exception {
        when(view.getSelectedRow()).thenReturn(0);
        when(view.getScriptNameAt(0)).thenReturn("myscript");
        when(view.showConfirm(anyString(), anyString())).thenReturn(false);

        presenter.handleDelete();

        verify(server, never()).deleteScript(any());
    }

    @Test
    void shouldShowErrorOnDeleteWhenNoSelection() throws Exception {
        when(view.getSelectedRow()).thenReturn(-1);

        presenter.handleDelete();

        verify(server, never()).deleteScript(any());
        verify(view).showError("Please select a script first.");
    }

    @Test
    void shouldRenameScriptWithValidName() throws Exception {
        when(view.getSelectedRow()).thenReturn(0);
        when(view.getScriptNameAt(0)).thenReturn("oldname");

        presenter.handleRename("newname");

        verify(server).rename("oldname", "newname");
        verify(view).refreshTable(any());
    }

    @Test
    void shouldShowErrorWhenRenameWithEmptyName() throws Exception {
        when(view.getSelectedRow()).thenReturn(0);
        when(view.getScriptNameAt(0)).thenReturn("oldname");

        presenter.handleRename("");

        verify(server, never()).rename(any(), any());
        verify(view).showError("Script name cannot be empty.");
    }

    @Test
    void shouldShowErrorWhenRenameWithNullName() throws Exception {
        when(view.getSelectedRow()).thenReturn(0);
        when(view.getScriptNameAt(0)).thenReturn("oldname");

        presenter.handleRename(null);

        verify(server, never()).rename(any(), any());
        verify(view).showError("Script name cannot be empty.");
    }

    @Test
    void shouldDoNothingWhenNewNameEqualsOldName() throws Exception {
        when(view.getSelectedRow()).thenReturn(0);
        when(view.getScriptNameAt(0)).thenReturn("samename");

        presenter.handleRename("samename");

        verify(server, never()).rename(any(), any());
        verify(view, never()).showError(anyString());
    }

    @Test
    void shouldShowErrorWhenRenameWithNoSelection() throws Exception {
        when(view.getSelectedRow()).thenReturn(-1);

        presenter.handleRename("newname");

        verify(server, never()).rename(any(), any());
        verify(view).showError("Please select a script first.");
    }

    @Test
    void shouldRefreshScriptList() throws Exception {
        SieveScript script1 = new SieveScript("script1", null, true);
        SieveScript script2 = new SieveScript("script2", null, false);
        when(server.getListScripts()).thenReturn(List.of(script1, script2));

        presenter.refreshScriptList();

        verify(view).refreshTable(argThat(data ->
            data.length == 2
            && data[0][0].equals("script1") && data[0][1].equals("active")
            && data[1][0].equals("script2") && data[1][1].equals("")
        ));
    }

    @Test
    void shouldShowErrorOnIOExceptionDuringActivate() throws Exception {
        when(view.getSelectedRow()).thenReturn(0);
        when(view.getScriptNameAt(0)).thenReturn("myscript");
        doThrow(new IOException("connection failed")).when(server).activateScript("myscript");

        presenter.handleActivate();

        verify(view).showError("Error: connection failed");
        verify(view, never()).refreshTable(any());
    }

    @Test
    void shouldShowErrorOnParseExceptionDuringActivate() throws Exception {
        when(view.getSelectedRow()).thenReturn(0);
        when(view.getScriptNameAt(0)).thenReturn("myscript");
        doThrow(new ParseException("parse error")).when(server).activateScript("myscript");

        presenter.handleActivate();

        verify(view).showError("Error: parse error");
        verify(view, never()).refreshTable(any());
    }

    @Test
    void shouldShowErrorOnIOExceptionDuringDeactivate() throws Exception {
        doThrow(new IOException("deactivate failed")).when(server).deactivateScript();

        presenter.handleDeactivate();

        verify(view).showError("Error: deactivate failed");
    }

    @Test
    void shouldShowErrorOnIOExceptionDuringDelete() throws Exception {
        when(view.getSelectedRow()).thenReturn(0);
        when(view.getScriptNameAt(0)).thenReturn("myscript");
        when(view.showConfirm(anyString(), anyString())).thenReturn(true);
        doThrow(new IOException("delete failed")).when(server).deleteScript("myscript");

        presenter.handleDelete();

        verify(view).showError("Error: delete failed");
    }

    @Test
    void shouldShowErrorOnIOExceptionDuringRename() throws Exception {
        when(view.getSelectedRow()).thenReturn(0);
        when(view.getScriptNameAt(0)).thenReturn("oldname");
        doThrow(new IOException("rename failed")).when(server).rename("oldname", "newname");

        presenter.handleRename("newname");

        verify(view).showError("Error: rename failed");
    }

    @Test
    void shouldShowErrorOnIOExceptionDuringRefresh() throws Exception {
        doThrow(new IOException("list failed")).when(server).getListScripts();

        presenter.refreshScriptList();

        verify(view).showError("Error: list failed");
        verify(view, never()).refreshTable(any());
    }
}
