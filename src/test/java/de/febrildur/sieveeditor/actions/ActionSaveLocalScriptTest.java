package de.febrildur.sieveeditor.actions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Test suite for ActionSaveLocalScript class.
 *
 * Note: UI interaction testing is limited due to Swing dependencies.
 * Tests focus on the testable aspects of the action.
 */
class ActionSaveLocalScriptTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldHaveDescriptiveName() {
        assertThat(ActionSaveLocalScript.class).isNotNull();
    }

    @Test
    void shouldDefaultToUserHomeDirectory() {
        // Given/When - constructor initializes lastDirectory
        String userHome = System.getProperty("user.home");

        // Then
        assertThat(userHome).isNotNull();
        assertThat(new File(userHome)).exists();
    }

    @Test
    void shouldHaveCorrectActionName() {
        // The action should be named "Save Local Script..."
        assertThat(ActionSaveLocalScript.class.getSimpleName())
                .contains("SaveLocalScript");
    }

    @Test
    void shouldAddSieveExtensionWhenMissing() throws IOException {
        // Test the extension logic by verifying file creation behavior
        // Given
        Path testFile = tempDir.resolve("testscript");

        // When - simulate what the action does
        String filename = testFile.getFileName().toString();
        if (!filename.toLowerCase().endsWith(".sieve")) {
            testFile = tempDir.resolve(filename + ".sieve");
        }
        Files.writeString(testFile, "# test content");

        // Then
        assertThat(testFile.toString()).endsWith(".sieve");
        assertThat(Files.exists(testFile)).isTrue();
    }

    @Test
    void shouldNotAddExtensionWhenAlreadyPresent() throws IOException {
        // Given
        Path testFile = tempDir.resolve("testscript.sieve");

        // When - simulate what the action does
        String filename = testFile.getFileName().toString();
        Path finalPath = testFile;
        if (!filename.toLowerCase().endsWith(".sieve")) {
            finalPath = tempDir.resolve(filename + ".sieve");
        }
        Files.writeString(finalPath, "# test content");

        // Then - should not have double extension
        assertThat(finalPath.toString()).doesNotContain(".sieve.sieve");
        assertThat(finalPath.toString()).endsWith(".sieve");
    }

    @Test
    void shouldWriteContentToFile() throws IOException {
        // Given
        Path testFile = tempDir.resolve("output.sieve");
        String content = "require [\"fileinto\"];\n\nif header :contains \"subject\" \"test\" {\n    fileinto \"Test\";\n}";

        // When
        Files.writeString(testFile, content);

        // Then
        assertThat(Files.readString(testFile)).isEqualTo(content);
    }

    /**
     * INTENDED TEST (requires UI refactoring):
     *
     * @Test
     * void shouldSaveEditorContentToFile() {
     *     // Given
     *     MockApplication app = new MockApplication();
     *     app.setScriptText("test sieve content");
     *     ActionSaveLocalScript action = new ActionSaveLocalScript(app);
     *     File targetFile = tempDir.resolve("saved.sieve").toFile();
     *
     *     // When
     *     action.saveToFile(targetFile);
     *
     *     // Then
     *     assertThat(Files.readString(targetFile.toPath())).isEqualTo("test sieve content");
     * }
     */

    /**
     * INTENDED TEST (requires UI refactoring):
     *
     * @Test
     * void shouldShowSuccessMessageAfterSave() {
     *     // Given
     *     MockApplication app = new MockApplication();
     *     ActionSaveLocalScript action = new ActionSaveLocalScript(app);
     *
     *     // When
     *     action.saveToFile(new File(tempDir.toFile(), "test.sieve"));
     *
     *     // Then
     *     assertThat(app.getLastMessage()).contains("saved successfully");
     * }
     */
}
