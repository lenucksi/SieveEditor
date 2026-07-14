package de.febrildur.sieveeditor.system;
// SPDX-FileCopyrightText: 2026 Claude
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Test suite for SieveCompletionProvider class.
 * Tests autocomplete completions for Sieve script language keywords.
 */
class SieveCompletionProviderTest {

    private SieveCompletionProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SieveCompletionProvider();
    }

    @Test
    void shouldHaveCompletions() {
        // Given - The provider should be populated with completions
        // When - Look up a known keyword
        List<Completion> completions = provider.getCompletionByInputText("if");

        // Then - Should find at least one completion
        assertThat(completions).isNotEmpty();
    }

    @Test
    void shouldFindControlFlowKeywordIf() {
        List<Completion> completions = provider.getCompletionByInputText("if");
        assertThat(completions)
            .withFailMessage("Should find completion for 'if'")
            .isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo("if");
    }

    @Test
    void shouldFindControlFlowKeywordElsif() {
        List<Completion> completions = provider.getCompletionByInputText("elsif");
        assertThat(completions).isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo("elsif");
    }

    @Test
    void shouldFindControlFlowKeywordRequire() {
        List<Completion> completions = provider.getCompletionByInputText("require");
        assertThat(completions).isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo("require");
    }

    @Test
    void shouldFindActionKeywordKeep() {
        List<Completion> completions = provider.getCompletionByInputText("keep");
        assertThat(completions)
            .withFailMessage("Should find completion for 'keep'")
            .isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo("keep");
    }

    @Test
    void shouldFindActionKeywordFileinto() {
        List<Completion> completions = provider.getCompletionByInputText("fileinto");
        assertThat(completions).isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo("fileinto");
    }

    @Test
    void shouldFindActionKeywordVacation() {
        List<Completion> completions = provider.getCompletionByInputText("vacation");
        assertThat(completions).isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo("vacation");
    }

    @Test
    void shouldFindTestKeywordHeader() {
        List<Completion> completions = provider.getCompletionByInputText("header");
        assertThat(completions)
            .withFailMessage("Should find completion for 'header'")
            .isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo("header");
    }

    @Test
    void shouldFindTestKeywordSize() {
        List<Completion> completions = provider.getCompletionByInputText("size");
        assertThat(completions).isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo("size");
    }

    @Test
    void shouldFindTestKeywordExists() {
        List<Completion> completions = provider.getCompletionByInputText("exists");
        assertThat(completions).isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo("exists");
    }

    @Test
    void shouldFindTestKeywordAddress() {
        List<Completion> completions = provider.getCompletionByInputText("address");
        assertThat(completions).isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo("address");
    }

    @Test
    void shouldFindTagContains() {
        List<Completion> completions = provider.getCompletionByInputText(":contains");
        assertThat(completions)
            .withFailMessage("Should find completion for ':contains'")
            .isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo(":contains");
    }

    @Test
    void shouldFindTagIs() {
        List<Completion> completions = provider.getCompletionByInputText(":is");
        assertThat(completions).isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo(":is");
    }

    @Test
    void shouldFindTagOver() {
        List<Completion> completions = provider.getCompletionByInputText(":over");
        assertThat(completions).isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo(":over");
    }

    @Test
    void shouldFindTagUnder() {
        List<Completion> completions = provider.getCompletionByInputText(":under");
        assertThat(completions).isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo(":under");
    }

    @Test
    void shouldFindTagDays() {
        List<Completion> completions = provider.getCompletionByInputText(":days");
        assertThat(completions).isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo(":days");
    }

    @Test
    void shouldFindTagMime() {
        List<Completion> completions = provider.getCompletionByInputText(":mime");
        assertThat(completions).isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo(":mime");
    }

    @Test
    void shouldFindLogicalOperatorAllof() {
        List<Completion> completions = provider.getCompletionByInputText("allof");
        assertThat(completions)
            .withFailMessage("Should find completion for 'allof'")
            .isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo("allof");
    }

    @Test
    void shouldFindLogicalOperatorAnyof() {
        List<Completion> completions = provider.getCompletionByInputText("anyof");
        assertThat(completions).isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo("anyof");
    }

    @Test
    void shouldFindLogicalOperatorNot() {
        List<Completion> completions = provider.getCompletionByInputText("not");
        assertThat(completions).isNotEmpty();
        assertThat(completions.get(0).getInputText()).isEqualTo("not");
    }

    @Test
    void shouldHaveDescriptionForIf() {
        List<Completion> completions = provider.getCompletionByInputText("if");
        assertThat(completions).isNotEmpty();
        Completion completion = completions.get(0);
        assertThat(completion).isInstanceOf(BasicCompletion.class);
        String description = ((BasicCompletion) completion).getShortDescription();
        assertThat(description)
            .withFailMessage("Completion for 'if' should have a short description")
            .isNotNull();
    }

    @Test
    void shouldHaveDescriptionForKeep() {
        List<Completion> completions = provider.getCompletionByInputText("keep");
        assertThat(completions).isNotEmpty();
        Completion completion = completions.get(0);
        assertThat(completion).isInstanceOf(BasicCompletion.class);
        String description = ((BasicCompletion) completion).getShortDescription();

        assertThat(description)
            .withFailMessage("Completion for 'keep' should have a short description")
            .isNotNull();
        assertThat(description).contains("keep");
    }

    @Test
    void shouldHaveDescriptionForHeader() {
        List<Completion> completions = provider.getCompletionByInputText("header");
        assertThat(completions).isNotEmpty();
        Completion completion = completions.get(0);
        assertThat(completion).isInstanceOf(BasicCompletion.class);
        String description = ((BasicCompletion) completion).getShortDescription();
        assertThat(description)
            .withFailMessage("Completion for 'header' should have a short description")
            .isNotNull();
    }

    @Test
    void shouldHaveDescriptionForContains() {
        List<Completion> completions = provider.getCompletionByInputText(":contains");
        assertThat(completions).isNotEmpty();
        Completion completion = completions.get(0);
        assertThat(completion).isInstanceOf(BasicCompletion.class);
        String description = ((BasicCompletion) completion).getShortDescription();
        assertThat(description)
            .withFailMessage("Completion for ':contains' should have a short description")
            .isNotNull();
    }

    @Test
    void shouldHaveDescriptionForAllof() {
        List<Completion> completions = provider.getCompletionByInputText("allof");
        assertThat(completions).isNotEmpty();
        Completion completion = completions.get(0);
        assertThat(completion).isInstanceOf(BasicCompletion.class);
        String description = ((BasicCompletion) completion).getShortDescription();
        assertThat(description)
            .withFailMessage("Completion for 'allof' should have a short description")
            .isNotNull();
    }

    @Test
    void shouldReturnReplacementText() {
        // The replacement text should match the input text
        List<Completion> completions = provider.getCompletionByInputText("if");
        assertThat(completions).isNotEmpty();
        assertThat(completions.get(0).getReplacementText()).isEqualTo("if");
    }

    @Test
    void shouldReturnCompletionForAllControlFlowKeywords() {
        String[] keywords = {"if", "elsif", "else", "stop", "return", "break", "foreverypart", "require"};
        for (String kw : keywords) {
            List<Completion> completions = provider.getCompletionByInputText(kw);
            assertThat(completions)
                .withFailMessage("Should find completion for '%s'", kw)
                .isNotEmpty();
        }
    }

    @Test
    void shouldReturnCompletionForAllActionKeywords() {
        String[] keywords = {"keep", "discard", "fileinto", "redirect", "reject", "ereject",
            "vacation", "notify", "denotify", "addflag", "removeflag", "setflag", "set",
            "include", "global", "mailbox", "duplicate", "cancel", "convert",
            "replace", "enclose", "extracttext", "deleteheader", "addheader"};
        for (String kw : keywords) {
            List<Completion> completions = provider.getCompletionByInputText(kw);
            assertThat(completions)
                .withFailMessage("Should find completion for '%s'", kw)
                .isNotEmpty();
        }
    }

    @Test
    void shouldReturnCompletionForAllTestKeywords() {
        String[] keywords = {"address", "envelope", "exists", "header", "size", "true", "false",
            "body", "date", "currentdate", "environment", "spamtest", "virustest",
            "hasflag", "mailboxexists", "metadata", "metadataexists",
            "servermetadata", "servermetadataexists", "ihave", "regex", "list"};
        for (String kw : keywords) {
            List<Completion> completions = provider.getCompletionByInputText(kw);
            assertThat(completions)
                .withFailMessage("Should find completion for '%s'", kw)
                .isNotEmpty();
        }
    }

    @Test
    void shouldReturnCompletionForLogicalOperators() {
        String[] keywords = {"allof", "anyof", "not"};
        for (String kw : keywords) {
            List<Completion> completions = provider.getCompletionByInputText(kw);
            assertThat(completions)
                .withFailMessage("Should find completion for '%s'", kw)
                .isNotEmpty();
        }
    }

    @Test
    void shouldReturnCompletionForCommonTags() {
        String[] keywords = {":is", ":contains", ":matches", ":regex", ":over", ":under",
            ":copy", ":days", ":mime", ":comparator"};
        for (String kw : keywords) {
            List<Completion> completions = provider.getCompletionByInputText(kw);
            assertThat(completions)
                .withFailMessage("Should find completion for '%s'", kw)
                .isNotEmpty();
        }
    }
}
