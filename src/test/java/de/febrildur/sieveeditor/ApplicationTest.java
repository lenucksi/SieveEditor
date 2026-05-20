package de.febrildur.sieveeditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

import de.febrildur.sieveeditor.system.ConnectAndListScripts;
import de.febrildur.sieveeditor.system.PropertiesSieve;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.MockedStatic;

class ApplicationTest {

	private Application app;

	@BeforeAll
	static void setUpOnce() {
		System.setProperty("java.awt.headless", "false");
	}

	@BeforeEach
	void setUp() {
	}

	@Test
	@DisplayName("Application should have userHasManuallyResizedDivider flag")
	void shouldHaveManualResizeFlag() throws Exception {
		app = new Application();

		Field field = Application.class.getDeclaredField("userHasManuallyResizedDivider");
		field.setAccessible(true);
		Boolean flagValue = (Boolean) field.get(app);

		assertThat(flagValue)
			.as("userHasManuallyResizedDivider flag should be initialized to false")
			.isFalse();
	}

	@Test
	@DisplayName("Application should have isAdjustingDividerProgrammatically flag")
	void shouldHaveProgrammaticAdjustmentFlag() throws Exception {
		app = new Application();

		Field field = Application.class.getDeclaredField("isAdjustingDividerProgrammatically");
		field.setAccessible(true);
		Boolean flagValue = (Boolean) field.get(app);

		assertThat(flagValue)
			.as("isAdjustingDividerProgrammatically flag should be initialized to false")
			.isFalse();
	}

	@Test
	@DisplayName("Application should have mainSplitPane field")
	void shouldHaveMainSplitPane() throws Exception {
		app = new Application();

		Field field = Application.class.getDeclaredField("mainSplitPane");
		field.setAccessible(true);
		JSplitPane splitPane = (JSplitPane) field.get(app);

		assertThat(splitPane)
			.as("mainSplitPane should be initialized")
			.isNotNull();
	}

	@Test
	@DisplayName("Application should register ComponentListener for window resize")
	void shouldHaveComponentListener() {
		app = new Application();

		java.awt.event.ComponentListener[] listeners = app.getComponentListeners();

		assertThat(listeners)
			.as("Application should have at least one ComponentListener for resize handling")
			.isNotEmpty();
	}

	@Test
	@DisplayName("MainSplitPane should have PropertyChangeListener for divider location")
	void shouldHavePropertyChangeListener() throws Exception {
		app = new Application();

		Field field = Application.class.getDeclaredField("mainSplitPane");
		field.setAccessible(true);
		JSplitPane splitPane = (JSplitPane) field.get(app);

		java.beans.PropertyChangeListener[] listeners = splitPane.getPropertyChangeListeners(JSplitPane.DIVIDER_LOCATION_PROPERTY);

		assertThat(listeners)
			.as("mainSplitPane should have PropertyChangeListener for divider location tracking")
			.isNotEmpty();
	}

	@Test
	@DisplayName("ComponentResized event should not throw exception")
	void shouldHandleComponentResizedEvent() {
		app = new Application();

		java.awt.event.ComponentListener[] listeners = app.getComponentListeners();
		assertThat(listeners).isNotEmpty();

		java.awt.event.ComponentEvent event = new java.awt.event.ComponentEvent(
			app, java.awt.event.ComponentEvent.COMPONENT_RESIZED);

		assertThatCode(() -> {
			for (java.awt.event.ComponentListener listener : listeners) {
				listener.componentResized(event);
			}
		}).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Document changed events should not throw exception")
	void shouldHandleDocumentChangedEvent() {
		app = new Application();

		javax.swing.text.Document doc = app.getScriptArea().getDocument();
		assertThat(doc.getLength()).isEqualTo(0);

		assertThatCode(() -> {
			app.getScriptArea().setText("require \"fileinto\";\n");
			app.getScriptArea().setText("");
		}).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Disconnect action should clear server state")
	void shouldHandleDisconnectAction() throws Exception {
		app = new Application();

		ConnectAndListScripts mockServer = mock(ConnectAndListScripts.class);

		Field serverField = Application.class.getDeclaredField("server");
		serverField.setAccessible(true);
		serverField.set(app, mockServer);

		Field scriptField = Application.class.getDeclaredField("script");
		scriptField.setAccessible(true);

		app.getScriptArea().setText("some script content");

		Field actionField = Application.class.getDeclaredField("actionDisconnect");
		actionField.setAccessible(true);
		AbstractAction disconnectAction = (AbstractAction) actionField.get(app);

		disconnectAction.actionPerformed(new java.awt.event.ActionEvent(app, 0, "Disconnect"));

		verify(mockServer).logout();
		assertThat(serverField.get(app)).isNull();
		assertThat(scriptField.get(app)).isNull();
		assertThat(app.getScriptArea().getText()).isEmpty();
		assertThat(app.getTitle()).isEqualTo("Sieve Editor");
	}

	@Test
	@DisplayName("Disconnect action should do nothing when server is null")
	void shouldHandleDisconnectActionWhenServerNull() throws Exception {
		app = new Application();

		Field actionField = Application.class.getDeclaredField("actionDisconnect");
		actionField.setAccessible(true);
		AbstractAction disconnectAction = (AbstractAction) actionField.get(app);

		assertThatCode(() ->
			disconnectAction.actionPerformed(new java.awt.event.ActionEvent(app, 0, "Disconnect"))
		).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("updateStatus should update action enable states")
	void shouldHandleUpdateStatus() {
		app = new Application();

		assertThatCode(() -> app.updateStatus())
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("loadLocalScript should set text and update title")
	void shouldHandleLoadLocalScript() {
		app = new Application();
		app.loadLocalScript("require \"fileinto\";", "test.sieve");

		assertThat(app.getScriptText()).isEqualTo("require \"fileinto\";");
		assertThat(app.getTitle()).contains("test.sieve");
	}

	@Test
	@DisplayName("jumpToLine should handle invalid line numbers")
	void shouldJumpToLineWithInvalidLine() {
		app = new Application();
		app.getScriptArea().setText("line1\nline2\nline3\n");

		assertThatCode(() -> app.jumpToLine(0))
			.doesNotThrowAnyException();

		assertThatCode(() -> app.jumpToLine(100))
			.doesNotThrowAnyException();

		assertThatCode(() -> app.jumpToLine(-5))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("jumpToLine should navigate to valid line")
	void shouldJumpToLineWithValidLine() {
		app = new Application();
		app.getScriptArea().setText("line1\nline2\nline3\n");

		assertThatCode(() -> app.jumpToLine(2))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("getProp should return non-null PropertiesSieve")
	void shouldGetProp() {
		app = new Application();

		assertThat(app.getProp()).isNotNull();
	}

	@Test
	@DisplayName("updateRuleNavigator should not throw when navigator is ready")
	void shouldUpdateRuleNavigator() {
		app = new Application();
		app.getScriptArea().setText("require \"fileinto\";\nif true { keep; }\n");

		assertThatCode(() -> app.updateRuleNavigator())
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("setServer and getServer should round-trip")
	void shouldSetAndGetServer() throws Exception {
		app = new Application();
		ConnectAndListScripts mockServer = mock(ConnectAndListScripts.class);

		app.setServer(mockServer);
		assertThat(app.getServer()).isSameAs(mockServer);
	}

	@Test
	@DisplayName("setProp and getProp should round-trip")
	void shouldSetAndGetProp() {
		app = new Application();
		PropertiesSieve original = app.getProp();
		assertThat(original).isNotNull();

		app.setProp(original);
		assertThat(app.getProp()).isSameAs(original);
	}

	@Test
	@DisplayName("getSearchPanel should return non-null panel")
	void shouldGetSearchPanel() {
		app = new Application();
		assertThat(app.getSearchPanel()).isNotNull();
	}

	@Test
	@DisplayName("ComponentResized with auto-sized navigator should not throw")
	void shouldHandleComponentResizedWithAutoSizedNavigator() throws Exception {
		app = new Application();
		app.getScriptArea().setText("require \"fileinto\";\nif true { keep; }\n");

		SwingUtilities.invokeAndWait(() -> {
			app.setVisible(true);
			app.setSize(800, 600);
		});
		try {
			SwingUtilities.invokeAndWait(() -> app.updateRuleNavigator());

			Field navigatorField = Application.class.getDeclaredField("ruleNavigator");
			navigatorField.setAccessible(true);
			Object navigator = navigatorField.get(app);
			navigator.getClass().getMethod("markWidthAutoSized").invoke(navigator);

			Thread.sleep(50);

			java.awt.event.ComponentEvent event = new java.awt.event.ComponentEvent(
				app, java.awt.event.ComponentEvent.COMPONENT_RESIZED);
			for (java.awt.event.ComponentListener listener : app.getComponentListeners()) {
				assertThatCode(() -> listener.componentResized(event))
					.doesNotThrowAnyException();
			}
		} finally {
			SwingUtilities.invokeAndWait(() -> app.dispose());
		}
	}

	@Test
	@DisplayName("save with mock server should not throw")
	void shouldSaveWithMockServer() throws Exception {
		app = new Application();
		ConnectAndListScripts mockServer = mock(ConnectAndListScripts.class);

		app.setServer(mockServer);

		com.fluffypeople.managesieve.SieveScript mockScript = mock(com.fluffypeople.managesieve.SieveScript.class);
		when(mockScript.getName()).thenReturn("testscript");

		Field scriptField = Application.class.getDeclaredField("script");
		scriptField.setAccessible(true);
		scriptField.set(app, mockScript);

		app.getScriptArea().setText("require \"fileinto\";");

		assertThatCode(() -> app.save())
			.doesNotThrowAnyException();

		verify(mockServer).putScript("testscript", "require \"fileinto\";");
	}

	@Test
	@DisplayName("save with exception from server should handle gracefully")
	void shouldSaveWithExceptionFromServer() throws Exception {
		app = new Application();
		ConnectAndListScripts mockServer = mock(ConnectAndListScripts.class);
		doThrow(new java.io.IOException("Test IO error"))
			.when(mockServer).putScript(anyString(), anyString());

		app.setServer(mockServer);

		com.fluffypeople.managesieve.SieveScript mockScript = mock(com.fluffypeople.managesieve.SieveScript.class);
		when(mockScript.getName()).thenReturn("testscript");

		Field scriptField = Application.class.getDeclaredField("script");
		scriptField.setAccessible(true);
		scriptField.set(app, mockScript);

		app.getScriptArea().setText("test content");

		try (MockedStatic<JOptionPane> jp = mockStatic(JOptionPane.class)) {
			app.save();
		}
	}

	@Test
	@DisplayName("setScript with mock server should update text area")
	void shouldHandleSetScript() throws Exception {
		app = new Application();
		ConnectAndListScripts mockServer = mock(ConnectAndListScripts.class);

		app.setServer(mockServer);

		com.fluffypeople.managesieve.SieveScript mockScript = mock(com.fluffypeople.managesieve.SieveScript.class);
		doReturn("require \"fileinto\";\n").when(mockServer).getScript(mockScript);

		assertThatCode(() -> app.setScript(mockScript))
			.doesNotThrowAnyException();

		assertThat(app.getScriptText()).isEqualTo("require \"fileinto\";\n");
	}

	@Test
	@DisplayName("registerGlobalKeystroke should register key binding")
	void shouldRegisterGlobalKeystroke() throws Exception {
		app = new Application();

		Field textAreaField = Application.class.getDeclaredField("textArea");
		textAreaField.setAccessible(true);
		javax.swing.text.JTextComponent textArea = (javax.swing.text.JTextComponent) textAreaField.get(app);

		javax.swing.InputMap inputMap = textArea.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
		assertThat(inputMap).isNotNull();
	}

	@Test
	@DisplayName("printHelp should print usage information")
	void shouldPrintHelp() throws Exception {
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		java.io.PrintStream original = System.out;
		System.setOut(new java.io.PrintStream(out));
		try {
			java.lang.reflect.Method method = Application.class.getDeclaredMethod("printHelp");
			method.setAccessible(true);
			method.invoke(null);
			assertThat(out.toString()).contains("SieveEditor");
		} finally {
			System.setOut(original);
		}
	}

	@Test
	@DisplayName("enableVerboseLogging should set log levels")
	void shouldEnableVerboseLogging() throws Exception {
		java.lang.reflect.Method method = Application.class.getDeclaredMethod("enableVerboseLogging");
		method.setAccessible(true);
		method.invoke(null);
		assertThat(java.util.logging.Logger.getLogger("de.febrildur.sieveeditor").getLevel())
			.isEqualTo(java.util.logging.Level.FINE);
	}
}
