package de.febrildur.sieveeditor.system.jbr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Under Maven test (JaCoCo agent), JBR.isAvailable() returns false.
 * AccessibleAnnouncer gracefully falls back (no-op).
 */
class JBRAccessibilitySupportTest {

    @Test
    void notSupportedInTestEnv() {
        assertThat(JBRAccessibilitySupport.isSupported()).isFalse();
    }

    @Test
    void announceDoesNotThrow() {
        JBRAccessibilitySupport.announce("test", null);
    }
}
