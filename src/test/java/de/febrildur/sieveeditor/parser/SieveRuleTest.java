package de.febrildur.sieveeditor.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SieveRuleTest {

    @Test
    void shouldCreateRuleWithMinimalConstructor() {
        SieveRule rule = new SieveRule(1, "Spam Filter", 5, "## Flag: |UniqueId:1 |Rulename: Spam Filter");

        assertThat(rule.getRuleNumber()).isEqualTo(1);
        assertThat(rule.getLabel()).isEqualTo("Spam Filter");
        assertThat(rule.getLineNumber()).isEqualTo(5);
        assertThat(rule.getComment()).isEqualTo("## Flag: |UniqueId:1 |Rulename: Spam Filter");
        assertThat(rule.getLastModified()).isNull();
        assertThat(rule.getModifiedBy()).isNull();
        assertThat(rule.getFlag()).isNull();
    }

    @Test
    void shouldCreateRuleWithFullConstructor() {
        SieveRule rule = new SieveRule(2, "Vacation", 10, "comment", "2025-12-18T13:54:27Z", "192.168.1.1", "vacation");

        assertThat(rule.getRuleNumber()).isEqualTo(2);
        assertThat(rule.getLabel()).isEqualTo("Vacation");
        assertThat(rule.getLineNumber()).isEqualTo(10);
        assertThat(rule.getComment()).isEqualTo("comment");
        assertThat(rule.getLastModified()).isEqualTo("2025-12-18T13:54:27Z");
        assertThat(rule.getModifiedBy()).isEqualTo("192.168.1.1");
        assertThat(rule.getFlag()).isEqualTo("vacation");
    }

    @Test
    void shouldHandleNullLabel() {
        SieveRule rule = new SieveRule(3, null, 1, "comment");

        assertThat(rule.getLabel()).isNull();
    }

    @Test
    void shouldHandleNullComment() {
        SieveRule rule = new SieveRule(1, "label", 1, null);

        assertThat(rule.getComment()).isNull();
    }

    @Test
    void shouldHandleNegativeRuleNumber() {
        SieveRule rule = new SieveRule(-1, "test", 0, "comment");

        assertThat(rule.getRuleNumber()).isEqualTo(-1);
    }

    @Test
    void shouldHandleZeroLineNumber() {
        SieveRule rule = new SieveRule(1, "test", 0, "comment");

        assertThat(rule.getLineNumber()).isZero();
    }

    @Test
    void shouldDetectMetadataWhenLastModifiedPresent() {
        SieveRule rule = new SieveRule(1, "test", 1, "c", "2025-01-01", null, null);

        assertThat(rule.hasMetadata()).isTrue();
    }

    @Test
    void shouldDetectMetadataWhenModifiedByPresent() {
        SieveRule rule = new SieveRule(1, "test", 1, "c", null, "192.168.1.1", null);

        assertThat(rule.hasMetadata()).isTrue();
    }

    @Test
    void shouldDetectMetadataWhenBothPresent() {
        SieveRule rule = new SieveRule(1, "test", 1, "c", "2025-01-01", "192.168.1.1", null);

        assertThat(rule.hasMetadata()).isTrue();
    }

    @Test
    void shouldNotDetectMetadataWhenBothNull() {
        SieveRule rule = new SieveRule(1, "test", 1, "c", null, null, null);

        assertThat(rule.hasMetadata()).isFalse();
    }

    @Test
    void shouldGenerateDisplayTextWithLabel() {
        SieveRule rule = new SieveRule(5, "My Rule", 1, "comment");

        assertThat(rule.getDisplayText()).isEqualTo("Rule 5: My Rule");
    }

    @Test
    void shouldGenerateDisplayTextWithNullLabel() {
        SieveRule rule = new SieveRule(3, null, 1, "comment");

        assertThat(rule.getDisplayText()).isEqualTo("Rule 3");
    }

    @Test
    void shouldGenerateDisplayTextWithEmptyLabel() {
        SieveRule rule = new SieveRule(7, "", 1, "comment");

        assertThat(rule.getDisplayText()).isEqualTo("Rule 7");
    }

    @Test
    void shouldGenerateDisplayTextWithBlankLabel() {
        SieveRule rule = new SieveRule(2, "  ", 1, "comment");

        assertThat(rule.getDisplayText()).isEqualTo("Rule 2:   ");
    }

    @Test
    void toStringShouldReturnDisplayText() {
        SieveRule rule = new SieveRule(1, "Test", 1, "comment");

        assertThat(rule.toString()).isEqualTo("Rule 1: Test");
        assertThat(rule.toString()).isEqualTo(rule.getDisplayText());
    }

    @Test
    void toStringShouldMatchDisplayTextForNullLabel() {
        SieveRule rule = new SieveRule(9, null, 1, "comment");

        assertThat(rule.toString()).isEqualTo(rule.getDisplayText());
    }

    @Test
    void shouldGetFlag() {
        SieveRule rule = new SieveRule(1, "test", 1, "c", null, null, "syscategory");

        assertThat(rule.getFlag()).isEqualTo("syscategory");
    }

    @Test
    void shouldGetNullFlag() {
        SieveRule rule = new SieveRule(1, "test", 1, "c");

        assertThat(rule.getFlag()).isNull();
    }
}
