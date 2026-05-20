package de.febrildur.sieveeditor.ui;

import de.febrildur.sieveeditor.parser.SieveRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class RuleNavigatorPanelTest {

    private RuleNavigatorPanel navigator;

    @BeforeEach
    void setUp() {
        navigator = new RuleNavigatorPanel();
    }

    @Test
    void shouldCreateNavigatorWithBorder() {
        assertThat(navigator.getBorder()).isNotNull();
        assertThat(navigator.getBorder()).isInstanceOf(TitledBorder.class);
        TitledBorder border = (TitledBorder) navigator.getBorder();
        assertThat(border.getTitle()).isEqualTo("Script Rules");
    }

    @Test
    void shouldUpdateRulesFromScript() {
        String script = """
                ## Flag: |UniqueId:1|Rulename: Spam Filter
                if header :contains "X-Spam-Flag" "YES" {
                    fileinto "Spam";
                }
                ## Flag: |UniqueId:2|Rulename: Vacation Rule
                vacation :days 7 "Out of office";
                """;
        navigator.updateRules(script);
    }

    @Test
    void shouldHandleEmptyScript() {
        navigator.updateRules("");
    }

    @Test
    void shouldHandleNullScript() {
        navigator.updateRules(null);
    }

    @Test
    void shouldHandleWhitespaceOnlyScript() {
        navigator.updateRules("   \n  \n  ");
    }

    @Test
    void shouldClearAllData() {
        navigator.updateRules("# test");
        navigator.clear();
        assertThat(navigator.getRecommendedWidth()).isEqualTo(200);
    }

    @Test
    void shouldSetJumpToLineCallback() {
        AtomicInteger captured = new AtomicInteger(-1);
        navigator.setJumpToLineCallback(captured::set);
        navigator.setJumpToLineCallback(line -> {});
    }

    @Test
    void shouldReturnDefaultWidthWhenEmpty() {
        assertThat(navigator.getRecommendedWidth()).isBetween(150, 400);
    }

    @Test
    void shouldReturnRecommendedWidthAfterUpdate() {
        String script = """
                ## Flag: |UniqueId:1|Rulename: Very Long Rule Name For Testing Width Calculation
                if true { stop; }
                """;
        navigator.updateRules(script);
        int width = navigator.getRecommendedWidth();
        assertThat(width).isBetween(150, 400);
    }

    @Test
    void shouldReportNotAutoSizedInitially() {
        assertThat(navigator.isWidthAutoSized()).isFalse();
    }

    @Test
    void shouldMarkWidthAutoSized() {
        navigator.markWidthAutoSized();
        assertThat(navigator.isWidthAutoSized()).isTrue();
    }

    @Test
    void shouldHandleUpdateWithManyRules() {
        StringBuilder script = new StringBuilder();
        for (int i = 1; i <= 20; i++) {
            script.append("## Flag: |UniqueId:").append(i)
                    .append("|Rulename: Rule ").append(i).append("\n");
            script.append("if true { stop; }\n");
        }
        navigator.updateRules(script.toString());
        assertThat(navigator.getRecommendedWidth()).isGreaterThan(0);
    }

    @Test
    void shouldHandleReapplyWarningPanelSizeWhenNoWarnings() {
        navigator.reapplyWarningPanelSize();
    }

    @Test
    void shouldHandleUpdateWithDuplicates() {
        String script = """
                ## Flag: |UniqueId:1|Rulename: First
                if true { stop; }
                ## Flag: |UniqueId:1|Rulename: Second
                if true { stop; }
                """;
        navigator.updateRules(script);
    }

    @Test
    void shouldHandleUpdateWithGaps() {
        String script = """
                ## Flag: |UniqueId:1|Rulename: First
                if true { stop; }
                ## Flag: |UniqueId:5|Rulename: Fifth
                if true { stop; }
                """;
        navigator.updateRules(script);
    }

    @Test
    void shouldBeResizable() {
        navigator.setPreferredSize(new Dimension(300, 500));
        navigator.doLayout();
        assertThat(navigator.getPreferredSize().width).isEqualTo(300);
    }
}
