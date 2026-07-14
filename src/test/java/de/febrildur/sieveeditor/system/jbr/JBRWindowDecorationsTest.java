package de.febrildur.sieveeditor.system.jbr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * WindowDecorations requires full JBR runtime — not available on vanilla JBR.
 */
class JBRWindowDecorationsTest {

    @Test
    void notSupportedOnVanillaJbr() {
        assertThat(JBRWindowDecorations.isSupported()).isFalse();
    }

    @Test
    void applyCustomTitleBarReturnsFalse() {
        assertThat(JBRWindowDecorations.applyCustomTitleBar(null, 0f)).isFalse();
    }
}
