package de.febrildur.sieveeditor;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test to verify test infrastructure is working correctly.
 * This test should always pass and confirms that JUnit 5, AssertJ,
 * and the test execution environment are properly configured.
 */
class SmokeTest {

    @Test
    void shouldVerifyTestInfrastructureWorks() {
        // Given
        String message = "Test infrastructure is working";

        // When
        int length = message.length();

        // Then
        assertThat(length).isGreaterThan(0);
        assertThat(message).contains("working");
    }

    @Test
    void shouldVerifyJUnitWorks() {
        // This test verifies JUnit 5 is properly configured
        assertThat(true).isTrue();
    }

    @Test
    void shouldVerifyAssertJWorks() {
        // This test verifies AssertJ fluent assertions work
        assertThat("hello")
            .isNotNull()
            .isNotEmpty()
            .hasSize(5)
            .startsWith("he")
            .endsWith("lo")
            .contains("ll");
    }
}
