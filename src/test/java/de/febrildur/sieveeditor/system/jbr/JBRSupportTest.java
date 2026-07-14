package de.febrildur.sieveeditor.system.jbr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * In the Maven test environment (with JaCoCo agent on boot classpath),
 * JBR.isAvailable() returns false. All JBR API services gracefully fall back.
 */
class JBRSupportTest {

    @Test
    void jbrApiNotAvailableUnderTest() {
        assertThat(JBRSupport.isAvailable()).isFalse();
    }

    @Test
    void apiVersionFromJar() {
        assertThat(JBRSupport.getApiVersion()).isEqualTo("SNAPSHOT");
    }

    @Test
    void implVersionUnknown() {
        assertThat(JBRSupport.getImplVersion()).isEqualTo("UNKNOWN");
    }

    @Test
    void logStatusReturnsFalseAndDoesNotThrow() {
        assertThat(JBRSupport.logStatus()).isFalse();
    }
}
