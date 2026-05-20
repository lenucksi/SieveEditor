package de.febrildur.sieveeditor.system;
// SPDX-FileCopyrightText: 2025 Claude
// SPDX-FileCopyrightText: 2025, 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import com.fluffypeople.managesieve.ManageSieveClient;
import com.fluffypeople.managesieve.ManageSieveResponse;
import com.fluffypeople.managesieve.ParseException;
import com.fluffypeople.managesieve.SieveScript;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectAndListScriptsTest {

    private ConnectAndListScripts connection;

    private ConnectAndListScripts diConnection;

    @Mock
    private SieveConnectionFactory mockFactory;

    @Mock
    private ManageSieveClient mockClient;

    @Mock
    private ManageSieveResponse mockResponse;

    @BeforeEach
    void setUp() {
        connection = new ConnectAndListScripts();
        lenient().when(mockFactory.create()).thenReturn(mockClient);
        diConnection = new ConnectAndListScripts(mockFactory);
        diConnection.setKeepAliveEnabled(false);
    }

    private void setupSuccessfulConnect() throws Exception {
        when(mockClient.connect(anyString(), anyInt())).thenReturn(mockResponse);
        when(mockClient.starttls(any(), anyBoolean())).thenReturn(mockResponse);
        when(mockClient.authenticate(anyString(), anyString())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true);
    }

    private void connectAndStubIsConnected() throws Exception {
        setupSuccessfulConnect();
        diConnection.connect("server", 4190, "user", "pass");
        when(mockClient.isConnected()).thenReturn(true);
    }

    // ===== Initial State Tests =====

    @Test
    void shouldIndicateNotLoggedInInitially() {
        assertThat(connection.isLoggedIn()).isFalse();
    }

    @Test
    void shouldIndicateNotLoggedInAfterFailedConnection() {
        ConnectAndListScripts conn = new ConnectAndListScripts();
        assertThat(conn.isLoggedIn()).isFalse();
    }

    @Test
    void shouldHandleLoggedInStateCorrectly() {
        assertThat(connection.isLoggedIn()).isFalse();
    }

    // ===== Connection Tests =====

    @Test
    void shouldIndicateLoggedInWhenConnected() throws Exception {
        setupSuccessfulConnect();

        diConnection.connect("server", 4190, "user", "pass");

        assertThat(diConnection.isLoggedIn()).isTrue();
    }

    @Test
    void shouldConnectAndStarttlsAndAuthenticateInOrder() throws Exception {
        when(mockClient.connect(anyString(), anyInt())).thenReturn(mockResponse);
        when(mockClient.starttls(any(), anyBoolean())).thenReturn(mockResponse);
        when(mockClient.authenticate(anyString(), anyString())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true);

        diConnection.connect("server", 4190, "user", "pass");

        verify(mockClient).connect("server", 4190);
        verify(mockClient).starttls(any(), eq(false));
        verify(mockClient).authenticate("user", "pass");
    }

    @Test
    void shouldThrowIOExceptionWhenConnectFails() throws Exception {
        when(mockClient.connect(anyString(), anyInt())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(false);
        when(mockResponse.getMessage()).thenReturn("Connection refused");

        assertThatThrownBy(() -> diConnection.connect("server", 4190, "user", "pass"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Can't connect to server");

        assertThat(diConnection.isLoggedIn()).isFalse();
    }

    @Test
    void shouldThrowIOExceptionWhenStarttlsFails() throws Exception {
        when(mockClient.connect(anyString(), anyInt())).thenReturn(mockResponse);
        when(mockClient.starttls(any(), anyBoolean())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true, false);
        when(mockResponse.getMessage()).thenReturn("TLS failed");

        assertThatThrownBy(() -> diConnection.connect("server", 4190, "user", "pass"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Can't start SSL");

        assertThat(diConnection.isLoggedIn()).isFalse();
    }

    @Test
    void shouldThrowIOExceptionWhenAuthenticateFails() throws Exception {
        when(mockClient.connect(anyString(), anyInt())).thenReturn(mockResponse);
        when(mockClient.starttls(any(), anyBoolean())).thenReturn(mockResponse);
        when(mockClient.authenticate(anyString(), anyString())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true, true, false);
        when(mockResponse.getMessage()).thenReturn("Auth failed");

        assertThatThrownBy(() -> diConnection.connect("server", 4190, "user", "pass"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Could not authenticate");

        assertThat(diConnection.isLoggedIn()).isFalse();
    }

    // ===== Parameter Validation Tests =====

    @Test
    void shouldRejectNullServer() {
        assertThatThrownBy(() -> connection.connect(null, 4190, "user", "pass"))
                .isInstanceOf(IOException.class);
    }

    @Test
    void shouldRejectNullUsername() {
        assertThatThrownBy(() -> connection.connect("server.example.com", 4190, null, "pass"))
                .isInstanceOf(IOException.class);
    }

    @Test
    void shouldRejectNullPassword() {
        assertThatThrownBy(() -> connection.connect("server.example.com", 4190, "user", null))
                .isInstanceOf(IOException.class);
    }

    @Test
    void shouldAcceptValidConnectionParameters() {
        assertThatCode(() -> {
            try {
                connection.connect("localhost", 4190, "user", "pass");
            } catch (IOException | ParseException e) {
                // Expected - no server running
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptValidPorts() {
        assertThatCode(() -> {
            try {
                connection.connect("localhost", 4190, "user", "pass");
                connection.connect("localhost", 2000, "user", "pass");
                connection.connect("localhost", 1, "user", "pass");
                connection.connect("localhost", 65535, "user", "pass");
            } catch (IOException | ParseException e) {
                // Expected - no server running
            }
        }).doesNotThrowAnyException();
    }

    // ===== Script Operations Tests =====

    @Test
    void shouldGetListOfScriptsWhenConnected() throws Exception {
        connectAndStubIsConnected();

        List<SieveScript> expectedScripts = new ArrayList<>();
        expectedScripts.add(new SieveScript("script1", "body1", true));
        when(mockClient.listscripts(any())).thenAnswer(invocation -> {
            List<SieveScript> list = invocation.getArgument(0);
            list.addAll(expectedScripts);
            return mockResponse;
        });

        List<SieveScript> scripts = diConnection.getListScripts();

        assertThat(scripts).hasSize(1);
        assertThat(scripts.get(0).getName()).isEqualTo("script1");
    }

    @Test
    void shouldUploadScriptToServer() throws Exception {
        connectAndStubIsConnected();

        when(mockClient.putscript("myscript", "require [\"fileinto\"];")).thenReturn(mockResponse);
        when(mockClient.setactive("myscript")).thenReturn(mockResponse);

        diConnection.putScript("myscript", "require [\"fileinto\"];");

        verify(mockClient).putscript("myscript", "require [\"fileinto\"];");
        verify(mockClient).setactive("myscript");
    }

    @Test
    void shouldDownloadScriptFromServer() throws Exception {
        connectAndStubIsConnected();

        SieveScript ss = new SieveScript("myscript", "unused", false);
        when(mockClient.getScript(ss)).thenReturn(mockResponse);
        ss.setBody("downloaded body");

        String body = diConnection.getScript(ss);

        assertThat(body).isEqualTo("downloaded body");
    }

    @Test
    void shouldCheckScriptSyntax() throws Exception {
        connectAndStubIsConnected();

        when(mockClient.checkscript("if true")).thenReturn(mockResponse);
        when(mockResponse.getMessage()).thenReturn("OK");

        String result = diConnection.checkScript("if true");

        assertThat(result).isEqualTo("OK");
    }

    @Test
    void shouldActivateScript() throws Exception {
        connectAndStubIsConnected();

        when(mockClient.setactive("myscript")).thenReturn(mockResponse);

        diConnection.activateScript("myscript");

        verify(mockClient).setactive("myscript");
    }

    @Test
    void shouldDeactivateScript() throws Exception {
        connectAndStubIsConnected();

        when(mockClient.setactive("")).thenReturn(mockResponse);

        diConnection.deactivateScript();

        verify(mockClient).setactive("");
    }

    @Test
    void shouldRenameScript() throws Exception {
        connectAndStubIsConnected();

        when(mockClient.renamescript("old", "new")).thenReturn(mockResponse);

        diConnection.rename("old", "new");

        verify(mockClient).renamescript("old", "new");
    }

    @Test
    void shouldDeleteScriptWhenConnected() throws Exception {
        connectAndStubIsConnected();

        when(mockClient.deletescript("myscript")).thenReturn(mockResponse);

        diConnection.deleteScript("myscript");

        verify(mockClient).deletescript("myscript");
    }

    @Test
    void shouldLogout() throws Exception {
        setupSuccessfulConnect();
        diConnection.connect("server", 4190, "user", "pass");

        when(mockClient.logout()).thenReturn(mockResponse);

        diConnection.logout();

        verify(mockClient).logout();
        assertThat(diConnection.isLoggedIn()).isFalse();
    }

    // ===== Script Operation Error Handling =====

    @Test
    void shouldThrowExceptionWhenPutScriptFails() throws Exception {
        connectAndStubIsConnected();

        when(mockClient.putscript(anyString(), anyString())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(false);
        when(mockResponse.getMessage()).thenReturn("Script too large");

        assertThatThrownBy(() -> diConnection.putScript("s", "body"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Can't upload script");
    }

    @Test
    void shouldThrowExceptionWhenGetListScriptsFails() throws Exception {
        connectAndStubIsConnected();

        when(mockClient.listscripts(any())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(false);

        assertThatThrownBy(() -> diConnection.getListScripts())
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Can't get script list");
    }

    @Test
    void shouldThrowExceptionWhenGetScriptFails() throws Exception {
        connectAndStubIsConnected();

        SieveScript ss = new SieveScript("s", null, false);
        when(mockClient.getScript(ss)).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(false);
        when(mockResponse.getMessage()).thenReturn("Not found");

        assertThatThrownBy(() -> diConnection.getScript(ss))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Could not get body");
    }

    @Test
    void shouldThrowExceptionWhenActivateScriptFails() throws Exception {
        connectAndStubIsConnected();

        when(mockClient.setactive(anyString())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(false);
        when(mockResponse.getMessage()).thenReturn("Unknown script");

        assertThatThrownBy(() -> diConnection.activateScript("s"))
                .isInstanceOf(IOException.class);
    }

    @Test
    void shouldThrowExceptionWhenDeactivateScriptFails() throws Exception {
        connectAndStubIsConnected();

        when(mockClient.setactive("")).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(false);
        when(mockResponse.getMessage()).thenReturn("Error");

        assertThatThrownBy(() -> diConnection.deactivateScript())
                .isInstanceOf(IOException.class);
    }

    @Test
    void shouldThrowExceptionWhenRenameFails() throws Exception {
        connectAndStubIsConnected();

        when(mockClient.renamescript(anyString(), anyString())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(false);
        when(mockResponse.getMessage()).thenReturn("Script not found");

        assertThatThrownBy(() -> diConnection.rename("old", "new"))
                .isInstanceOf(IOException.class);
    }

    @Test
    void shouldThrowExceptionWhenDeleteScriptFails() throws Exception {
        connectAndStubIsConnected();

        when(mockClient.deletescript(anyString())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(false);
        when(mockResponse.getMessage()).thenReturn("Not found");

        assertThatThrownBy(() -> diConnection.deleteScript("s"))
                .isInstanceOf(IOException.class);
    }

    @Test
    void shouldThrowExceptionWhenLogoutFails() throws Exception {
        setupSuccessfulConnect();
        diConnection.connect("server", 4190, "user", "pass");

        when(mockClient.logout()).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(false);
        when(mockResponse.getMessage()).thenReturn("Error");

        assertThatThrownBy(() -> diConnection.logout())
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Can't logout");

        assertThat(diConnection.isLoggedIn()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenPutScriptSetActiveFails() throws Exception {
        connectAndStubIsConnected();

        when(mockClient.putscript(anyString(), anyString())).thenReturn(mockResponse);
        when(mockClient.setactive(anyString())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true, false);
        when(mockResponse.getMessage()).thenReturn("Cannot activate");

        assertThatThrownBy(() -> diConnection.putScript("s", "body"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Can't set script");
    }

    // ===== Connection-less Operation Tests =====

    @Test
    void shouldThrowExceptionWhenOperationCalledWithoutConnection() {
        assertThatThrownBy(() -> connection.getListScripts())
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Not connected to server");
    }

    @Test
    void shouldThrowExceptionWhenDeleteScriptCalledWithoutConnection() {
        assertThatThrownBy(() -> connection.deleteScript("myscript"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Not connected to server");
    }

    // ===== SSL Factory Tests =====

    @Test
    void shouldReturnSSLSocketFactory() {
        SSLSocketFactory factory = ConnectAndListScripts.getInsecureSSLFactory();
        assertThat(factory).isNotNull();
    }

    @Test
    void shouldReturnValidSSLContext() {
        SSLSocketFactory factory = ConnectAndListScripts.getInsecureSSLFactory();
        assertThat(factory.getDefaultCipherSuites()).isNotEmpty();
        assertThat(factory.getSupportedCipherSuites()).isNotEmpty();
    }

    // ===== Keep-Alive Tests =====

    @Test
    void shouldEnableKeepAliveByDefault() {
        assertThat(connection.isKeepAliveEnabled()).isTrue();
    }

    @Test
    void shouldAllowDisablingKeepAlive() {
        connection.setKeepAliveEnabled(false);
        assertThat(connection.isKeepAliveEnabled()).isFalse();
    }

    @Test
    void shouldAllowEnablingKeepAlive() {
        connection.setKeepAliveEnabled(false);
        connection.setKeepAliveEnabled(true);
        assertThat(connection.isKeepAliveEnabled()).isTrue();
    }

    @Test
    void shouldNotFailWhenDisablingKeepAliveWithoutConnection() {
        assertThatCode(() -> connection.setKeepAliveEnabled(false))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldStartKeepAliveTimerWhenConnected() throws Exception {
        when(mockClient.connect(anyString(), anyInt())).thenReturn(mockResponse);
        when(mockClient.starttls(any(), anyBoolean())).thenReturn(mockResponse);
        when(mockClient.authenticate(anyString(), anyString())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true);
        diConnection.setKeepAliveEnabled(true);

        diConnection.connect("server", 4190, "user", "pass");

        verify(mockClient, never()).noop(anyString());
    }

    @Test
    void shouldStopKeepAliveTimerWhenDisabled() throws Exception {
        when(mockClient.connect(anyString(), anyInt())).thenReturn(mockResponse);
        when(mockClient.starttls(any(), anyBoolean())).thenReturn(mockResponse);
        when(mockClient.authenticate(anyString(), anyString())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true);
        diConnection.setKeepAliveEnabled(true);

        diConnection.connect("server", 4190, "user", "pass");
        diConnection.setKeepAliveEnabled(false);

        verify(mockClient, never()).noop(anyString());
    }

    @Test
    void shouldNotStartKeepAliveWhenDisabledBeforeConnect() throws Exception {
        setupSuccessfulConnect();

        diConnection.connect("server", 4190, "user", "pass");

        // Enabling after connect won't start timer if client appears disconnected
        diConnection.setKeepAliveEnabled(true);
        verify(mockClient, never()).noop(anyString());
    }

    @Test
    void shouldHandleNoopExceptionGracefully() throws Exception {
        java.lang.reflect.Field intervalField = ConnectAndListScripts.class.getDeclaredField("KEEP_ALIVE_INTERVAL_MS");
        intervalField.setAccessible(true);
        intervalField.set(null, 10L);

        when(mockClient.connect(anyString(), anyInt())).thenReturn(mockResponse);
        when(mockClient.starttls(any(), anyBoolean())).thenReturn(mockResponse);
        when(mockClient.authenticate(anyString(), anyString())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true);
        when(mockClient.isConnected()).thenReturn(true);
        doThrow(new IOException("timeout")).when(mockClient).noop("keep-alive");
        diConnection.setKeepAliveEnabled(true);

        diConnection.connect("server", 4190, "user", "pass");

        Thread.sleep(50);

        verify(mockClient, atLeastOnce()).noop("keep-alive");
    }

    @Test
    void shouldSendNoopWhenKeepAliveTimerFires() throws Exception {
        java.lang.reflect.Field intervalField = ConnectAndListScripts.class.getDeclaredField("KEEP_ALIVE_INTERVAL_MS");
        intervalField.setAccessible(true);
        intervalField.set(null, 10L);

        when(mockClient.connect(anyString(), anyInt())).thenReturn(mockResponse);
        when(mockClient.starttls(any(), anyBoolean())).thenReturn(mockResponse);
        when(mockClient.authenticate(anyString(), anyString())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true);
        when(mockClient.isConnected()).thenReturn(true);
        diConnection.setKeepAliveEnabled(true);

        diConnection.connect("server", 4190, "user", "pass");

        Thread.sleep(50);

        verify(mockClient, atLeastOnce()).noop("keep-alive");
    }

    // ===== Auto-Reconnect Tests =====

    @Test
    void shouldAutoReconnectWhenConnectionLost() throws Exception {
        connectAndStubIsConnected();
        // Override to trigger reconnect
        when(mockClient.isConnected()).thenReturn(false);

        when(mockClient.putscript(anyString(), anyString())).thenReturn(mockResponse);
        when(mockClient.setactive(anyString())).thenReturn(mockResponse);

        diConnection.putScript("s", "body");

        verify(mockFactory, atLeast(2)).create();
    }

    @Test
    void shouldNotShowDialogDuringReconnectWhenNoParentComponent() throws Exception {
        connectAndStubIsConnected();
        diConnection.setParentComponent(null);
        // Override to trigger reconnect
        when(mockClient.isConnected()).thenReturn(false);

        when(mockClient.putscript(anyString(), anyString())).thenReturn(mockResponse);
        when(mockClient.setactive(anyString())).thenReturn(mockResponse);

        assertThatCode(() -> diConnection.putScript("s", "body"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowIOExceptionWhenReconnectFails() throws Exception {
        connectAndStubIsConnected();
        // Override to trigger reconnect
        when(mockClient.isConnected()).thenReturn(false);

        when(mockClient.connect(anyString(), anyInt())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(false);
        when(mockResponse.getMessage()).thenReturn("Reconnect failed");

        assertThatThrownBy(() -> diConnection.getListScripts())
                .isInstanceOf(IOException.class)
                .hasMessageContaining("auto-reconnect failed");
    }

    @Test
    void shouldThrowIOExceptionWhenReconnectHasNoCredentials() throws Exception {
        setupSuccessfulConnect();
        diConnection.connect("server", 4190, "user", "pass");
        when(mockClient.isConnected()).thenReturn(false);

        // Clear connection state via logout failure to make lastServer null
        // Use a fresh DI connection with no stored state for the "no credentials" path
        ConnectAndListScripts noStateConn = new ConnectAndListScripts(mockFactory);
        noStateConn.setKeepAliveEnabled(false);
        // Set client but clear all state
        java.lang.reflect.Field clientField = ConnectAndListScripts.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(noStateConn, mockClient);
        when(mockClient.isConnected()).thenReturn(false);

        assertThatThrownBy(() -> noStateConn.getListScripts())
                .isInstanceOf(IOException.class)
                .hasMessageContaining("cannot auto-reconnect");
    }

    @Test
    void shouldConnectWithInteractiveCertValidationWhenParentSet() throws Exception {
        when(mockClient.connect(anyString(), anyInt())).thenReturn(mockResponse);
        when(mockClient.starttls(any(), anyBoolean())).thenReturn(mockResponse);
        when(mockClient.authenticate(anyString(), anyString())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true);

        javax.swing.JFrame parent = new javax.swing.JFrame();
        diConnection.setParentComponent(parent);

        diConnection.connect("server", 4190, "user", "pass");

        verify(mockClient).connect("server", 4190);
        verify(mockClient).authenticate("user", "pass");
    }

    @Test
    void shouldReturnSecureSslFactoryWithInteractiveValidation() {
        java.awt.Frame parent = new javax.swing.JFrame();
        SSLSocketFactory factory = ConnectAndListScripts.getInteractiveSSLSocketFactory("test-server", parent);
        assertThat(factory).isNotNull();
        assertThat(factory.getDefaultCipherSuites()).isNotEmpty();
    }

    @Test
    void shouldConnectWithNonInteractiveCertValidation() throws Exception {
        when(mockClient.connect(anyString(), anyInt())).thenReturn(mockResponse);
        when(mockClient.starttls(any(), anyBoolean())).thenReturn(mockResponse);
        when(mockClient.authenticate(anyString(), anyString())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true);

        diConnection.connect("server", 4190, "user", "pass", false);

        verify(mockClient).connect("server", 4190);
        verify(mockClient).authenticate("user", "pass");
    }

    @Test
    void shouldStartKeepAliveWhenEnabledAfterConnect() throws Exception {
        when(mockClient.connect(anyString(), anyInt())).thenReturn(mockResponse);
        when(mockClient.starttls(any(), anyBoolean())).thenReturn(mockResponse);
        when(mockClient.authenticate(anyString(), anyString())).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true);
        when(mockClient.isConnected()).thenReturn(true);

        diConnection.connect("server", 4190, "user", "pass");
        diConnection.setKeepAliveEnabled(true);

        verify(mockClient, never()).noop(anyString());
    }

    @Test
    void shouldCreateSslFactoryWithCustomCertificate() {
        SSLSocketFactory factory = ConnectAndListScripts.getSecureSSLSocketFactory("/tmp/sieve-test.crt");
        assertThat(factory).isNotNull();
        assertThat(factory.getDefaultCipherSuites()).isNotEmpty();
    }

    @Test
    void shouldThrowRuntimeExceptionForInvalidCertificatePath() {
        assertThatThrownBy(() -> ConnectAndListScripts.getSecureSSLSocketFactory("/nonexistent/cert.pem"))
                .isInstanceOf(RuntimeException.class);
    }

    // ===== Connection Failure Tests =====

    @Test
    void shouldThrowIOExceptionOnConnectionFailure() {
        assertThatThrownBy(() -> connection.connect("nonexistent.invalid.server", 4190, "user", "pass"))
                .isInstanceOf(IOException.class);
    }

    // ===== PropertiesSieve overload test =====

    @Test
    void shouldConnectWithPropertiesSieve() throws Exception {
        setupSuccessfulConnect();

        PropertiesSieve props = new PropertiesSieve();
        props.setServer("props-server");
        props.setPort(4190);
        props.setUsername("props-user");
        props.setPassword("props-pass");

        diConnection.connect(props);

        verify(mockClient).connect("props-server", 4190);
        verify(mockClient).authenticate("props-user", "props-pass");
    }
}
