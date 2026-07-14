package de.febrildur.sieveeditor.system.jbr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * RoundedCornersManager requires full JBR runtime — not available on vanilla JBR.
 * (GNOME already rounds corners via CSD, so no loss on Linux.)
 */
class JBRRoundedCornersTest {

    @Test
    void notSupportedOnVanillaJbr() {
        assertThat(JBRRoundedCorners.isSupported()).isFalse();
    }

    @Test
    void applyReturnsFalse() {
        assertThat(JBRRoundedCorners.apply(null)).isFalse();
    }
}
