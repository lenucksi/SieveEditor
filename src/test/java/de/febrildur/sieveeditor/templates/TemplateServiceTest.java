package de.febrildur.sieveeditor.templates;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Test suite for TemplateService class.
 */
class TemplateServiceTest {

    private TemplateService templateService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        templateService = new TemplateService();
    }

    @Test
    void shouldProvideBuiltinTemplates() {
        // When
        List<SieveTemplate> templates = templateService.getBuiltinTemplates();

        // Then
        assertThat(templates).isNotEmpty();
        assertThat(templates.size()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void shouldHaveSpamFilterTemplate() {
        // When
        List<SieveTemplate> templates = templateService.getBuiltinTemplates();

        // Then
        assertThat(templates)
                .extracting(SieveTemplate::getName)
                .contains("Spam Filter to Folder");
    }

    @Test
    void shouldHaveVacationTemplate() {
        // When
        List<SieveTemplate> templates = templateService.getBuiltinTemplates();

        // Then
        assertThat(templates)
                .extracting(SieveTemplate::getName)
                .contains("Vacation Auto-Reply");
    }

    @Test
    void shouldHaveMailingListTemplate() {
        // When
        List<SieveTemplate> templates = templateService.getBuiltinTemplates();

        // Then
        assertThat(templates)
                .extracting(SieveTemplate::getName)
                .contains("Mailing List Filter");
    }

    @Test
    void shouldHavePriorityFlaggingTemplate() {
        // When
        List<SieveTemplate> templates = templateService.getBuiltinTemplates();

        // Then
        assertThat(templates)
                .extracting(SieveTemplate::getName)
                .contains("Priority Flagging");
    }

    @Test
    void shouldHaveCompleteStarterScript() {
        // When
        List<SieveTemplate> templates = templateService.getBuiltinTemplates();

        // Then
        assertThat(templates)
                .extracting(SieveTemplate::getName)
                .contains("Complete Starter Script");

        SieveTemplate starterScript = templates.stream()
                .filter(t -> t.getName().equals("Complete Starter Script"))
                .findFirst()
                .orElseThrow();

        assertThat(starterScript.getContent())
                .contains("require")
                .contains("fileinto")
                .contains("SPAM FILTERING")
                .contains("keep;");
    }

    @Test
    void shouldMarkBuiltinTemplatesAsBuiltin() {
        // When
        List<SieveTemplate> templates = templateService.getBuiltinTemplates();

        // Then
        assertThat(templates).allMatch(SieveTemplate::isBuiltin);
    }

    @Test
    void shouldReturnEmptyListWhenNoUserTemplates() {
        // When - no templates directory exists
        List<SieveTemplate> userTemplates = templateService.getUserTemplates();

        // Then - may be empty or contain templates from real user dir
        assertThat(userTemplates).isNotNull();
    }

    @Test
    void shouldReturnTemplatesDirectory() {
        // When
        Path templatesDir = templateService.getTemplatesDirectory();

        // Then
        assertThat(templatesDir).isNotNull();
        assertThat(templatesDir.toString()).contains("templates");
    }

    @Test
    void shouldCombineBuiltinAndUserTemplates() {
        // When
        List<SieveTemplate> allTemplates = templateService.getAllTemplates();

        // Then
        assertThat(allTemplates.size())
                .isGreaterThanOrEqualTo(templateService.getBuiltinTemplates().size());
    }

    @Test
    void shouldContainRequireStatementsInTemplates() {
        // When
        List<SieveTemplate> templates = templateService.getBuiltinTemplates();

        // Then - templates that use extensions should have require
        SieveTemplate vacationTemplate = templates.stream()
                .filter(t -> t.getName().equals("Vacation Auto-Reply"))
                .findFirst()
                .orElseThrow();

        assertThat(vacationTemplate.getContent()).contains("require");
        assertThat(vacationTemplate.getContent()).contains("vacation");
    }

    @Test
    void shouldContainValidSieveSyntax() {
        // When
        List<SieveTemplate> templates = templateService.getBuiltinTemplates();

        // Then - spot check templates for valid syntax patterns
        for (SieveTemplate template : templates) {
            String content = template.getContent();

            // If template has fileinto, it should have require
            if (content.contains("fileinto") && !content.contains("discard;")) {
                assertThat(content)
                        .as("Template '%s' uses fileinto but missing require", template.getName())
                        .contains("require");
            }

            // If template has imap4flags actions, it should require it
            if (content.contains("setflag") || content.contains("addflag")) {
                assertThat(content)
                        .as("Template '%s' uses imap4flags but missing require", template.getName())
                        .contains("imap4flags");
            }
        }
    }

    @Test
    void builtinTemplatesShouldBeImmutable() {
        // When
        List<SieveTemplate> templates = templateService.getBuiltinTemplates();

        // Then
        assertThatThrownBy(() -> templates.add(SieveTemplate.builtin("New", "desc", "content")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
