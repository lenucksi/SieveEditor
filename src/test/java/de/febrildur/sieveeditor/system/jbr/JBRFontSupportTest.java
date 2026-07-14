package de.febrildur.sieveeditor.system.jbr;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Font;

import org.junit.jupiter.api.Test;

/**
 * Under Maven test (JaCoCo agent), JBR.isAvailable() returns false.
 * FontExtensions gracefully falls back: deriveEditorFont returns base font unchanged.
 */
class JBRFontSupportTest {

    @Test
    void notSupportedInTestEnv() {
        assertThat(JBRFontSupport.isSupported()).isFalse();
    }

    @Test
    void deriveEditorFontReturnsBaseFontWhenUnsupported() {
        Font base = new Font(Font.MONOSPACED, Font.PLAIN, 13);
        Font result = JBRFontSupport.deriveEditorFont(base);
        assertThat(result).isSameAs(base);
    }

    @Test
    void getJbrMonoFontReturnsNullInTestEnv() {
        // Under test (JaCoCo), java.home is the system JVM, not JBR,
        // so no bundled JetBrains Mono font is found.
        assertThat(JBRFontSupport.getJbrMonoFont()).isNull();
    }
}
