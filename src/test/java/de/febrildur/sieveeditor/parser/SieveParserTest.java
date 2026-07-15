package de.febrildur.sieveeditor.parser;

import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SieveParserTest {

    private SieveParser parser;

    @BeforeEach
    void setUp() {
        parser = new SieveParser();
    }

    private RSyntaxDocument createDocument(String text) {
        RSyntaxTextArea textArea = new RSyntaxTextArea(text);
        return (RSyntaxDocument) textArea.getDocument();
    }

    @Test
    void noErrorsForEmptyScript() {
        RSyntaxDocument doc = createDocument("");
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).isEmpty();
    }

    @Test
    void noErrorsForSimpleIfBlock() {
        RSyntaxDocument doc = createDocument("if true {\n    keep;\n}\n");
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).isEmpty();
    }

    @Test
    void detectsUnmatchedOpeningBrace() {
        RSyntaxDocument doc = createDocument("if true {\n    keep;\n");
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).hasSize(1);
        assertThat(result.getNotices().get(0).getMessage()).contains("Unmatched opening brace");
    }

    @Test
    void detectsUnmatchedClosingBrace() {
        RSyntaxDocument doc = createDocument("}\n");
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).hasSize(1);
        assertThat(result.getNotices().get(0).getMessage()).contains("Unmatched closing brace");
    }

    @Test
    void detectsUnmatchedOpeningParen() {
        RSyntaxDocument doc = createDocument("if allof(condition1, condition2 {\n    keep;\n}\n");
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).isNotEmpty();
    }

    @Test
    void detectsUnmatchedClosingParen() {
        RSyntaxDocument doc = createDocument("if true) {\n    keep;\n}\n");
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).hasSize(1);
        assertThat(result.getNotices().get(0).getMessage()).contains("Unmatched closing parenthesis");
    }

    @Test
    void detectsUnmatchedOpeningBracket() {
        RSyntaxDocument doc = createDocument("require [\"fileinto\"\n");
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).hasSize(1);
        assertThat(result.getNotices().get(0).getMessage()).contains("Unmatched opening bracket");
    }

    @Test
    void detectsUnmatchedClosingBracket() {
        RSyntaxDocument doc = createDocument("require \"fileinto\"]\n");
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).hasSize(1);
        assertThat(result.getNotices().get(0).getMessage()).contains("Unmatched closing bracket");
    }

    @Test
    void noErrorsForRequireWithBrackets() {
        RSyntaxDocument doc = createDocument("require [\"fileinto\", \"vacation\"];\n");
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).isEmpty();
    }

    @Test
    void noErrorsForNestedBlocks() {
        RSyntaxDocument doc = createDocument(
            "if header :contains \"subject\" \"spam\" {\n" +
            "    if size :over 100K {\n" +
            "        discard;\n" +
            "    }\n" +
            "}\n"
        );
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).isEmpty();
    }

    @Test
    void noErrorsForVacationWithHeredoc() {
        RSyntaxDocument doc = createDocument(
            "vacation :days 7 :mime text:\n" +
            "Hi, I'm on vacation.\n" +
            ".\n"
        );
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).isEmpty();
    }

    @Test
    void detectsUnclosedString() {
        RSyntaxDocument doc = createDocument("if header :contains \"unclosed\n");
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).hasSize(1);
        assertThat(result.getNotices().get(0).getMessage()).contains("Unclosed string");
    }

    @Test
    void noErrorsForProperStringEscaping() {
        RSyntaxDocument doc = createDocument("if header :contains \"quote\\\"here\" {\n    keep;\n}\n");
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).isEmpty();
    }

    @Test
    void noErrorsForCommentOnly() {
        RSyntaxDocument doc = createDocument("# just a comment\n");
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).isEmpty();
    }

    @Test
    void detectsMultipleErrors() {
        RSyntaxDocument doc = createDocument("if true {\n    keep;\n\n");
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).hasSize(1);
    }

    @Test
    void noErrorsForMatchingBracesAndParens() {
        RSyntaxDocument doc = createDocument(
            "require [\"fileinto\"];\n" +
            "if anyof (header :contains \"list-id\" \"mailinglist\",\n" +
            "          header :contains \"subject\" \"spam\") {\n" +
            "    fileinto \"Spam\";\n" +
            "}\n"
        );
        ParseResult result = parser.parse(doc, "text/sieve");
        assertThat(result.getNotices()).isEmpty();
    }

    @Test
    void parserIsEnabledByDefault() {
        assertThat(parser.isEnabled()).isTrue();
    }
}
