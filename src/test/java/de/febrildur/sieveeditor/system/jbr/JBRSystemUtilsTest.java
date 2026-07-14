package de.febrildur.sieveeditor.system.jbr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Under Maven test (JaCoCo agent), JBR.isAvailable() returns false.
 * SystemUtils gracefully falls back (no-op).
 */
class JBRSystemUtilsTest {

    @Test
    void notSupportedInTestEnv() {
        assertThat(JBRSystemUtils.isSupported()).isFalse();
    }

    @Test
    void tryCompactMemoryDoesNotThrow() {
        JBRSystemUtils.tryCompactMemory();
    }
}
