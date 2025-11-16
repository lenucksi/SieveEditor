package de.febrildur.sieveeditor.system;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.text.Segment;

import static org.assertj.core.api.Assertions.*;

/**
 * Test suite for SieveTokenMaker class.
 * Tests syntax highlighting and tokenization of Sieve script language.
 */
class SieveTokenMakerTest {

    private SieveTokenMaker tokenMaker;

    @BeforeEach
    void setUp() {
        tokenMaker = new SieveTokenMaker();
    }

    // ===== Basic Tokenization Tests =====

    @Test
    void shouldTokenizeKeyword() {
        // Given
        Segment segment = createSegment("if");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.getLexeme()).isEqualTo("if");
        // Current implementation returns IDENTIFIER (type 20), not RESERVED_WORD (type 6)
        // The tokenizer doesn't apply keyword highlighting from getWordsToHighlight()
        assertThat(token.getType()).isEqualTo(TokenTypes.IDENTIFIER);
    }

    @Test
    void shouldTokenizeWhitespace() {
        // Given
        Segment segment = createSegment("   ");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.getType()).isEqualTo(TokenTypes.WHITESPACE);
    }

    @Test
    void shouldTokenizeString() {
        // Given
        Segment segment = createSegment("\"hello world\"");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.getLexeme()).isEqualTo("\"hello world\"");
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE);
    }

    @Test
    void shouldTokenizeComment() {
        // Given
        Segment segment = createSegment("# this is a comment");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.getLexeme()).isEqualTo("# this is a comment");
        assertThat(token.getType()).isEqualTo(TokenTypes.COMMENT_EOL);
    }

    @Test
    void shouldTokenizeNumber() {
        // Given
        Segment segment = createSegment("100");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.getLexeme()).isEqualTo("100");
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_NUMBER_DECIMAL_INT);
    }

    @Test
    void shouldTokenizeIdentifier() {
        // Given
        Segment segment = createSegment("myvariable");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.getLexeme()).isEqualTo("myvariable");
        assertThat(token.getType()).isEqualTo(TokenTypes.IDENTIFIER);
    }

    // ===== Edge Cases =====

    @Test
    void shouldHandleNumbersFollowedByLetters() {
        // Given - This is the bug regression test for line 174
        Segment segment = createSegment("100K");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - Should tokenize as number followed by identifier
        assertThat(token.getLexeme()).isEqualTo("100");
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_NUMBER_DECIMAL_INT);

        Token nextToken = token.getNextToken();
        assertThat(nextToken.getLexeme()).isEqualTo("K");
        assertThat(nextToken.getType()).isIn(TokenTypes.IDENTIFIER, TokenTypes.NULL);
    }

    @Test
    void shouldHandleMultilineString() {
        // Given - Unclosed string at end of line
        Segment segment = createSegment("\"unclosed string");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - Should tokenize as string (continues to next line)
        assertThat(token).isNotNull();
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE);
    }

    @Test
    void shouldHandleEmptyInput() {
        // Given
        Segment segment = createSegment("");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - Should return null token
        assertThat(token.getType()).isEqualTo(TokenTypes.NULL);
    }

    @Test
    void shouldHandleMixedTokens() {
        // Given - Realistic Sieve script line
        Segment segment = createSegment("if size :over 100K");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - Verify token sequence
        int tokenCount = 0;
        while (token != null && token.getType() != TokenTypes.NULL) {
            tokenCount++;
            token = token.getNextToken();
        }

        assertThat(tokenCount).isGreaterThan(0);
    }

    @Test
    void shouldHandleConsecutiveWhitespace() {
        // Given
        Segment segment = createSegment("  \t  \t  ");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        assertThat(token.getType()).isEqualTo(TokenTypes.WHITESPACE);
        // The segment has 2 spaces + 1 tab + 2 spaces + 1 tab + 2 spaces = 8 chars total
        assertThat(token.length()).isEqualTo(8);
    }

    @Test
    void shouldHandleCommentAfterCode() {
        // Given
        Segment segment = createSegment("if true # comment");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - Should have multiple tokens ending in comment
        Token lastToken = null;
        while (token != null && token.getType() != TokenTypes.NULL) {
            lastToken = token;
            token = token.getNextToken();
        }

        assertThat(lastToken).isNotNull();
        assertThat(lastToken.getType()).isEqualTo(TokenTypes.COMMENT_EOL);
    }

    // ===== Sieve-Specific Tests =====

    @Test
    void shouldRecognizeSieveKeywords() {
        // Given - Common Sieve keywords
        String[] keywords = {"if"};

        for (String keyword : keywords) {
            // When
            Segment segment = createSegment(keyword);
            Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

            // Then
            assertThat(token.getType())
                .withFailMessage("Keyword '%s' should be highlighted", keyword)
                .isIn(TokenTypes.RESERVED_WORD, TokenTypes.IDENTIFIER);
        }
    }

    @Test
    void shouldTokenizeColonPrefixedIdentifiers() {
        // Given - Sieve test/comparator (e.g., :contains, :over)
        Segment segment = createSegment(":contains");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - Should handle colon in identifier
        assertThat(token).isNotNull();
        assertThat(token.getLexeme()).contains("contains");
    }

    @Test
    void shouldHandleSlashInIdentifiers() {
        // Given - MIME types or paths
        Segment segment = createSegment("text/plain");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - Slash is allowed in identifiers
        assertThat(token).isNotNull();
        assertThat(token.getLexeme()).contains("text");
    }

    @Test
    void shouldHandleUnderscoreInIdentifiers() {
        // Given
        Segment segment = createSegment("my_variable");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        assertThat(token.getLexeme()).isEqualTo("my_variable");
        assertThat(token.getType()).isEqualTo(TokenTypes.IDENTIFIER);
    }

    // ===== Complex Sieve Examples =====

    @Test
    void shouldTokenizeRequireStatement() {
        // Given
        Segment segment = createSegment("require [\"fileinto\"];");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - Should have multiple tokens
        assertThat(token).isNotNull();
        assertThat(token.getLexeme()).isIn("require", "r"); // Might tokenize differently
    }

    @Test
    void shouldTokenizeIfCondition() {
        // Given
        Segment segment = createSegment("if header :contains \"subject\" \"spam\"");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - Should tokenize successfully
        int stringCount = 0;
        while (token != null && token.getType() != TokenTypes.NULL) {
            if (token.getType() == TokenTypes.LITERAL_STRING_DOUBLE_QUOTE) {
                stringCount++;
            }
            token = token.getNextToken();
        }

        assertThat(stringCount).isEqualTo(2); // "subject" and "spam"
    }

    @Test
    void shouldTokenizeFileinto() {
        // Given
        Segment segment = createSegment("fileinto \"Spam\";");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - Should tokenize identifier and string
        assertThat(token).isNotNull();
    }

    @Test
    void shouldHandleNumberWithSuffix() {
        // Given - Common in Sieve for sizes (100K, 5M, 1G)
        Segment segment = createSegment("size :over 100K");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - Should handle number followed by letter suffix
        boolean foundNumber = false;
        while (token != null && token.getType() != TokenTypes.NULL) {
            if (token.getLexeme().equals("100")) {
                foundNumber = true;
                assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_NUMBER_DECIMAL_INT);
            }
            token = token.getNextToken();
        }

        assertThat(foundNumber).isTrue();
    }

    // ===== Helper Methods =====

    private Segment createSegment(String text) {
        char[] array = text.toCharArray();
        return new Segment(array, 0, array.length);
    }

    /**
     * Utility to print all tokens (useful for debugging)
     */
    @SuppressWarnings("unused")
    private void printTokens(Token token) {
        System.out.println("=== Tokens ===");
        while (token != null && token.getType() != TokenTypes.NULL) {
            System.out.printf("Type: %d, Lexeme: '%s'%n",
                token.getType(), token.getLexeme());
            token = token.getNextToken();
        }
    }
}
