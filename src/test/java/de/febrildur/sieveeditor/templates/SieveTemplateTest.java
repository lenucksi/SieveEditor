package de.febrildur.sieveeditor.templates;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test suite for SieveTemplate class.
 */
class SieveTemplateTest {

    @Test
    void shouldCreateBuiltinTemplate() {
        // Given
        String name = "Test Template";
        String description = "A test template";
        String content = "require [\"fileinto\"];";

        // When
        SieveTemplate template = SieveTemplate.builtin(name, description, content);

        // Then
        assertThat(template.getName()).isEqualTo(name);
        assertThat(template.getDescription()).isEqualTo(description);
        assertThat(template.getContent()).isEqualTo(content);
        assertThat(template.isBuiltin()).isTrue();
    }

    @Test
    void shouldCreateUserTemplate() {
        // Given
        String name = "My Custom Template";
        String content = "# custom sieve script";

        // When
        SieveTemplate template = SieveTemplate.user(name, content);

        // Then
        assertThat(template.getName()).isEqualTo(name);
        assertThat(template.getDescription()).isEqualTo("User template");
        assertThat(template.getContent()).isEqualTo(content);
        assertThat(template.isBuiltin()).isFalse();
    }

    @Test
    void shouldReturnNameAsToString() {
        // Given
        SieveTemplate template = SieveTemplate.builtin("Spam Filter", "desc", "content");

        // When/Then
        assertThat(template.toString()).isEqualTo("Spam Filter");
    }

    @Test
    void shouldStoreAllFieldsCorrectly() {
        // Given
        String name = "Complete Template";
        String description = "Full description here";
        String content = """
                require ["fileinto", "imap4flags"];

                if header :contains "subject" "test" {
                    setflag "\\\\Flagged";
                    fileinto "Test";
                }
                """;

        // When
        SieveTemplate template = new SieveTemplate(name, description, content, true);

        // Then
        assertThat(template.getName()).isEqualTo(name);
        assertThat(template.getDescription()).isEqualTo(description);
        assertThat(template.getContent()).isEqualTo(content);
        assertThat(template.getContent()).contains("fileinto");
        assertThat(template.getContent()).contains("imap4flags");
    }
}
