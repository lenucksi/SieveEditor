package de.febrildur.sieveeditor.system;
// SPDX-FileCopyrightText: 2024 Zwixx
// SPDX-FileCopyrightText: 2025 Claude
// SPDX-FileCopyrightText: 2025 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.*;


public class SieveTokenMaker extends AbstractTokenMaker {

    // Custom state constant for text: heredoc multi-line string tracking
    private static final int MULTILINE_STRING = Integer.MAX_VALUE - 1;

    // Instance variable to track heredoc state across line-by-line getTokenList() calls
    private boolean inMultilineString = false;

    /**
     * Returns a list of tokens representing the given text.
     *
     * @param text           The text to break into tokens.
     * @param startTokenType The token with which to start tokenizing.
     * @param startOffset    The offset at which the line of tokens begins.
     * @return A linked list of tokens representing <code>text</code>.
     */
    @Override
    public Token getTokenList(Segment text, int startTokenType, int startOffset) {

        resetTokenList();

        char[] array = text.array;
        int offset = text.offset;
        int count = text.count;
        int end = offset + count;

        // Token starting offsets are always of the form:
        // 'startOffset + (currentTokenStart-offset)', but since startOffset and
        // offset are constant, tokens' starting positions become:
        // 'newStartOffset+currentTokenStart'.
        int newStartOffset = startOffset - offset;

        int currentTokenStart = offset;
        int currentTokenType = startTokenType;

        // --- Handle text: heredoc multi-line string continuation (TASK-35.3) ---
        if (inMultilineString) {
            String trimmedLine = new String(array, offset, count).trim();
            // Check for terminator: a line with just "." (optionally with trailing CRLF/whitespace)
            if (trimmedLine.equals(".")) {
                addToken(text, offset, end - 1, TokenTypes.LITERAL_BACKQUOTE,
                        newStartOffset + offset);
                addNullToken();
                inMultilineString = false;
                return firstToken;
            }
            // Regular content line - tokenize all as LITERAL_BACKQUOTE
            addToken(text, offset, end - 1, TokenTypes.LITERAL_BACKQUOTE,
                    newStartOffset + offset);
            // Don't add null token; state continues to the next line
            return firstToken;
        }

        // Flags for detecting "text:" heredoc start
        boolean sawTextBeforeColon = false;
        int textColonPos = -1;

        for (int i = offset; i < end; i++) {

            char c = array[i];

            switch (currentTokenType) {

                case TokenTypes.NULL:

                    currentTokenStart = i; // Starting a new token here.

                    switch (c) {

                        case ' ':
                        case '\t':
                            currentTokenType = TokenTypes.WHITESPACE;
                            break;

                        case '"':
                            currentTokenType = TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
                            break;

                        case '#':
                            currentTokenType = TokenTypes.COMMENT_EOL;
                            break;

                        // 35.2-a: SEPARATOR tokens for brackets, semicolon, comma
                        case ';':
                        case '[':
                        case ']':
                        case '(':
                        case ')':
                        case '{':
                        case '}':
                        case ',':
                            currentTokenType = TokenTypes.SEPARATOR;
                            break;

                        // 35.2-b: ANNOTATION token type for colon-prefixed tags
                        case ':':
                            currentTokenType = TokenTypes.ANNOTATION;
                            break;

                        // 35.2-c: COMMENT_MULTILINE for /* */ block comments
                        case '/':
                            if (i + 1 < end && array[i + 1] == '*') {
                                currentTokenType = TokenTypes.COMMENT_MULTILINE;
                                i++; // skip the *
                                break;
                            }
                            // Fall through to identifier handling
                            currentTokenType = TokenTypes.IDENTIFIER;
                            break;

                        default:
                            if (RSyntaxUtilities.isDigit(c)) {
                                currentTokenType = TokenTypes.LITERAL_NUMBER_DECIMAL_INT;
                                break;
                            } else if (RSyntaxUtilities.isLetter(c) || c == '/' || c == '_') {
                                currentTokenType = TokenTypes.IDENTIFIER;
                                break;
                            }

                            // Anything not currently handled - mark as an identifier
                            currentTokenType = TokenTypes.IDENTIFIER;
                            break;

                    }

                    break;

                case TokenTypes.WHITESPACE:

                    switch (c) {

                        case ' ':
                        case '\t':
                            break; // Still whitespace.

                        case '"':
                            addToken(text, currentTokenStart, i - 1, TokenTypes.WHITESPACE,
                                    newStartOffset + currentTokenStart);
                            currentTokenStart = i;
                            currentTokenType = TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
                            break;

                        case '#':
                            addToken(text, currentTokenStart, i - 1, TokenTypes.WHITESPACE,
                                    newStartOffset + currentTokenStart);
                            currentTokenStart = i;
                            currentTokenType = TokenTypes.COMMENT_EOL;
                            break;

                        // 35.2-a: SEPARATOR from whitespace
                        case ';':
                        case '[':
                        case ']':
                        case '(':
                        case ')':
                        case '{':
                        case '}':
                        case ',':
                            addToken(text, currentTokenStart, i - 1, TokenTypes.WHITESPACE,
                                    newStartOffset + currentTokenStart);
                            currentTokenStart = i;
                            currentTokenType = TokenTypes.SEPARATOR;
                            break;

                        // 35.2-b: ANNOTATION from whitespace
                        case ':':
                            addToken(text, currentTokenStart, i - 1, TokenTypes.WHITESPACE,
                                    newStartOffset + currentTokenStart);
                            currentTokenStart = i;
                            currentTokenType = TokenTypes.ANNOTATION;
                            break;

                        default: // Add the whitespace token and start anew.

                            addToken(text, currentTokenStart, i - 1, TokenTypes.WHITESPACE,
                                    newStartOffset + currentTokenStart);
                            currentTokenStart = i;

                            if (RSyntaxUtilities.isDigit(c)) {
                                currentTokenType = TokenTypes.LITERAL_NUMBER_DECIMAL_INT;
                                break;
                            } else if (RSyntaxUtilities.isLetter(c) || c == '/' || c == '_') {
                                currentTokenType = TokenTypes.IDENTIFIER;
                                break;
                            }

                            // Anything not currently handled - mark as identifier
                            currentTokenType = TokenTypes.IDENTIFIER;

                    }

                    break;

                case TokenTypes.IDENTIFIER:

                    switch (c) {

                        case ' ':
                        case '\t':
                            addToken(text, currentTokenStart, i - 1, TokenTypes.IDENTIFIER,
                                    newStartOffset + currentTokenStart);
                            currentTokenStart = i;
                            currentTokenType = TokenTypes.WHITESPACE;
                            break;

                        case '"':
                            addToken(text, currentTokenStart, i - 1, TokenTypes.IDENTIFIER,
                                    newStartOffset + currentTokenStart);
                            currentTokenStart = i;
                            currentTokenType = TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
                            break;

                        default:
                            if (RSyntaxUtilities.isLetterOrDigit(c) || c == '/' || c == '_') {
                                break; // Still an identifier of some type.
                            }
                            // End identifier on non-identifier character
                            int idStart = currentTokenStart;
                            addToken(text, currentTokenStart, i - 1, TokenTypes.IDENTIFIER,
                                    newStartOffset + currentTokenStart);
                            currentTokenStart = i;

                            // Check for "text:" pattern to start heredoc (TASK-35.3)
                            if (c == ':') {
                                if (i - idStart == 4
                                        && array[idStart] == 't'
                                        && array[idStart + 1] == 'e'
                                        && array[idStart + 2] == 'x'
                                        && array[idStart + 3] == 't') {
                                    sawTextBeforeColon = true;
                                    textColonPos = i;
                                }
                                currentTokenType = TokenTypes.ANNOTATION;
                                break;
                            }

                            // 35.2-a: Handle separators at end of identifier
                            if (c == ';' || c == '[' || c == ']'
                                    || c == '(' || c == ')'
                                    || c == '{' || c == '}'
                                    || c == ',') {
                                currentTokenType = TokenTypes.SEPARATOR;
                                break;
                            }

                            // For any other non-identifier character, re-process in NULL state
                            i--;
                            currentTokenType = TokenTypes.NULL;
                    }

                    break;

                // 35.2-b: ANNOTATION state handler for colon-prefixed tags
                case TokenTypes.ANNOTATION:

                    switch (c) {

                        case ' ':
                        case '\t':
                            addToken(text, currentTokenStart, i - 1, TokenTypes.ANNOTATION,
                                    newStartOffset + currentTokenStart);
                            // If the annotation was just ':' after "text", this is a heredoc start
                            if (sawTextBeforeColon && i - currentTokenStart == 1) {
                                textColonPos = currentTokenStart;
                            }
                            currentTokenStart = i;
                            currentTokenType = TokenTypes.WHITESPACE;
                            break;

                        case '"':
                            addToken(text, currentTokenStart, i - 1, TokenTypes.ANNOTATION,
                                    newStartOffset + currentTokenStart);
                            currentTokenStart = i;
                            currentTokenType = TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
                            break;

                        default:
                            if (RSyntaxUtilities.isLetterOrDigit(c) || c == '/' || c == '_') {
                                break; // Still inside tag name
                            }
                            // For any other character, end the tag and re-process
                            addToken(text, currentTokenStart, i - 1, TokenTypes.ANNOTATION,
                                    newStartOffset + currentTokenStart);
                            currentTokenStart = i;
                            i--;
                            currentTokenType = TokenTypes.NULL;
                    }

                    break;

                // 35.2-a: SEPARATOR state handler (single-character tokens)
                case TokenTypes.SEPARATOR:
                    addToken(text, currentTokenStart, i - 1, TokenTypes.SEPARATOR,
                            newStartOffset + currentTokenStart);
                    currentTokenStart = i;
                    currentTokenType = TokenTypes.NULL;
                    i--; // Re-process current character in NULL state
                    break;

                // 35.2-c: COMMENT_MULTILINE state handler for /* */ block comments
                case TokenTypes.COMMENT_MULTILINE:
                    if (c == '*' && i + 1 < end && array[i + 1] == '/') {
                        addToken(text, currentTokenStart, i + 1, TokenTypes.COMMENT_MULTILINE,
                                newStartOffset + currentTokenStart);
                        i++; // skip the /
                        currentTokenType = TokenTypes.NULL;
                    }
                    break;

                case TokenTypes.LITERAL_NUMBER_DECIMAL_INT:

                    switch (c) {

                        case ' ':
                        case '\t':
                            addToken(text, currentTokenStart, i - 1, TokenTypes.LITERAL_NUMBER_DECIMAL_INT,
                                    newStartOffset + currentTokenStart);
                            currentTokenStart = i;
                            currentTokenType = TokenTypes.WHITESPACE;
                            break;

                        case '"':
                            addToken(text, currentTokenStart, i - 1, TokenTypes.LITERAL_NUMBER_DECIMAL_INT,
                                    newStartOffset + currentTokenStart);
                            currentTokenStart = i;
                            currentTokenType = TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
                            break;

                        default:

                            if (RSyntaxUtilities.isDigit(c)) {
                                break; // Still a literal number.
                            }

                            // 35.2-d: Number quantifier suffixes (K/M/G) remain part of number token
                            if (c == 'K' || c == 'M' || c == 'G') {
                                break; // Keep quantifier as part of the number token
                            }

                            // Otherwise, remember this was a number and start over.
                            addToken(text, currentTokenStart, i - 1, TokenTypes.LITERAL_NUMBER_DECIMAL_INT,
                                    newStartOffset + currentTokenStart);
                            i--;
                            currentTokenType = TokenTypes.NULL;

                    }

                    break;

                case TokenTypes.COMMENT_EOL:
                    i = end - 1;
                    addToken(text, currentTokenStart, i, currentTokenType,
                            newStartOffset + currentTokenStart);
                    // We need to set token type to null so at the bottom we don't add one more
                    // TokenTypes.
                    currentTokenType = TokenTypes.NULL;
                    break;

                case TokenTypes.LITERAL_STRING_DOUBLE_QUOTE:
                    if (c == '"') {
                        addToken(text, currentTokenStart, i, TokenTypes.LITERAL_STRING_DOUBLE_QUOTE,
                                newStartOffset + currentTokenStart);
                        currentTokenType = TokenTypes.NULL;
                    }
                    // 35.2-e: Escape sequences in strings - skip escaped character
                    else if (c == '\\' && i + 1 < end) {
                        i++; // Skip the escaped character (e.g., \" doesn't terminate the string)
                    }
                    break;

                default: // Should never happen
            }
        }

        // --- Check for text: heredoc start (TASK-35.3) ---
        if (sawTextBeforeColon && textColonPos >= 0) {
            if (currentTokenType == TokenTypes.ANNOTATION) {
                // Still in annotation (no whitespace after ':')
                // If annotation is just ':' (single char at end of line), start heredoc
                if (currentTokenStart == end - 1) {
                    inMultilineString = true;
                }
            } else if (currentTokenType == TokenTypes.WHITESPACE) {
                // Annotation was already added when whitespace was encountered
                // If the annotation was just ':' (textColonPos immediately before whitespace)
                if (currentTokenStart > 0 && currentTokenStart - 1 == textColonPos) {
                    inMultilineString = true;
                }
            }
        }

        switch (currentTokenType) {

            // Remember what token type to begin the next line with.
            case TokenTypes.LITERAL_STRING_DOUBLE_QUOTE:
                addToken(text, currentTokenStart, end - 1, currentTokenType,
                        newStartOffset + currentTokenStart);
                break;

            // COMMENT_MULTILINE continues across lines (no null token to preserve state)
            case TokenTypes.COMMENT_MULTILINE:
                addToken(text, currentTokenStart, end - 1, currentTokenType,
                        newStartOffset + currentTokenStart);
                break;

            // Do nothing if everything was okay.
            case TokenTypes.NULL:
                addNullToken();
                break;

            // All other token types don't continue to the next line...
            default:
                addToken(text, currentTokenStart, end - 1, currentTokenType,
                        newStartOffset + currentTokenStart);
                addNullToken();
        }

        // Return the first token in our linked list.
        return firstToken;

    }

    @Override
    public TokenMap getWordsToHighlight() {
        TokenMap tokenMap = new TokenMap(true); // case-insensitive

        // === Control Flow Commands (RESERVED_WORD) ===
        tokenMap.put("if", TokenTypes.RESERVED_WORD);
        tokenMap.put("elsif", TokenTypes.RESERVED_WORD);
        tokenMap.put("else", TokenTypes.RESERVED_WORD);
        tokenMap.put("stop", TokenTypes.RESERVED_WORD);
        tokenMap.put("return", TokenTypes.RESERVED_WORD);
        tokenMap.put("break", TokenTypes.RESERVED_WORD);
        tokenMap.put("foreverypart", TokenTypes.RESERVED_WORD);
        tokenMap.put("require", TokenTypes.RESERVED_WORD);

        // === Action Commands (FUNCTION) ===
        tokenMap.put("keep", TokenTypes.FUNCTION);
        tokenMap.put("discard", TokenTypes.FUNCTION);
        tokenMap.put("fileinto", TokenTypes.FUNCTION);
        tokenMap.put("redirect", TokenTypes.FUNCTION);
        tokenMap.put("reject", TokenTypes.FUNCTION);
        tokenMap.put("ereject", TokenTypes.FUNCTION);
        tokenMap.put("vacation", TokenTypes.FUNCTION);
        tokenMap.put("notify", TokenTypes.FUNCTION);
        tokenMap.put("denotify", TokenTypes.FUNCTION);
        tokenMap.put("addflag", TokenTypes.FUNCTION);
        tokenMap.put("removeflag", TokenTypes.FUNCTION);
        tokenMap.put("setflag", TokenTypes.FUNCTION);
        tokenMap.put("set", TokenTypes.FUNCTION);
        tokenMap.put("include", TokenTypes.FUNCTION);
        tokenMap.put("global", TokenTypes.FUNCTION);
        tokenMap.put("mailbox", TokenTypes.FUNCTION);
        tokenMap.put("duplicate", TokenTypes.FUNCTION);
        tokenMap.put("cancel", TokenTypes.FUNCTION);
        tokenMap.put("convert", TokenTypes.FUNCTION);
        tokenMap.put("replace", TokenTypes.FUNCTION);
        tokenMap.put("enclose", TokenTypes.FUNCTION);
        tokenMap.put("extracttext", TokenTypes.FUNCTION);
        tokenMap.put("deleteheader", TokenTypes.FUNCTION);
        tokenMap.put("addheader", TokenTypes.FUNCTION);

        // === Test Commands (RESERVED_WORD_2) ===
        tokenMap.put("address", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("envelope", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("exists", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("header", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("size", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("body", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("date", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("currentdate", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("environment", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("spamtest", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("virustest", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("hasflag", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("mailboxexists", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("metadata", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("metadataexists", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("servermetadata", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("servermetadataexists", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("ihave", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("regex", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("list", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("true", TokenTypes.RESERVED_WORD_2);
        tokenMap.put("false", TokenTypes.RESERVED_WORD_2);

        // === Logical Operators (OPERATOR) ===
        tokenMap.put("allof", TokenTypes.OPERATOR);
        tokenMap.put("anyof", TokenTypes.OPERATOR);
        tokenMap.put("not", TokenTypes.OPERATOR);

        // === Tags (ANNOTATION) ===
        // Match-type
        tokenMap.put(":is", TokenTypes.ANNOTATION);
        tokenMap.put(":contains", TokenTypes.ANNOTATION);
        tokenMap.put(":matches", TokenTypes.ANNOTATION);
        tokenMap.put(":regex", TokenTypes.ANNOTATION);
        // Relational
        tokenMap.put(":count", TokenTypes.ANNOTATION);
        tokenMap.put(":value", TokenTypes.ANNOTATION);
        // Address-part
        tokenMap.put(":localpart", TokenTypes.ANNOTATION);
        tokenMap.put(":domain", TokenTypes.ANNOTATION);
        tokenMap.put(":all", TokenTypes.ANNOTATION);
        tokenMap.put(":user", TokenTypes.ANNOTATION);
        tokenMap.put(":detail", TokenTypes.ANNOTATION);
        // Comparator
        tokenMap.put(":comparator", TokenTypes.ANNOTATION);
        // Size
        tokenMap.put(":over", TokenTypes.ANNOTATION);
        tokenMap.put(":under", TokenTypes.ANNOTATION);
        // Action modifiers
        tokenMap.put(":copy", TokenTypes.ANNOTATION);
        tokenMap.put(":create", TokenTypes.ANNOTATION);
        tokenMap.put(":flags", TokenTypes.ANNOTATION);
        tokenMap.put(":fcc", TokenTypes.ANNOTATION);
        tokenMap.put(":list", TokenTypes.ANNOTATION);
        tokenMap.put(":permissions", TokenTypes.ANNOTATION);
        // Vacation
        tokenMap.put(":days", TokenTypes.ANNOTATION);
        tokenMap.put(":from", TokenTypes.ANNOTATION);
        tokenMap.put(":subject", TokenTypes.ANNOTATION);
        tokenMap.put(":handle", TokenTypes.ANNOTATION);
        tokenMap.put(":mime", TokenTypes.ANNOTATION);
        tokenMap.put(":text", TokenTypes.ANNOTATION);
        tokenMap.put(":addresses", TokenTypes.ANNOTATION);
        tokenMap.put(":noreply", TokenTypes.ANNOTATION);
        tokenMap.put(":reply_regex", TokenTypes.ANNOTATION);
        tokenMap.put(":reply_prefix", TokenTypes.ANNOTATION);
        tokenMap.put(":sender", TokenTypes.ANNOTATION);
        tokenMap.put(":database", TokenTypes.ANNOTATION);
        tokenMap.put(":return_address", TokenTypes.ANNOTATION);
        tokenMap.put(":header", TokenTypes.ANNOTATION);
        tokenMap.put(":always_reply", TokenTypes.ANNOTATION);
        tokenMap.put(":rfc2822", TokenTypes.ANNOTATION);
        tokenMap.put(":file", TokenTypes.ANNOTATION);
        // Notification
        tokenMap.put(":importance", TokenTypes.ANNOTATION);
        tokenMap.put(":message", TokenTypes.ANNOTATION);
        tokenMap.put(":options", TokenTypes.ANNOTATION);
        tokenMap.put(":priority", TokenTypes.ANNOTATION);
        // Date/Timezone
        tokenMap.put(":zone", TokenTypes.ANNOTATION);
        tokenMap.put(":originalzone", TokenTypes.ANNOTATION);
        tokenMap.put(":index", TokenTypes.ANNOTATION);
        tokenMap.put(":last", TokenTypes.ANNOTATION);
        // MIME
        tokenMap.put(":anychild", TokenTypes.ANNOTATION);
        tokenMap.put(":type", TokenTypes.ANNOTATION);
        tokenMap.put(":subtype", TokenTypes.ANNOTATION);
        tokenMap.put(":contenttype", TokenTypes.ANNOTATION);
        tokenMap.put(":param", TokenTypes.ANNOTATION);
        // Foreverypart
        tokenMap.put(":outer", TokenTypes.ANNOTATION);
        tokenMap.put(":first", TokenTypes.ANNOTATION);
        tokenMap.put(":atleast", TokenTypes.ANNOTATION);
        // Duplicate
        tokenMap.put(":uniqueid", TokenTypes.ANNOTATION);
        tokenMap.put(":seconds", TokenTypes.ANNOTATION);
        tokenMap.put(":limit", TokenTypes.ANNOTATION);
        // Editheader
        tokenMap.put(":deleteheaders", TokenTypes.ANNOTATION);
        tokenMap.put(":addheaders", TokenTypes.ANNOTATION);
        tokenMap.put(":changefield", TokenTypes.ANNOTATION);
        tokenMap.put(":newfield", TokenTypes.ANNOTATION);
        // Extlists
        tokenMap.put(":addrbook", TokenTypes.ANNOTATION);
        // Convert
        tokenMap.put(":modifier", TokenTypes.ANNOTATION);

        return tokenMap;
    }

}
