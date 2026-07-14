package de.febrildur.sieveeditor.system.jbr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URI;

import org.junit.jupiter.api.Test;

/**
 * Under Maven test (JaCoCo agent), JBR.isAvailable() returns false.
 * DesktopActions gracefully falls back to java.awt.Desktop.
 */
class JBRDesktopSupportTest {

    @Test
    void notSupportedInTestEnv() {
        assertThat(JBRDesktopSupport.isSupported()).isFalse();
    }

    @Test
    void browseDoesNotThrow() {
        JBRDesktopSupport.browse(URI.create("https://example.com"));
    }

    @Test
    void editDoesNotThrow() {
        JBRDesktopSupport.edit(new File("/nonexistent"));
    }
}
