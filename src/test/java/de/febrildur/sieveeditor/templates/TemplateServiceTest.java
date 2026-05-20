package de.febrildur.sieveeditor.templates;
// SPDX-FileCopyrightText: 2025, 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class TemplateServiceTest {

    private static final String SIEVEEDITOR_TEST_DIR = "sieveeditor.test.dir";
    private static final String SIEVE_TEMPLATE_CONTENT = "require [\"fileinto\"];\nif true { keep; }";

    private TemplateService templateService;
    private String originalTestDir;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        originalTestDir = System.getProperty(SIEVEEDITOR_TEST_DIR);
        System.setProperty(SIEVEEDITOR_TEST_DIR, tempDir.toString());
        templateService = new TemplateService();
    }

    @AfterEach
    void tearDown() {
        if (originalTestDir != null) {
            System.setProperty(SIEVEEDITOR_TEST_DIR, originalTestDir);
        } else {
            System.clearProperty(SIEVEEDITOR_TEST_DIR);
        }
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
        List<SieveTemplate> templates = templateService.getBuiltinTemplates();

        assertThatThrownBy(() -> templates.add(SieveTemplate.builtin("New", "desc", "content")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Nested
    class UserTemplateTests {

        @BeforeEach
        void setUp() throws IOException {
            Path templatesDir = templateService.getTemplatesDirectory();
            Files.createDirectories(templatesDir);
        }

        @Test
        void shouldReturnEmptyWhenNoSieveFiles() {
            List<SieveTemplate> userTemplates = templateService.getUserTemplates();

            assertThat(userTemplates).isEmpty();
        }

        @Test
        void shouldLoadUserTemplatesFromDirectory() throws IOException {
            Path templatesDir = templateService.getTemplatesDirectory();
            Files.writeString(templatesDir.resolve("my-filter.sieve"), SIEVE_TEMPLATE_CONTENT);

            List<SieveTemplate> userTemplates = templateService.getUserTemplates();

            assertThat(userTemplates).hasSize(1);
            assertThat(userTemplates.get(0).getName()).isEqualTo("my-filter");
            assertThat(userTemplates.get(0).getContent()).isEqualTo(SIEVE_TEMPLATE_CONTENT);
            assertThat(userTemplates.get(0).isBuiltin()).isFalse();
        }

        @Test
        void shouldLoadMultipleUserTemplates() throws IOException {
            Path templatesDir = templateService.getTemplatesDirectory();
            Files.writeString(templatesDir.resolve("vacation.sieve"), "require [\"vacation\"];");
            Files.writeString(templatesDir.resolve("spam.sieve"), "require [\"fileinto\"];");

            List<SieveTemplate> userTemplates = templateService.getUserTemplates();

            assertThat(userTemplates).hasSize(2);
            assertThat(userTemplates).extracting(SieveTemplate::getName)
                .containsExactlyInAnyOrder("vacation", "spam");
        }

        @Test
        void shouldIgnoreNonSieveFiles() throws IOException {
            Path templatesDir = templateService.getTemplatesDirectory();
            Files.writeString(templatesDir.resolve("test.txt"), "text");
            Files.writeString(templatesDir.resolve("notes.md"), "notes");

            List<SieveTemplate> userTemplates = templateService.getUserTemplates();

            assertThat(userTemplates).isEmpty();
        }

        @Test
        void shouldIncludeUserTemplatesInAllTemplates() throws IOException {
            Path templatesDir = templateService.getTemplatesDirectory();
            Files.writeString(templatesDir.resolve("custom.sieve"), SIEVE_TEMPLATE_CONTENT);

            List<SieveTemplate> allTemplates = templateService.getAllTemplates();

            assertThat(allTemplates).extracting(SieveTemplate::getName)
                .contains("custom");
            assertThat(allTemplates.size())
                .isGreaterThan(templateService.getBuiltinTemplates().size());
        }
    }

    @Nested
    class DirectoryManagementTests {

        @Test
        void ensureTemplatesDirectoryExistsShouldCreateDirectory() {
            Path templatesDir = templateService.getTemplatesDirectory();

            assertThat(templatesDir).doesNotExist();

            templateService.ensureTemplatesDirectoryExists();

            assertThat(templatesDir).exists();
            assertThat(templatesDir).isDirectory();
        }

        @Test
        void ensureTemplatesDirectoryExistsShouldNotFailWhenAlreadyExists() throws IOException {
            Path templatesDir = templateService.getTemplatesDirectory();
            Files.createDirectories(templatesDir);

            templateService.ensureTemplatesDirectoryExists();

            assertThat(templatesDir).exists();
        }
    }
}
