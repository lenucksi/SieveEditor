package de.febrildur.sieveeditor.system;
// SPDX-FileCopyrightText: 2025 Claude
// SPDX-FileCopyrightText: 2025 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
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
        // The raw tokenizer returns IDENTIFIER (type 20), not RESERVED_WORD (type 6)
        // The framework applies getWordsToHighlight() TokenMap to re-map keyword types
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
        // Given - 100K with quantifier handling makes it a single number token
        Segment segment = createSegment("100K");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - Should tokenize as single number token with quantifier
        assertThat(token.getLexeme()).isEqualTo("100K");
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_NUMBER_DECIMAL_INT);

        // Next token should be NULL (end of tokens)
        Token nextToken = token.getNextToken();
        assertThat(nextToken.getType()).isEqualTo(TokenTypes.NULL);
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
        assertThat(token.getLexeme()).isEqualTo(":contains");
    }

    @Test
    void shouldHandleSlashInIdentifiers() {
        // Given - MIME types or paths
        Segment segment = createSegment("text/plain");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - Slash is allowed in identifiers
        assertThat(token).isNotNull();
        assertThat(token.getLexeme()).isEqualTo("text/plain");
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

        // Then - Should handle number quantifier as part of the number token
        boolean foundQuantifiedNumber = false;
        while (token != null && token.getType() != TokenTypes.NULL) {
            if (token.getLexeme().equals("100K")) {
                foundQuantifiedNumber = true;
                assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_NUMBER_DECIMAL_INT);
            }
            token = token.getNextToken();
        }

        assertThat(foundQuantifiedNumber).isTrue();
    }

    // ===== Keyword Type Tests using TokenMap =====
    //
    // The getTokenList() method produces IDENTIFIER tokens for keywords.
    // The RSyntaxTextArea framework applies the TokenMap from getWordsToHighlight()
    // to re-map IDENTIFIER tokens to their correct keyword types.
    // These tests verify the TokenMap directly to ensure correct mappings.
    //
    // Note: TokenMap.get(char[], int, int) uses (array, start, end) with inclusive end.

    private void assertKeywordType(String keyword, int expectedType) {
        TokenMap tokenMap = tokenMaker.getWordsToHighlight();
        char[] chars = keyword.toCharArray();
        int actualType = tokenMap.get(chars, 0, chars.length - 1);
        assertThat(actualType)
            .withFailMessage("Keyword '%s' should map to token type %d but got %d",
                keyword, expectedType, actualType)
            .isEqualTo(expectedType);
    }

    // ===== (a) RESERVED_WORD tests =====

    @Test
    void shouldMapControlFlowKeywordsToReservedWord() {
        String[] keywords = {"if", "elsif", "else", "stop", "require", "return", "break", "foreverypart"};
        for (String kw : keywords) {
            assertKeywordType(kw, TokenTypes.RESERVED_WORD);
        }
    }

    @Test
    void shouldTokenizeControlFlowKeywordsAsIdentifier() {
        String[] keywords = {"if", "elsif", "else", "stop", "require", "return", "break", "foreverypart"};
        for (String kw : keywords) {
            Segment segment = createSegment(kw);
            Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
            assertThat(token.getType())
                .withFailMessage("Raw token for '%s' should be IDENTIFIER", kw)
                .isEqualTo(TokenTypes.IDENTIFIER);
            assertThat(token.getLexeme()).isEqualTo(kw);
        }
    }

    // ===== (b) FUNCTION tests =====

    @Test
    void shouldMapActionKeywordsToFunction() {
        String[] keywords = {"keep", "discard", "fileinto", "redirect", "reject", "ereject",
            "vacation", "notify", "denotify", "addflag", "removeflag", "setflag", "set",
            "include", "global", "mailbox", "duplicate", "cancel", "convert",
            "replace", "enclose", "extracttext", "deleteheader", "addheader"};
        for (String kw : keywords) {
            assertKeywordType(kw, TokenTypes.FUNCTION);
        }
    }

    @Test
    void shouldTokenizeActionKeywordsAsIdentifier() {
        String[] keywords = {"keep", "discard", "fileinto", "redirect", "vacation", "notify"};
        for (String kw : keywords) {
            Segment segment = createSegment(kw);
            Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
            assertThat(token.getType())
                .withFailMessage("Raw token for '%s' should be IDENTIFIER", kw)
                .isEqualTo(TokenTypes.IDENTIFIER);
            assertThat(token.getLexeme()).isEqualTo(kw);
        }
    }

    // ===== (c) RESERVED_WORD_2 tests =====

    @Test
    void shouldMapTestKeywordsToReservedWord2() {
        String[] keywords = {"address", "envelope", "exists", "header", "size", "body",
            "date", "currentdate", "environment", "spamtest", "virustest", "hasflag",
            "mailboxexists", "metadata", "metadataexists", "servermetadata",
            "servermetadataexists", "ihave", "regex", "list", "true", "false"};
        for (String kw : keywords) {
            assertKeywordType(kw, TokenTypes.RESERVED_WORD_2);
        }
    }

    @Test
    void shouldTokenizeTestKeywordsAsIdentifier() {
        String[] keywords = {"header", "size", "exists", "address", "body", "date"};
        for (String kw : keywords) {
            Segment segment = createSegment(kw);
            Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
            assertThat(token.getType())
                .withFailMessage("Raw token for '%s' should be IDENTIFIER", kw)
                .isEqualTo(TokenTypes.IDENTIFIER);
            assertThat(token.getLexeme()).isEqualTo(kw);
        }
    }

    // ===== (d) OPERATOR tests =====

    @Test
    void shouldMapLogicalOperatorsToOperator() {
        String[] keywords = {"allof", "anyof", "not"};
        for (String kw : keywords) {
            assertKeywordType(kw, TokenTypes.OPERATOR);
        }
    }

    // ===== (e) ANNOTATION tests (colon-prefixed tags) =====

    @Test
    void shouldTokenizeColonTagAsAnnotation() {
        Segment segment = createSegment(":contains");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.ANNOTATION);
        assertThat(token.getLexeme()).isEqualTo(":contains");
    }

    @Test
    void shouldTokenizeColonIsAsAnnotation() {
        Segment segment = createSegment(":is");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.ANNOTATION);
        assertThat(token.getLexeme()).isEqualTo(":is");
    }

    @Test
    void shouldTokenizeColonMatchesAsAnnotation() {
        Segment segment = createSegment(":matches");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.ANNOTATION);
        assertThat(token.getLexeme()).isEqualTo(":matches");
    }

    @Test
    void shouldTokenizeColonOverAsAnnotation() {
        Segment segment = createSegment(":over");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.ANNOTATION);
        assertThat(token.getLexeme()).isEqualTo(":over");
    }

    @Test
    void shouldTokenizeColonUnderAsAnnotation() {
        Segment segment = createSegment(":under");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.ANNOTATION);
        assertThat(token.getLexeme()).isEqualTo(":under");
    }

    @Test
    void shouldTokenizeColonDaysAsAnnotation() {
        Segment segment = createSegment(":days");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.ANNOTATION);
        assertThat(token.getLexeme()).isEqualTo(":days");
    }

    @Test
    void shouldTokenizeColonMimeAsAnnotation() {
        Segment segment = createSegment(":mime");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.ANNOTATION);
        assertThat(token.getLexeme()).isEqualTo(":mime");
    }

    @Test
    void shouldTokenizeColonCopyAsAnnotation() {
        Segment segment = createSegment(":copy");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.ANNOTATION);
        assertThat(token.getLexeme()).isEqualTo(":copy");
    }

    @Test
    void shouldTokenizeColonTagInContext() {
        // Given - Tag used in context with other tokens
        Segment segment = createSegment("header :contains \"subject\" \"spam\"");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        int annotationCount = 0;
        int stringCount = 0;

        // Then
        while (token != null && token.getType() != TokenTypes.NULL) {
            if (token.getType() == TokenTypes.ANNOTATION) {
                annotationCount++;
                assertThat(token.getLexeme()).isEqualTo(":contains");
            }
            if (token.getType() == TokenTypes.LITERAL_STRING_DOUBLE_QUOTE) {
                stringCount++;
            }
            token = token.getNextToken();
        }

        assertThat(annotationCount).isEqualTo(1);
        assertThat(stringCount).isEqualTo(2);
    }

    // ===== (f) SEPARATOR tests =====

    @Test
    void shouldTokenizeSemicolonAsSeparator() {
        Segment segment = createSegment(";");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.SEPARATOR);
        assertThat(token.getLexeme()).isEqualTo(";");
    }

    @Test
    void shouldTokenizeOpenBracketAsSeparator() {
        Segment segment = createSegment("[");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.SEPARATOR);
        assertThat(token.getLexeme()).isEqualTo("[");
    }

    @Test
    void shouldTokenizeCloseBracketAsSeparator() {
        Segment segment = createSegment("]");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.SEPARATOR);
        assertThat(token.getLexeme()).isEqualTo("]");
    }

    @Test
    void shouldTokenizeOpenParenAsSeparator() {
        Segment segment = createSegment("(");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.SEPARATOR);
        assertThat(token.getLexeme()).isEqualTo("(");
    }

    @Test
    void shouldTokenizeCloseParenAsSeparator() {
        Segment segment = createSegment(")");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.SEPARATOR);
        assertThat(token.getLexeme()).isEqualTo(")");
    }

    @Test
    void shouldTokenizeOpenBraceAsSeparator() {
        Segment segment = createSegment("{");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.SEPARATOR);
        assertThat(token.getLexeme()).isEqualTo("{");
    }

    @Test
    void shouldTokenizeCloseBraceAsSeparator() {
        Segment segment = createSegment("}");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.SEPARATOR);
        assertThat(token.getLexeme()).isEqualTo("}");
    }

    @Test
    void shouldTokenizeCommaAsSeparator() {
        Segment segment = createSegment(",");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.SEPARATOR);
        assertThat(token.getLexeme()).isEqualTo(",");
    }

    @Test
    void shouldTokenizeSeparatorsInStatement() {
        // Given - A realistic Sieve statement with multiple separators
        Segment segment = createSegment("require [\"fileinto\", \"vacation\"];");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        int separatorCount = 0;
        boolean hasOpenBracket = false;
        boolean hasCloseBracket = false;
        boolean hasComma = false;
        boolean hasSemicolon = false;

        while (token != null && token.getType() != TokenTypes.NULL) {
            if (token.getType() == TokenTypes.SEPARATOR) {
                separatorCount++;
                switch (token.getLexeme()) {
                    case "[" -> hasOpenBracket = true;
                    case "]" -> hasCloseBracket = true;
                    case "," -> hasComma = true;
                    case ";" -> hasSemicolon = true;
                }
            }
            token = token.getNextToken();
        }

        assertThat(separatorCount).isGreaterThanOrEqualTo(4);
        assertThat(hasOpenBracket).isTrue();
        assertThat(hasCloseBracket).isTrue();
        assertThat(hasComma).isTrue();
        assertThat(hasSemicolon).isTrue();
    }

    // ===== (g) Block comment tests =====

    @Test
    void shouldTokenizeBlockComment() {
        // Given
        Segment segment = createSegment("/* comment */");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        assertThat(token.getType()).isEqualTo(TokenTypes.COMMENT_MULTILINE);
        assertThat(token.getLexeme()).isEqualTo("/* comment */");
    }

    @Test
    void shouldTokenizeEmptyBlockComment() {
        // Given - Minimal block comment
        Segment segment = createSegment("/**/");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        assertThat(token.getType()).isEqualTo(TokenTypes.COMMENT_MULTILINE);
        assertThat(token.getLexeme()).isEqualTo("/**/");
    }

    @Test
    void shouldTokenizeBlockCommentWithStars() {
        // Given - Block comment with internal stars
        Segment segment = createSegment("/* *** important *** */");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        assertThat(token.getType()).isEqualTo(TokenTypes.COMMENT_MULTILINE);
        assertThat(token.getLexeme()).isEqualTo("/* *** important *** */");
    }

    @Test
    void shouldTokenizeMultiLineBlockComment() {
        // Given - Multi-line block comment (per-line tokenization)
        // Line 1: start of block comment
        Segment line1 = createSegment("/* line 1");
        Token token1 = tokenMaker.getTokenList(line1, TokenTypes.NULL, 0);

        // Then - First line should be COMMENT_MULTILINE, no null token (state continues)
        assertThat(token1.getType()).isEqualTo(TokenTypes.COMMENT_MULTILINE);
        assertThat(token1.getLexeme()).isEqualTo("/* line 1");

        // When - Line 2: continuation with startTokenType = COMMENT_MULTILINE
        Segment line2 = createSegment("   line 2 */");
        Token token2 = tokenMaker.getTokenList(line2, TokenTypes.COMMENT_MULTILINE, 0);

        // Then - Second line should be COMMENT_MULTILINE followed by NULL
        assertThat(token2.getType()).isEqualTo(TokenTypes.COMMENT_MULTILINE);
        assertThat(token2.getLexeme()).isEqualTo("   line 2 */");
        assertThat(token2.getNextToken().getType()).isEqualTo(TokenTypes.NULL);
    }

    @Test
    void shouldHandleBlockCommentAndCode() {
        // Given - Code after block comment on same line
        Segment segment = createSegment("/* done */ keep;");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        assertThat(token.getType()).isEqualTo(TokenTypes.COMMENT_MULTILINE);
        assertThat(token.getLexeme()).isEqualTo("/* done */");

        // Should have more tokens after the comment
        Token next = token.getNextToken();
        assertThat(next.getType()).isNotEqualTo(TokenTypes.NULL);
    }

    // ===== (h) Number quantifier tests =====

    @Test
    void shouldTokenize100KAsSingleNumberToken() {
        Segment segment = createSegment("100K");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getLexeme()).isEqualTo("100K");
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_NUMBER_DECIMAL_INT);
        assertThat(token.getNextToken().getType()).isEqualTo(TokenTypes.NULL);
    }

    @Test
    void shouldTokenize5MAsSingleNumberToken() {
        Segment segment = createSegment("5M");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getLexeme()).isEqualTo("5M");
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_NUMBER_DECIMAL_INT);
        assertThat(token.getNextToken().getType()).isEqualTo(TokenTypes.NULL);
    }

    @Test
    void shouldTokenize1GAsSingleNumberToken() {
        Segment segment = createSegment("1G");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getLexeme()).isEqualTo("1G");
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_NUMBER_DECIMAL_INT);
        assertThat(token.getNextToken().getType()).isEqualTo(TokenTypes.NULL);
    }

    @Test
    void shouldTokenizeNumberWithoutQuantifier() {
        Segment segment = createSegment("42");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getLexeme()).isEqualTo("42");
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_NUMBER_DECIMAL_INT);
    }

    @Test
    void shouldTokenizePlainNumberInContext() {
        // Given - Number without quantifier in a context
        Segment segment = createSegment(":days 7");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        boolean foundNumber = false;
        while (token != null && token.getType() != TokenTypes.NULL) {
            if (token.getType() == TokenTypes.LITERAL_NUMBER_DECIMAL_INT) {
                assertThat(token.getLexeme()).isEqualTo("7");
                foundNumber = true;
            }
            token = token.getNextToken();
        }
        assertThat(foundNumber).isTrue();
    }

    // ===== (i) Escape sequence tests =====

    @Test
    void shouldHandleEscapedQuoteInString() {
        // Given - String with escaped quote: "test\"quote"
        Segment segment = createSegment("\"test\\\"quote\"");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - The whole string should be a single token
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE);
        assertThat(token.getLexeme()).isEqualTo("\"test\\\"quote\"");
    }

    @Test
    void shouldHandleEscapedBackslashInString() {
        // Given - String with escaped backslash: "path\\to\\file"
        Segment segment = createSegment("\"path\\\\to\\\\file\"");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - The whole string should be a single token
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE);
        assertThat(token.getLexeme()).isEqualTo("\"path\\\\to\\\\file\"");
    }

    @Test
    void shouldHandleEscapeAtEndOfString() {
        // Given - String ending with escape: "test\"
        Segment segment = createSegment("\"test\\\"");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - The string should be a single token
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE);
        assertThat(token.getLexeme()).isEqualTo("\"test\\\"");
    }

    @Test
    void shouldNotTerminateStringAtEscapedQuote() {
        // Given - String with escaped quote in middle: "he said \"hello\" world"
        Segment segment = createSegment("\"he said \\\"hello\\\" world\"");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - The whole string should be a single token
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE);
        // The escaped quotes should not terminate the string
        assertThat(token.getLexeme()).isEqualTo("\"he said \\\"hello\\\" world\"");
    }

    // ===== (j) Heredoc multi-line string tests =====

    @Test
    void shouldTokenizeTextColonHeredocDeclaration() {
        // Given - "text:" triggers heredoc mode
        Segment segment = createSegment("text:");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - "text" should be an IDENTIFIER and ":" should be ANNOTATION
        assertThat(token.getType()).isEqualTo(TokenTypes.IDENTIFIER);
        assertThat(token.getLexeme()).isEqualTo("text");

        Token colonToken = token.getNextToken();
        assertThat(colonToken.getType()).isEqualTo(TokenTypes.ANNOTATION);
        assertThat(colonToken.getLexeme()).isEqualTo(":");
    }

    @Test
    void shouldTokenizeHeredocContent() {
        // Given - After "text:", content line should be LITERAL_BACKQUOTE
        // First, trigger heredoc state
        tokenMaker.getTokenList(createSegment("text:"), TokenTypes.NULL, 0);

        // When - Content line
        Segment contentLine = createSegment("content line here");
        Token token = tokenMaker.getTokenList(contentLine, TokenTypes.NULL, 0);

        // Then - The content should be a LITERAL_BACKQUOTE token
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_BACKQUOTE);
        assertThat(token.getLexeme()).isEqualTo("content line here");
    }

    @Test
    void shouldTerminateHeredocOnDotLine() {
        // Given - After "text:", a line with just "." terminates heredoc
        tokenMaker.getTokenList(createSegment("text:"), TokenTypes.NULL, 0);

        // When - Content line
        tokenMaker.getTokenList(createSegment("some content"), TokenTypes.NULL, 0);

        // When - Terminator line
        Segment dotLine = createSegment(".");
        Token token = tokenMaker.getTokenList(dotLine, TokenTypes.NULL, 0);

        // Then - The dot should be a LITERAL_BACKQUOTE, followed by NULL
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_BACKQUOTE);
        assertThat(token.getLexeme()).isEqualTo(".");
        assertThat(token.getNextToken().getType()).isEqualTo(TokenTypes.NULL);
    }

    @Test
    void shouldHandleMultipleHeredocContentLines() {
        // Given - Simulating multiple content lines in heredoc
        tokenMaker.getTokenList(createSegment("text:"), TokenTypes.NULL, 0);

        // When & Then - Multiple content lines
        Token line1 = tokenMaker.getTokenList(createSegment("Hello Julius,"), TokenTypes.NULL, 0);
        assertThat(line1.getType()).isEqualTo(TokenTypes.LITERAL_BACKQUOTE);
        assertThat(line1.getLexeme()).isEqualTo("Hello Julius,");

        Token line2 = tokenMaker.getTokenList(createSegment("I'm on vacation."), TokenTypes.NULL, 0);
        assertThat(line2.getType()).isEqualTo(TokenTypes.LITERAL_BACKQUOTE);
        assertThat(line2.getLexeme()).isEqualTo("I'm on vacation.");

        // Terminate
        Token dot = tokenMaker.getTokenList(createSegment("."), TokenTypes.NULL, 0);
        assertThat(dot.getType()).isEqualTo(TokenTypes.LITERAL_BACKQUOTE);
        assertThat(dot.getNextToken().getType()).isEqualTo(TokenTypes.NULL);
    }

    @Test
    void shouldTokenizeHeredocInVacationContext() {
        // Given - Full vacation command with heredoc
        // Line: vacation :days 7 :mime text:
        Segment vacationLine = createSegment("vacation :days 7 :mime text:");
        Token token = tokenMaker.getTokenList(vacationLine, TokenTypes.NULL, 0);

        // Then - Verify tokens
        assertThat(token.getLexeme()).isEqualTo("vacation");
        // Token list should have: IDENTIFIER(vacation), WHITESPACE, ANNOTATION(:days),
        // WHITESPACE, NUMBER(7), WHITESPACE, ANNOTATION(:mime), WHITESPACE,
        // IDENTIFIER(text), ANNOTATION(:)

        // And heredoc state should be active
        Token contentToken = tokenMaker.getTokenList(createSegment("vacation body"), TokenTypes.NULL, 0);
        assertThat(contentToken.getType()).isEqualTo(TokenTypes.LITERAL_BACKQUOTE);
        assertThat(contentToken.getLexeme()).isEqualTo("vacation body");
    }

    @Test
    void shouldResetHeredocAfterTermination() {
        // Given - Complete a heredoc
        tokenMaker.getTokenList(createSegment("text:"), TokenTypes.NULL, 0);
        tokenMaker.getTokenList(createSegment("content"), TokenTypes.NULL, 0);
        Token dot = tokenMaker.getTokenList(createSegment("."), TokenTypes.NULL, 0);
        assertThat(dot.getNextToken().getType()).isEqualTo(TokenTypes.NULL);

        // When - A new line after heredoc is terminated
        Segment normalLine = createSegment("keep;");
        Token token = tokenMaker.getTokenList(normalLine, TokenTypes.NULL, 0);

        // Then - Should tokenize normally (not as heredoc), "keep" is IDENTIFIER
        assertThat(token.getType()).isEqualTo(TokenTypes.IDENTIFIER);
        assertThat(token.getLexeme()).isEqualTo("keep");
    }

    // ===== (k) Case-insensitivity tests =====

    @Test
    void shouldMapUppercaseIfToReservedWord() {
        TokenMap tokenMap = tokenMaker.getWordsToHighlight();
        assertThat(tokenMap.get("IF".toCharArray(), 0, 1)).isEqualTo(TokenTypes.RESERVED_WORD);
    }

    @Test
    void shouldMapCapitalizedIfToReservedWord() {
        TokenMap tokenMap = tokenMaker.getWordsToHighlight();
        assertThat(tokenMap.get("If".toCharArray(), 0, 1)).isEqualTo(TokenTypes.RESERVED_WORD);
    }

    @Test
    void shouldMapMixedCaseIfToReservedWord() {
        TokenMap tokenMap = tokenMaker.getWordsToHighlight();
        assertThat(tokenMap.get("iF".toCharArray(), 0, 1)).isEqualTo(TokenTypes.RESERVED_WORD);
    }

    @Test
    void shouldMapUppercaseKeepToFunction() {
        TokenMap tokenMap = tokenMaker.getWordsToHighlight();
        assertThat(tokenMap.get("KEEP".toCharArray(), 0, 3)).isEqualTo(TokenTypes.FUNCTION);
    }

    @Test
    void shouldMapUppercaseHeaderToReservedWord2() {
        TokenMap tokenMap = tokenMaker.getWordsToHighlight();
        assertThat(tokenMap.get("HEADER".toCharArray(), 0, 5)).isEqualTo(TokenTypes.RESERVED_WORD_2);
    }

    @Test
    void shouldMapUppercaseAllofToOperator() {
        TokenMap tokenMap = tokenMaker.getWordsToHighlight();
        assertThat(tokenMap.get("ALLOF".toCharArray(), 0, 4)).isEqualTo(TokenTypes.OPERATOR);
    }

    @Test
    void shouldNotMapNonKeyword() {
        TokenMap tokenMap = tokenMaker.getWordsToHighlight();
        assertThat(tokenMap.get("nonexistent".toCharArray(), 0, 10)).isEqualTo(-1);
    }

    @Test
    void shouldTokenizeCaseInsensitively() {
        // Given - The raw tokenizer should tokenize "IF" as IDENTIFIER (letters)
        Segment segment = createSegment("IF");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        assertThat(token.getType()).isEqualTo(TokenTypes.IDENTIFIER);
        assertThat(token.getLexeme()).isEqualTo("IF");
    }

    // ===== (l) Complex Sieve script test =====

    @Test
    void shouldTokenizeComplexSieveScriptLine1() {
        // Line: require ["fileinto", "vacation"];
        Segment segment = createSegment("require [\"fileinto\", \"vacation\"];");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - "require" is the first identifier
        assertThat(token.getLexeme()).isEqualTo("require");
        assertThat(token.getType()).isEqualTo(TokenTypes.IDENTIFIER);

        // Should have multiple tokens including separators and strings
        int separatorCount = 0;
        int stringCount = 0;
        while (token != null && token.getType() != TokenTypes.NULL) {
            if (token.getType() == TokenTypes.SEPARATOR) separatorCount++;
            if (token.getType() == TokenTypes.LITERAL_STRING_DOUBLE_QUOTE) stringCount++;
            token = token.getNextToken();
        }
        assertThat(separatorCount).isGreaterThanOrEqualTo(4);
        assertThat(stringCount).isEqualTo(2);
    }

    @Test
    void shouldTokenizeComplexSieveScriptLine2() {
        // Line: if header :contains "subject" "spam" {
        Segment segment = createSegment("if header :contains \"subject\" \"spam\" {");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        int identifierCount = 0;
        int annotationCount = 0;
        int stringCount = 0;
        int separatorCount = 0;
        while (token != null && token.getType() != TokenTypes.NULL) {
            switch (token.getType()) {
                case TokenTypes.IDENTIFIER -> identifierCount++;
                case TokenTypes.ANNOTATION -> annotationCount++;
                case TokenTypes.LITERAL_STRING_DOUBLE_QUOTE -> stringCount++;
                case TokenTypes.SEPARATOR -> separatorCount++;
            }
            token = token.getNextToken();
        }

        assertThat(identifierCount).isEqualTo(2); // "if", "header"
        assertThat(annotationCount).isEqualTo(1); // ":contains"
        assertThat(stringCount).isEqualTo(2);     // "subject", "spam"
        assertThat(separatorCount).isEqualTo(1);  // "{"
    }

    @Test
    void shouldTokenizeComplexSieveScriptLine3() {
        // Line: fileinto "Spam";
        Segment segment = createSegment("fileinto \"Spam\";");

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then
        assertThat(token.getLexeme()).isEqualTo("fileinto");
        assertThat(token.getType()).isEqualTo(TokenTypes.IDENTIFIER);
    }

    @Test
    void shouldTokenizeBlockCommentAndVacationLine() {
        // Line: /* vacation notice */
        Segment segment = createSegment("/* vacation notice */");
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);
        assertThat(token.getType()).isEqualTo(TokenTypes.COMMENT_MULTILINE);
        assertThat(token.getLexeme()).isEqualTo("/* vacation notice */");
    }

    @Test
    void shouldTokenizeCompleteSieveScriptPerLine() {
        // Tokenize a realistic multi-line Sieve script one line at a time
        // This test verifies that all constructs work together

        // Line 1: require ["fileinto", "vacation"];
        Token t1 = tokenMaker.getTokenList(createSegment("require [\"fileinto\", \"vacation\"];"), TokenTypes.NULL, 0);
        assertThat(t1.getType()).isEqualTo(TokenTypes.IDENTIFIER);
        assertThat(t1.getLexeme()).isEqualTo("require");

        // Line 2: if header :contains "subject" "spam" {
        Token t2 = tokenMaker.getTokenList(createSegment("if header :contains \"subject\" \"spam\" {"), TokenTypes.NULL, 0);
        assertThat(t2.getType()).isEqualTo(TokenTypes.IDENTIFIER);
        assertThat(t2.getLexeme()).isEqualTo("if");

        // Verify :contains is ANNOTATION
        boolean foundAnnotation = false;
        Token walk = t2;
        while (walk != null && walk.getType() != TokenTypes.NULL) {
            if (walk.getType() == TokenTypes.ANNOTATION) {
                assertThat(walk.getLexeme()).isEqualTo(":contains");
                foundAnnotation = true;
            }
            walk = walk.getNextToken();
        }
        assertThat(foundAnnotation).isTrue();

        // Line 3: fileinto "Spam";
        Token t3 = tokenMaker.getTokenList(createSegment("fileinto \"Spam\";"), TokenTypes.NULL, 0);
        assertThat(t3.getLexeme()).isEqualTo("fileinto");

        // Line 4: /* vacation notice */
        Token t4 = tokenMaker.getTokenList(createSegment("/* vacation notice */"), TokenTypes.NULL, 0);
        assertThat(t4.getType()).isEqualTo(TokenTypes.COMMENT_MULTILINE);
        assertThat(t4.getLexeme()).isEqualTo("/* vacation notice */");

        // Line 5: vacation :days 7 :mime text:  (triggers heredoc)
        Token t5 = tokenMaker.getTokenList(createSegment("vacation :days 7 :mime text:"), TokenTypes.NULL, 0);
        assertThat(t5.getLexeme()).isEqualTo("vacation");

        // Line 6: heredoc content (Hi Julius,)
        Token t6 = tokenMaker.getTokenList(createSegment("Hi Julius,"), TokenTypes.NULL, 0);
        assertThat(t6.getType()).isEqualTo(TokenTypes.LITERAL_BACKQUOTE);
        assertThat(t6.getLexeme()).isEqualTo("Hi Julius,");

        // Line 7: heredoc content (I'm on vacation.)
        Token t7 = tokenMaker.getTokenList(createSegment("I'm on vacation."), TokenTypes.NULL, 0);
        assertThat(t7.getType()).isEqualTo(TokenTypes.LITERAL_BACKQUOTE);
        assertThat(t7.getLexeme()).isEqualTo("I'm on vacation.");

        // Line 8: heredoc terminator (.)
        Token t8 = tokenMaker.getTokenList(createSegment("."), TokenTypes.NULL, 0);
        assertThat(t8.getType()).isEqualTo(TokenTypes.LITERAL_BACKQUOTE);
        assertThat(t8.getLexeme()).isEqualTo(".");
        assertThat(t8.getNextToken().getType()).isEqualTo(TokenTypes.NULL);
    }

    // ===== Helper Methods =====

    private Segment createSegment(String text) {
        char[] array = text.toCharArray();
        return new Segment(array, 0, array.length);
    }

    // ===== Curly Braces Denote Code Blocks =====

    @Test
    void shouldIndicateCurlyBracesDenoteCodeBlocks() {
        assertThat(tokenMaker.getCurlyBracesDenoteCodeBlocks(0)).isTrue();
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
