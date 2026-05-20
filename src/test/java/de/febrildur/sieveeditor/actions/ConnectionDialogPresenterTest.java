package de.febrildur.sieveeditor.actions;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fluffypeople.managesieve.ParseException;
import com.fluffypeople.managesieve.SieveScript;

import de.febrildur.sieveeditor.Application;
import de.febrildur.sieveeditor.system.ConnectAndListScripts;
import de.febrildur.sieveeditor.system.PropertiesSieve;

@ExtendWith(MockitoExtension.class)
class ConnectionDialogPresenterTest {

    private static final String SIEVEEDITOR_TEST_DIR = "sieveeditor.test.dir";

    @TempDir
    Path tempDir;

    @Mock
    private ConnectionDialogView view;

    @Mock
    private Application application;

    @Captor
    private ArgumentCaptor<ConnectionDialogModel> modelCaptor;

    @Captor
    private ArgumentCaptor<PropertiesSieve> propsCaptor;

    private ConnectionDialogPresenter presenter;
    private String originalUserHome;
    private String originalTestDir;

    @BeforeEach
    void setUp() {
        originalUserHome = System.setProperty("user.home", tempDir.toAbsolutePath().toString());
        originalTestDir = System.setProperty(SIEVEEDITOR_TEST_DIR,
                tempDir.resolve("sieveeditor").toAbsolutePath().toString());

        // Create default profile so PropertiesSieve.getAvailableProfiles() works
        PropertiesSieve defaultProps = new PropertiesSieve("default");
        defaultProps.write();

        presenter = new ConnectionDialogPresenter(view, application);
    }

    @AfterEach
    void tearDown() {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
        if (originalTestDir != null) {
            System.setProperty(SIEVEEDITOR_TEST_DIR, originalTestDir);
        } else {
            System.clearProperty(SIEVEEDITOR_TEST_DIR);
        }
    }

    // ===== handleOk() Tests =====

    @Test
    void shouldHandleOkWithValidData() throws Exception {
        createProfile("testprofile", "sieve.example.com", 4190, "user", "pass");

        ConnectionDialogModel model = new ConnectionDialogModel();
        model.setServer("sieve.example.com");
        model.setPort(4190);
        model.setUsername("user");
        model.setPassword("pass");
        when(view.getFieldValues()).thenReturn(model);
        when(view.getSelectedProfile()).thenReturn("testprofile");

        ConnectAndListScripts mockServer = mock(ConnectAndListScripts.class);
        when(mockServer.getListScripts()).thenReturn(List.of());
        when(application.getServer()).thenReturn(mockServer);

        presenter.handleOk();

        verify(application).setServer(any(ConnectAndListScripts.class));
        verify(mockServer).connect(any(PropertiesSieve.class));
        verify(application).setProp(any(PropertiesSieve.class));
        verify(application).updateStatus();
        verify(view).close();
    }

    @Test
    void shouldSaveProfileDataOnHandleOk() throws Exception {
        createProfile("testprofile", "", 4190, "", "");

        ConnectionDialogModel model = new ConnectionDialogModel();
        model.setServer("newserver.com");
        model.setPort(2000);
        model.setUsername("newuser");
        model.setPassword("newpass");
        when(view.getFieldValues()).thenReturn(model);
        when(view.getSelectedProfile()).thenReturn("testprofile");

        ConnectAndListScripts mockServer = mock(ConnectAndListScripts.class);
        when(mockServer.getListScripts()).thenReturn(List.of());
        when(application.getServer()).thenReturn(mockServer);

        presenter.handleOk();

        PropertiesSieve saved = new PropertiesSieve("testprofile");
        saved.load();
        assertThat(saved.getServer()).isEqualTo("newserver.com");
        assertThat(saved.getPort()).isEqualTo(2000);
        assertThat(saved.getUsername()).isEqualTo("newuser");
        assertThat(saved.getPassword()).isEqualTo("newpass");
    }

    @Test
    void shouldSaveLastUsedProfileOnHandleOk() throws Exception {
        createProfile("testprofile", "", 4190, "", "");

        ConnectionDialogModel model = new ConnectionDialogModel();
        model.setServer("s.example.com");
        model.setPort(4190);
        model.setUsername("u");
        model.setPassword("p");
        when(view.getFieldValues()).thenReturn(model);
        when(view.getSelectedProfile()).thenReturn("testprofile");

        ConnectAndListScripts mockServer = mock(ConnectAndListScripts.class);
        when(mockServer.getListScripts()).thenReturn(List.of());
        when(application.getServer()).thenReturn(mockServer);

        presenter.handleOk();

        assertThat(PropertiesSieve.getLastUsedProfile()).isEqualTo("testprofile");
    }

    @Test
    void shouldShowErrorWhenPortIsInvalid() {
        when(view.getFieldValues()).thenReturn(new ConnectionDialogModel() {{
            setServer("s.example.com");
            setPort(0);
            setUsername("u");
            setPassword("p");
        }});
        when(view.getSelectedProfile()).thenReturn("test");

        presenter.handleOk();

        verify(view).showError(anyString());
        verify(view, never()).close();
    }

    @Test
    void shouldShowErrorWhenPortIsNegative() {
        when(view.getFieldValues()).thenReturn(new ConnectionDialogModel() {{
            setServer("s.example.com");
            setPort(-1);
            setUsername("u");
            setPassword("p");
        }});
        when(view.getSelectedProfile()).thenReturn("test");

        presenter.handleOk();

        verify(view).showError(anyString());
        verify(view, never()).close();
    }

    @Test
    void shouldShowErrorWhenPortExceedsMax() {
        when(view.getFieldValues()).thenReturn(new ConnectionDialogModel() {{
            setServer("s.example.com");
            setPort(65536);
            setUsername("u");
            setPassword("p");
        }});
        when(view.getSelectedProfile()).thenReturn("test");

        presenter.handleOk();

        verify(view).showError(anyString());
        verify(view, never()).close();
    }

    @Test
    void shouldShowErrorOnConnectionFailure() throws Exception {
        createProfile("testprofile", "", 4190, "", "");

        ConnectionDialogModel model = new ConnectionDialogModel();
        model.setServer("s.example.com");
        model.setPort(4190);
        model.setUsername("u");
        model.setPassword("p");
        when(view.getFieldValues()).thenReturn(model);
        when(view.getSelectedProfile()).thenReturn("testprofile");

        ConnectAndListScripts mockServer = mock(ConnectAndListScripts.class);
        doThrow(new IOException("connection refused"))
                .when(mockServer).connect(any(PropertiesSieve.class));
        when(application.getServer()).thenReturn(mockServer);

        presenter.handleOk();

        verify(view).showError(contains("connection refused"));
        verify(view, never()).close();
    }

    @Test
    void shouldAutoLoadSingleScript() throws Exception {
        createProfile("p", "", 4190, "", "");

        ConnectionDialogModel model = new ConnectionDialogModel();
        model.setServer("s.example.com");
        model.setPort(4190);
        model.setUsername("u");
        model.setPassword("p");
        when(view.getFieldValues()).thenReturn(model);
        when(view.getSelectedProfile()).thenReturn("p");

        SieveScript script = new SieveScript("myscript", null, true);
        ConnectAndListScripts mockServer = mock(ConnectAndListScripts.class);
        when(mockServer.getListScripts()).thenReturn(List.of(script));
        when(application.getServer()).thenReturn(mockServer);

        presenter.handleOk();

        verify(application).setScript(script);
        verify(application, times(2)).updateStatus();
    }

    @Test
    void shouldNotAutoLoadWhenMultipleScripts() throws Exception {
        createProfile("p", "", 4190, "", "");

        ConnectionDialogModel model = new ConnectionDialogModel();
        model.setServer("s.example.com");
        model.setPort(4190);
        model.setUsername("u");
        model.setPassword("p");
        when(view.getFieldValues()).thenReturn(model);
        when(view.getSelectedProfile()).thenReturn("p");

        SieveScript s1 = new SieveScript("s1", null, true);
        SieveScript s2 = new SieveScript("s2", null, false);
        ConnectAndListScripts mockServer = mock(ConnectAndListScripts.class);
        when(mockServer.getListScripts()).thenReturn(List.of(s1, s2));
        when(application.getServer()).thenReturn(mockServer);

        presenter.handleOk();

        verify(application, never()).setScript(any());
    }

    // ===== handleProfileChange() Tests =====

    @Test
    void shouldSaveCurrentProfileAndLoadNewOnProfileChange() {
        createProfile("profile1", "server1.com", 4190, "user1", "pass1");
        createProfile("profile2", "server2.com", 2000, "user2", "pass2");

        presenter.init();
        reset(view);

        ConnectionDialogModel currentModel = new ConnectionDialogModel();
        currentModel.setServer("modified1.com");
        currentModel.setPort(4190);
        currentModel.setUsername("mod1");
        currentModel.setPassword("modpass1");
        when(view.getFieldValues()).thenReturn(currentModel);

        presenter.handleProfileChange("profile2");

        ArgumentCaptor<ConnectionDialogModel> captor =
                ArgumentCaptor.forClass(ConnectionDialogModel.class);
        verify(view).setFieldValues(captor.capture());
        ConnectionDialogModel loaded = captor.getValue();
        assertThat(loaded.getServer()).isEqualTo("server2.com");
        assertThat(loaded.getPort()).isEqualTo(2000);
        assertThat(loaded.getUsername()).isEqualTo("user2");
        assertThat(loaded.getPassword()).isEqualTo("pass2");

        PropertiesSieve savedDefault = new PropertiesSieve("default");
        try {
            savedDefault.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertThat(savedDefault.getServer()).isEqualTo("modified1.com");
        assertThat(savedDefault.getUsername()).isEqualTo("mod1");
    }

    @Test
    void shouldIgnoreProfileChangeWhenProfileIsNull() {
        presenter.handleProfileChange(null);

        verify(view, never()).getFieldValues();
        verify(view, never()).setFieldValues(any());
    }

    @Test
    void shouldIgnoreProfileChangeWhenProfileIsSame() {
        presenter.init();
        reset(view);

        presenter.handleProfileChange("default");

        verify(view, never()).getFieldValues();
        verify(view, never()).setFieldValues(any());
    }

    // ===== handleNewProfile() Tests =====

    @Test
    void shouldCreateNewProfileWithValidName() {
        presenter.handleNewProfile("newprofile");

        assertThat(PropertiesSieve.profileExists("newprofile")).isTrue();
        verify(view).refreshProfileList(anyList(), eq("newprofile"));
    }

    @Test
    void shouldSanitizeNewProfileName() {
        presenter.handleNewProfile("invalid name!!!");

        assertThat(PropertiesSieve.profileExists("invalidname")).isTrue();
        assertThat(PropertiesSieve.profileExists("invalid name!!!")).isFalse();
    }

    @Test
    void shouldShowErrorWhenNewProfileNameIsDuplicate() {
        createProfile("existing", "", 4190, "", "");
        reset(view);

        presenter.handleNewProfile("existing");

        verify(view).showError(contains("already exists"));
        assertThat(PropertiesSieve.profileExists("existing")).isTrue();
    }

    @Test
    void shouldDoNothingWhenNewProfileNameIsEmpty() {
        presenter.handleNewProfile("");

        verify(view, never()).showError(anyString());
        verify(view, never()).refreshProfileList(anyList(), anyString());
    }

    @Test
    void shouldDoNothingWhenNewProfileNameIsNull() {
        presenter.handleNewProfile(null);

        verify(view, never()).showError(anyString());
        verify(view, never()).refreshProfileList(anyList(), anyString());
    }

    @Test
    void shouldDoNothingWhenSanitizedNewProfileNameIsEmpty() {
        presenter.handleNewProfile("!!!");

        verify(view, never()).showError(anyString());
        verify(view, never()).refreshProfileList(anyList(), anyString());
    }

    // ===== handleDeleteProfile() Tests =====

    @Test
    void shouldDeleteProfileAndSelectDefault() {
        createProfile("todelete", "", 4190, "", "");
        createProfile("default", "", 4190, "", "");
        reset(view);

        presenter.handleDeleteProfile("todelete");

        assertThat(PropertiesSieve.profileExists("todelete")).isFalse();
        verify(view).refreshProfileList(anyList(), eq("default"));
    }

    @Test
    void shouldDeleteProfileAndSelectFirstAvailable() {
        createProfile("todelete", "", 4190, "", "");
        PropertiesSieve.deleteProfile("default"); // remove default
        reset(view);

        List<String> profiles = PropertiesSieve.getAvailableProfiles();
        assertThat(profiles).containsExactly("todelete");

        presenter.handleDeleteProfile("todelete");

        assertThat(PropertiesSieve.profileExists("todelete")).isFalse();
    }

    @Test
    void shouldDoNothingWhenDeletingNullProfile() {
        presenter.handleDeleteProfile(null);

        verify(view, never()).showError(anyString());
        verify(view, never()).refreshProfileList(anyList(), anyString());
    }

    @Test
    void shouldShowErrorWhenDeleteFails() {
        presenter.init();
        reset(view);

        presenter.handleDeleteProfile("nonexistent");

        verify(view).showError(contains("Failed to delete"));
    }

    // ===== handleRenameProfile() Tests =====

    @Test
    void shouldRenameProfileWithValidName() {
        createProfile("oldname", "server.com", 4190, "u", "p");
        reset(view);

        presenter.handleRenameProfile("oldname", "newname");

        assertThat(PropertiesSieve.profileExists("oldname")).isFalse();
        assertThat(PropertiesSieve.profileExists("newname")).isTrue();

        PropertiesSieve renamed = new PropertiesSieve("newname");
        try {
            renamed.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertThat(renamed.getServer()).isEqualTo("server.com");

        verify(view).refreshProfileList(anyList(), eq("newname"));
    }

    @Test
    void shouldDoNothingWhenRenamingToSameName() {
        createProfile("samename", "", 4190, "", "");
        reset(view);

        presenter.handleRenameProfile("samename", "samename");

        assertThat(PropertiesSieve.profileExists("samename")).isTrue();
        verify(view, never()).showError(anyString());
        verify(view, never()).refreshProfileList(anyList(), anyString());
    }

    @Test
    void shouldShowErrorWhenRenamingToExistingName() {
        createProfile("name1", "", 4190, "", "");
        createProfile("name2", "", 4190, "", "");
        reset(view);

        presenter.handleRenameProfile("name1", "name2");

        verify(view).showError(contains("already exists"));
        assertThat(PropertiesSieve.profileExists("name1")).isTrue();
        assertThat(PropertiesSieve.profileExists("name2")).isTrue();
    }

    @Test
    void shouldShowErrorWhenRenamingWithEmptySanitizedName() {
        createProfile("old", "", 4190, "", "");
        reset(view);

        presenter.handleRenameProfile("old", "!!!");

        verify(view).showError(contains("cannot be empty"));
        assertThat(PropertiesSieve.profileExists("old")).isTrue();
    }

    @Test
    void shouldDoNothingWhenRenamingWithNullName() {
        createProfile("old", "", 4190, "", "");
        reset(view);

        presenter.handleRenameProfile("old", null);

        verify(view, never()).showError(anyString());
        verify(view, never()).refreshProfileList(anyList(), anyString());
    }

    @Test
    void shouldDoNothingWhenRenamingWithEmptyName() {
        createProfile("old", "", 4190, "", "");
        reset(view);

        presenter.handleRenameProfile("old", "");

        verify(view, never()).showError(anyString());
        verify(view, never()).refreshProfileList(anyList(), anyString());
    }

    @Test
    void shouldSanitizeRenamedProfileName() {
        createProfile("old", "server.com", 4190, "u", "p");
        reset(view);

        presenter.handleRenameProfile("old", "new name!!!");

        assertThat(PropertiesSieve.profileExists("old")).isFalse();
        assertThat(PropertiesSieve.profileExists("newname")).isTrue();
    }

    // ===== init() Tests =====

    @Test
    void shouldLoadProfilesAndLastUsedOnInit() {
        createProfile("work", "work.com", 4190, "w", "wp");
        PropertiesSieve.saveLastUsedProfile("work");

        presenter = new ConnectionDialogPresenter(view, application);
        presenter.init();

        verify(view).refreshProfileList(anyList(), eq("work"));
        verify(view).setFieldValues(any(ConnectionDialogModel.class));
    }

    @Test
    void shouldFallbackToFirstProfileWhenLastUsedMissing() {
        createProfile("alpha", "a.com", 4190, "a", "ap");
        PropertiesSieve.saveLastUsedProfile("nonexistent");

        presenter = new ConnectionDialogPresenter(view, application);
        presenter.init();

        verify(view).refreshProfileList(anyList(), eq("alpha"));
    }

    // ===== Helpers =====

    private void createProfile(String name, String server, int port, String user, String pass) {
        PropertiesSieve props = new PropertiesSieve(name);
        props.setServer(server);
        props.setPort(port);
        props.setUsername(user);
        props.setPassword(pass);
        props.write();
    }
}
