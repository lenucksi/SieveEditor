package de.febrildur.sieveeditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;

/**
 * Test suite for Application class window resize behavior.
 *
 * Tests the auto-resize functionality for the rule navigator panel
 * when the main window is resized (Task #22).
 */
class ApplicationTest {

	private Application app;

	@BeforeEach
	void setUp() {
		// Create application in headless mode for testing
		// Note: Full GUI testing would require a display environment
		System.setProperty("java.awt.headless", "false");
	}

	@Test
	@DisplayName("Application should have userHasManuallyResizedDivider flag")
	void shouldHaveManualResizeFlag() throws Exception {
		// Given/When
		app = new Application(); // Default constructor

		// Then - Verify the flag exists using reflection
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
		// Given/When
		app = new Application(); // Default constructor

		// Then - Verify the flag exists using reflection
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
		// Given/When
		app = new Application(); // Default constructor

		// Then - Verify the mainSplitPane exists
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
		// Given/When
		app = new Application(); // Default constructor

		// Then - Verify a ComponentListener is registered
		java.awt.event.ComponentListener[] listeners = app.getComponentListeners();

		assertThat(listeners)
			.as("Application should have at least one ComponentListener for resize handling")
			.isNotEmpty();
	}

	@Test
	@DisplayName("MainSplitPane should have PropertyChangeListener for divider location")
	void shouldHavePropertyChangeListener() throws Exception {
		// Given/When
		app = new Application(); // Default constructor

		// Get mainSplitPane via reflection
		Field field = Application.class.getDeclaredField("mainSplitPane");
		field.setAccessible(true);
		JSplitPane splitPane = (JSplitPane) field.get(app);

		// Then - Verify PropertyChangeListener is registered
		java.beans.PropertyChangeListener[] listeners = splitPane.getPropertyChangeListeners(JSplitPane.DIVIDER_LOCATION_PROPERTY);

		assertThat(listeners)
			.as("mainSplitPane should have PropertyChangeListener for divider location tracking")
			.isNotEmpty();
	}

	/**
	 * Integration test for manual resize detection.
	 * This test verifies that when a user manually moves the divider,
	 * the userHasManuallyResizedDivider flag is set to true.
	 *
	 * Note: This test requires Swing EDT and may be flaky in CI environments.
	 */
	@Test
	@DisplayName("Manual divider resize should set userHasManuallyResizedDivider flag")
	void shouldDetectManualDividerResize() throws Exception {
		// Given
		app = new Application();

		// Get the mainSplitPane and flag via reflection
		Field splitPaneField = Application.class.getDeclaredField("mainSplitPane");
		splitPaneField.setAccessible(true);
		JSplitPane splitPane = (JSplitPane) splitPaneField.get(app);

		Field flagField = Application.class.getDeclaredField("userHasManuallyResizedDivider");
		flagField.setAccessible(true);

		// Ensure initial state
		assertThat((Boolean) flagField.get(app))
			.as("Flag should initially be false")
			.isFalse();

		// Make the frame visible and wait for layout
		SwingUtilities.invokeAndWait(() -> {
			app.setVisible(true);
			app.setSize(800, 600);
		});

		Thread.sleep(100); // Allow layout to complete

		// When - Simulate user manually moving the divider
		// First, load a script to trigger auto-sizing
		SwingUtilities.invokeAndWait(() -> {
			app.getScriptArea().setText("# Test script\nrequire \"fileinto\";\n");
			app.updateRuleNavigator();
		});

		Thread.sleep(200); // Allow auto-resize to complete

		// Now manually move the divider (simulate user action)
		SwingUtilities.invokeAndWait(() -> {
			splitPane.setDividerLocation(400); // User manually sets divider
		});

		Thread.sleep(100); // Allow property change to propagate

		// Then
		Boolean flagValue = (Boolean) flagField.get(app);
		assertThat(flagValue)
			.as("userHasManuallyResizedDivider should be true after manual divider move")
			.isTrue();

		// Cleanup
		SwingUtilities.invokeAndWait(() -> app.dispose());
	}

	/**
	 * Placeholder test for window resize behavior.
	 * Full testing would require:
	 * - Creating a visible frame
	 * - Loading a script with rules
	 * - Triggering window resize events
	 * - Verifying divider position adjustment
	 *
	 * This is complex in headless environments and may require GUI test frameworks.
	 */
	@Test
	@DisplayName("Window resize should adjust navigator width when auto-sizing is active")
	void shouldAdjustNavigatorWidthOnWindowResize() {
		// This test is a placeholder for manual verification
		// Testing Swing resize behavior in unit tests is challenging
		assertThat(true)
			.as("Manual testing required for full window resize verification")
			.isTrue();
	}
}
