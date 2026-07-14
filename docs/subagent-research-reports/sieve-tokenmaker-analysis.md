Now I have all the research data needed. Let me compile the comprehensive analysis.

---

# Comprehensive Sieve Language Tokenization Analysis for RSyntaxTextArea

## 1. Categorized Keyword-to-TokenType Mapping Table

### 1A. Keywords Handled via `getWordsToHighlight()` (Identifier-Chars Only)

These are pure identifier tokens (letters/digits/underscores). They are tokenized as `IDENTIFIER` by `getTokenList()` and reclassified by the framework using the `TokenMap` returned from `getWordsToHighlight()`.

| Category | Keywords | Proposed `TokenTypes` | Rationale |
|---|---|---|---|
| **Control Flow** | `if`, `elsif`, `else`, `stop`, `return`, `break`, `foreverypart`, `require` | `RESERVED_WORD` | Core language control structures; analogous to `if/else/while/return` in C/Java. |
| **Action Commands** | `keep`, `discard`, `fileinto`, `redirect`, `reject`, `ereject`, `vacation`, `notify`, `denotify`, `addflag`, `removeflag`, `setflag`, `set`, `include`, `global`, `mailbox`, `duplicate`, `cancel`, `convert`, `extracttext`, `replace`, `enclose`, `deleteheader`, `addheader` | `FUNCTION` | These are built-in action functions that the Sieve engine executes. In tree-sitter they are `function.builtin`. |
| **Test Commands** | `address`, `envelope`, `exists`, `header`, `size`, `body`, `date`, `currentdate`, `environment`, `spamtest`, `virustest`, `hasflag`, `mailboxexists`, `metadata`, `metadataexists`, `servermetadata`, `servermetadataexists`, `duplicate` (test), `ihave`, `regex`, `list` | `RESERVED_WORD_2` | Second-tier reserved words to distinguish test commands from control flow visually. Tests are used within conditional contexts. |
| **Boolean Tests** | `true`, `false` | `RESERVED_WORD_2` | These are test commands returning boolean, not literal values. Consistent with other test commands. |
| **Logical Operators** | `allof`, `anyof`, `not` | `OPERATOR` | Logical AND/OR/NOT operators used in test composition. In TextMate grammar they are `keyword.operator.logical`. |
| **Numeric Quantifiers** | (Used as trailing suffix on numbers: K, M, G) | (Not in TokenMap; handled at tokenizer level) | These are quantifier suffixes for the `number` production. Handled in `getTokenList()` as part of `LITERAL_NUMBER_DECIMAL_INT`. |

### 1B. Tags Handled via `getTokenList()` Modification (Colon-Prefixed)

Tags start with `:` — this is NOT an identifier character. They require modifying `getTokenList()` to produce an `ANNOTATION`-type token.

| Subcategory | Tags | Proposed `TokenTypes` | Rationale |
|---|---|---|---|
| **Match-Type** | `:is`, `:contains`, `:matches`, `:regex` | `ANNOTATION` | Tags modify how a command/test behaves, analogous to annotations/attributes. |
| **Relational** | `:count`, `:value` | `ANNOTATION` | RFC 5231 relational match types. Functionally similar to match-type tags. |
| **Address-Part** | `:localpart`, `:domain`, `:all`, `:user`, `:detail` | `ANNOTATION` | Control how address/envelope tests parse email addresses. |
| **Comparator** | `:comparator` | `ANNOTATION` | Specifies the comparison algorithm. Takes a string argument. |
| **Size** | `:over`, `:under` | `ANNOTATION` | Used with `size` test to specify comparison direction. |
| **Action Modifiers** | `:copy`, `:create`, `:flags`, `:fcc`, `:list`, `:permissions` | `ANNOTATION` | Modify how actions execute (e.g., `:copy` preserves original). |
| **Vacation** | `:days`, `:from`, `:subject`, `:handle`, `:mime`, `:text`, `:addresses`, `:noreply` | `ANNOTATION` | Vacation action parameters. |
| **Notification** | `:importance`, `:message`, `:options`, `:priority` | `ANNOTATION` | Notification parameters. |
| **Date/Timezone** | `:zone`, `:originalzone`, `:index`, `:last` | `ANNOTATION` | Used with `date` and `currentdate` tests. |
| **MIME** | `:mime`, `:anychild`, `:type`, `:subtype`, `:contenttype`, `:param` | `ANNOTATION` | RFC 5703 MIME part handling. |
| **Foreverypart** | `:outer`, `:first`, `:last`, `:atleast`, `:value` | `ANNOTATION` | RFC 5703 MIME iteration. |
| **Editheader** | `:last`, `:index` | `ANNOTATION` | RFC 5293 header editing location specifiers. |
| **Duplicate** | `:header`, `:uniqueid`, `:seconds`, `:limit` | `ANNOTATION` | RFC 8579 duplicate detection parameters. |

### 1C. Literals and Comments (Handled in `getTokenList()`)

| Construct | Examples | Proposed `TokenTypes` |
|---|---|---|
| **Quoted strings** | `"hello world"` | `LITERAL_STRING_DOUBLE_QUOTE` |
| **Multi-line strings** | `text:\ncontent\n.\n` | Content as `LITERAL_BACKQUOTE` |
| **Integer numbers** | `0`, `42`, `100`, `65535` | `LITERAL_NUMBER_DECIMAL_INT` |
| **Numbers with quantifier** | `100K`, `5M`, `1G` | `LITERAL_NUMBER_DECIMAL_INT` (for `100`), `IDENTIFIER` (for `K`) — OR combined as `ERROR_NUMBER_FORMAT` if we handle it specially |
| **Hash comments** | `# comment` | `COMMENT_EOL` |
| **Block comments** | `/* comment */` | `COMMENT_MULTILINE` |

### 1D. Separators and Punctuation

| Character(s) | Proposed `TokenTypes` |
|---|---|
| `;` (statement terminator) | `SEPARATOR` |
| `[` `]` (string lists) | `SEPARATOR` |
| `(` `)` (test lists) | `SEPARATOR` |
| `{` `}` (blocks) | `SEPARATOR` |
| `,` (list separator) | `SEPARATOR` |

---

## 2. Analysis of What's Missing from the Current Tokenizer

### 2.1. Block Comments (`/* */`) — CRITICAL MISSING FEATURE

**Current behavior:** The tokenizer only handles `#` line comments. When encountering `/`, it starts an `IDENTIFIER` token (line 71). When `*` follows as an identifier char, the `/` and `*` are consumed as part of an identifier.

**Required behavior:** Recognize `/*` as starting a `COMMENT_MULTILINE` token, consume all content until `*/`, and properly handle the closing delimiter.

**Implementation complexity:** High. This requires:

- Detecting `/` in the `NULL` state and peeking at the next character
- If next is `*`, switch to `COMMENT_MULTILINE`
- In `COMMENT_MULTILINE` state, scan for `*/` (properly handling nested STAR sequences as per RFC 5228 section 8.1 grammar: `*not-star 1*STAR *(not-star-slash *not-star 1*STAR)`)
- `COMMENT_MULTILINE` state must continue across lines (tracked via `startTokenType`)

The RFC grammar for block comments is:

```text
bracket-comment = "/*" *not-star 1*STAR
                  *(not-star-slash *not-star 1*STAR) "/"
```

This means: after `/*`, zero or more non-star characters, then at least one star, then a potentially repeating group of "non-star-and-non-slash followed by zero or more non-star followed by at least one star", and finally a `/`. The key rule: `*/` inside the comment is forbidden, so `/* foo */` is the minimal valid form.

### 2.2. Multi-line Strings (`text:` heredoc) — MISSING

**Current behavior:** `text:` is tokenized as an `IDENTIFIER` (starting with `t`). The content lines
are tokenized per-line as identifiers, separators, etc.

**Required behavior:** Recognize `text:` as starting a multi-line string token. The multi-line string
content should be highlighted as a string literal, terminated by a line with only `.`.

**Implementation complexity:** Very high (cross-line state tracking). The `startTokenType` parameter
in `getTokenList()` would need a custom state value (beyond the standard TokenTypes) to indicate
"inside multi-line string". This requires:

- Custom state constants (e.g., `private static final int MULTILINE_STRING = 1000;`)
- Detecting `text:` followed by whitespace and newline as the start
- Consuming lines until a line consisting solely of `.` (optionally followed by CRLF)
- Tracking this state via `startTokenType` across line tokenization calls

The RFC grammar:

```text
multi-line = "text:" *(SP / HTAB) (hash-comment / CRLF)
             *(multiline-literal / multiline-dotstart)
             "." CRLF
multiline-literal = [ octet-not-period *octet-not-crlf ] CRLF
multiline-dotstart = "." 1*octet-not-crlf CRLF
```

Note: Lines starting with `..` have one `.` stripped (dot-stuffing).

### 2.3. Colon-Prefixed Tags (`:contains`, `:over`, etc.) — POORLY HANDLED

**Current behavior:** The `:` character is caught by the fallthrough in the `default` case:

```java
currentTokenType = TokenTypes.IDENTIFIER;
```

So `:contains` becomes a single `IDENTIFIER` token with lexeme `:contains`.

**Required behavior:** Either:

1. Recognize `:` as starting an `ANNOTATION` token and include the tag name portion
2. Or keep as `IDENTIFIER` and rely on TokenMap lookup with `"contains"` as key

The cleanest approach is to add a case for `:` in the `NULL` state that starts `ANNOTATION`.

### 2.4. Number Quantifier Suffixes (`K`, `M`, `G`) — NOT HANDLED CORRECTLY

**Current behavior:** `100K` is tokenized as `100` (LITERAL_NUMBER_DECIMAL_INT) followed by `K` (IDENTIFIER).

**Required behavior:** The RFC specifies `number = 1*DIGIT [ QUANTIFIER ]`. The quantifier should be
part of the number token. Either:

1. Make the entire `100K` a single `LITERAL_NUMBER_DECIMAL_INT` token (cleaner), or
2. Keep the current split behavior (technically correct for parsing, less clean visually)

Option 1 is better for highlighting. The tokenizer should check if a letter following digits is K/M/G
and include it in the number token, or use `ERROR_NUMBER_FORMAT` if we want to flag the quantifier
as non-standard numeric notation.

### 2.5. Brackets (`[`, `]`, `(`, `)`, `{`, `}`) — UNHANDLED

**Current behavior:** `[`, `]`, `(`, `)`, `{`, `}` are all caught by the fallthrough default case and
tokenized as `IDENTIFIER` tokens.

**Required behavior:** These should be `SEPARATOR` tokens. RSyntaxTextArea's `[`/`]` and `{`/`}`
bracket matching depends on them being `SEPARATOR`.

### 2.6. Semicolons (`;`) — UNHANDLED

**Current behavior:** Tokenized as `IDENTIFIER`.

**Required behavior:** Should be `SEPARATOR` for proper bracket-aware features.

### 2.7. The `_` in Identifiers — PARTIALLY HANDLED

**Current behavior:** `_` is recognized as starting an identifier (line 71). This is correct per RFC
`identifier = (ALPHA / "_") *(ALPHA / DIGIT / "_")`.

However, the identifier character check in the `IDENTIFIER` state uses `RSyntaxUtilities.isLetterOrDigit(c) || c == '/' || c == '_'` which correctly includes `_`.

**Note:** The RFC does NOT allow `-` or `.` in identifiers. The tokenizer correctly excludes them.

### 2.8. Escape Sequences in Quoted Strings — NOT HANDLED

**Current behavior:** `\"` inside a quoted string terminates the string token at `\`, not at the
escaped `"`.

**Required behavior:** Recognize `\` as starting an escape sequence within strings. The character
following `\` should be consumed as part of the string without terminating it. Per RFC:

```text
quoted-other = "\" octet-not-qspecial
```

---

## 3. Recommendations for Improving `getTokenList()` to Handle Tags

### 3.1. Add a New `ANNOTATION` Token State for Tags

In the `NULL` state handler, add a case for `:`:

```java
case ':':
    currentTokenType = TokenTypes.ANNOTATION;
    break;
```

Then add an `ANNOTATION` state handler alongside the `IDENTIFIER` state:

```java
case TokenTypes.ANNOTATION:
    switch (c) {
        case ' ':
        case '\t':
            addToken(text, currentTokenStart, i - 1, TokenTypes.ANNOTATION,
                newStartOffset + currentTokenStart);
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
            i--;
            currentTokenType = TokenTypes.NULL;
    }
    break;
```

Similarly, add handling for `:` in the `WHITESPACE` state and other relevant states.

### 3.2. Alternative: Use TokenMap with Colon-Included Keys

A simpler alternative that requires minimal `getTokenList()` changes:

```java
// In getWordsToHighlight():
tokenMap.put(":contains", TokenTypes.ANNOTATION);
tokenMap.put(":is", TokenTypes.ANNOTATION);
// ... etc.
```

This works because `TokenMap.get(char[], int, int)` does character-by-character comparison without
filtering for identifier-valid characters. The `:contains` string is stored and matched exactly.

**Trade-off:** The `:` prefix is included in the highlighted token, which makes the tag name itself
highlighted as an `ANNOTATION` type. This is acceptable and parallels how Sieve is displayed in
other editors (TextMate grammar shows `:is` as `keyword.operator.comparison.sieve`).

**Recommendation:** Use the TokenMap approach for immediate improvement (less invasive), but plan to
add the `ANNOTATION` state for better accuracy and to allow bracket matching with `SEPARATOR` tokens.

### 3.3. Complete `getWordsToHighlight()` Implementation

Here is the proposed complete `getWordsToHighlight()` method:

```java
@Override
public TokenMap getWordsToHighlight() {
    TokenMap tokenMap = new TokenMap();

    // === Control Flow Commands (RESERVED_WORD) ===
    tokenMap.put("if", TokenTypes.RESERVED_WORD);
    tokenMap.put("elsif", TokenTypes.RESERVED_WORD);
    tokenMap.put("else", TokenTypes.RESERVED_WORD);
    tokenMap.put("stop", TokenTypes.RESERVED_WORD);
    tokenMap.put("return", TokenTypes.RESERVED_WORD);        // RFC 6609
    tokenMap.put("break", TokenTypes.RESERVED_WORD);         // RFC 5703
    tokenMap.put("foreverypart", TokenTypes.RESERVED_WORD);  // RFC 5703
    tokenMap.put("require", TokenTypes.RESERVED_WORD);

    // === Action Commands (FUNCTION) ===
    tokenMap.put("keep", TokenTypes.FUNCTION);
    tokenMap.put("discard", TokenTypes.FUNCTION);
    tokenMap.put("fileinto", TokenTypes.FUNCTION);
    tokenMap.put("redirect", TokenTypes.FUNCTION);
    tokenMap.put("reject", TokenTypes.FUNCTION);             // RFC 5429
    tokenMap.put("ereject", TokenTypes.FUNCTION);            // RFC 5429
    tokenMap.put("vacation", TokenTypes.FUNCTION);           // RFC 5230
    tokenMap.put("notify", TokenTypes.FUNCTION);             // RFC 5435
    tokenMap.put("denotify", TokenTypes.FUNCTION);           // RFC 5435
    tokenMap.put("addflag", TokenTypes.FUNCTION);            // RFC 5232
    tokenMap.put("removeflag", TokenTypes.FUNCTION);         // RFC 5232
    tokenMap.put("setflag", TokenTypes.FUNCTION);            // RFC 5232
    tokenMap.put("set", TokenTypes.FUNCTION);                // RFC 5229
    tokenMap.put("include", TokenTypes.FUNCTION);            // RFC 6609
    tokenMap.put("global", TokenTypes.FUNCTION);             // RFC 6609
    tokenMap.put("mailbox", TokenTypes.FUNCTION);            // RFC 5490
    tokenMap.put("duplicate", TokenTypes.FUNCTION);          // RFC 8579
    tokenMap.put("cancel", TokenTypes.FUNCTION);             // RFC 8579
    tokenMap.put("convert", TokenTypes.FUNCTION);            // RFC 6558
    tokenMap.put("replace", TokenTypes.FUNCTION);            // RFC 5703
    tokenMap.put("enclose", TokenTypes.FUNCTION);            // RFC 5703
    tokenMap.put("extracttext", TokenTypes.FUNCTION);        // RFC 5703
    tokenMap.put("deleteheader", TokenTypes.FUNCTION);       // RFC 5293 / 5703
    tokenMap.put("addheader", TokenTypes.FUNCTION);          // RFC 5293 / 5703

    // === Test Commands (RESERVED_WORD_2) ===
    tokenMap.put("address", TokenTypes.RESERVED_WORD_2);
    tokenMap.put("envelope", TokenTypes.RESERVED_WORD_2);
    tokenMap.put("exists", TokenTypes.RESERVED_WORD_2);
    tokenMap.put("header", TokenTypes.RESERVED_WORD_2);
    tokenMap.put("size", TokenTypes.RESERVED_WORD_2);
    tokenMap.put("true", TokenTypes.RESERVED_WORD_2);
    tokenMap.put("false", TokenTypes.RESERVED_WORD_2);
    tokenMap.put("body", TokenTypes.RESERVED_WORD_2);        // RFC 5173
    tokenMap.put("date", TokenTypes.RESERVED_WORD_2);        // RFC 5260
    tokenMap.put("currentdate", TokenTypes.RESERVED_WORD_2); // RFC 5260
    tokenMap.put("environment", TokenTypes.RESERVED_WORD_2); // RFC 5183
    tokenMap.put("spamtest", TokenTypes.RESERVED_WORD_2);    // RFC 5235
    tokenMap.put("virustest", TokenTypes.RESERVED_WORD_2);   // RFC 5235
    tokenMap.put("hasflag", TokenTypes.RESERVED_WORD_2);     // RFC 5232
    tokenMap.put("mailboxexists", TokenTypes.RESERVED_WORD_2); // RFC 5490
    tokenMap.put("metadata", TokenTypes.RESERVED_WORD_2);    // RFC 5490
    tokenMap.put("metadataexists", TokenTypes.RESERVED_WORD_2); // RFC 5490
    tokenMap.put("servermetadata", TokenTypes.RESERVED_WORD_2);
    tokenMap.put("servermetadataexists", TokenTypes.RESERVED_WORD_2);
    tokenMap.put("ihave", TokenTypes.RESERVED_WORD_2);       // RFC 5463
    tokenMap.put("regex", TokenTypes.RESERVED_WORD_2);       // draft
    tokenMap.put("list", TokenTypes.RESERVED_WORD_2);        // RFC 6134

    // === Logical Operators (OPERATOR) ===
    tokenMap.put("allof", TokenTypes.OPERATOR);
    tokenMap.put("anyof", TokenTypes.OPERATOR);
    tokenMap.put("not", TokenTypes.OPERATOR);

    // === Tags (ANNOTATION) - Option A: include colon in key ===
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
```

---

## 4. Consideration of Sieve-Specific Constructs

### 4.1. `text:` Heredoc Strings

**Challenge:** The multi-line string spans multiple lines and ends with a line containing only `.`.
This is the Sieve equivalent of heredoc syntax.

**Implementation approach:**

- Define a custom state constant: `private static final int MULTILINE_STRING = Integer.MAX_VALUE - 1;`
- In `getTokenList()`, when `startTokenType == MULTILINE_STRING`, the current line is continuation
- Detect the start pattern: `text:` followed by whitespace to end-of-line (or hash-comment)
- Tokenize the content as `LITERAL_BACKQUOTE`
- Check each line for the dot terminator (line starting with `.` followed only by CRLF)
- Track state return value so next line call knows it's still in multi-line

**Pseudo-implementation:**

```java
// In NULL state, detect "text:"
// Need to lookahead: if we see 't', then 'e', 'x', 't', ':', consume as start of heredoc
// This requires a multi-character lookahead, which is complex in the current per-char loop

// Simpler approach: after tokenizing "text:" as IDENTIFIER and then ":" as ANNOTATION
// (or however the colon is handled), detect the pattern and switch states
```

**Alternative:** Tokenize `text:` as an `IDENTIFIER` or `ANNOTATION` token, and the content lines as `LITERAL_BACKQUOTE` with the `.` terminator also as `LITERAL_BACKQUOTE`. Use `startTokenType` to track the cross-line state.

### 4.2. Block Comments (`/* */`)

**Challenge:** The RFC grammar forbids `*/` inside the comment, but allows `*` freely as long as it's
not followed by `/`. The `no-star-slash` rule means any `*` that isn't the last one must be followed
by a non-slash character.

**Implementation approach:**

- Use `TokenTypes.COMMENT_MULTILINE` for block comments
- Detect `/*` sequence using a lookahead in the `NULL` state
- Track state across lines via `startTokenType`
- Parse content: consume everything until `*/` is found
- The RFC grammar: `"/*" *not-star 1*STAR *(not-star-slash *not-star 1*STAR) "/"`

**Simplified implementation (no strict RFC enforcement):**

```java
// In NULL state, handle '/':
case '/':
    if (i + 1 < end && array[i + 1] == '*') {
        currentTokenType = TokenTypes.COMMENT_MULTILINE;
        i++; // skip the '*'
        break;
    }
    // Fall through to default (treat '/' as identifier start)
    currentTokenType = TokenTypes.IDENTIFIER;
    break;

// In COMMENT_MULTILINE state (at the bottom of the for loop, after switch):
case TokenTypes.COMMENT_MULTILINE:
    if (c == '*' && i + 1 < end && array[i + 1] == '/') {
        addToken(text, currentTokenStart, i + 1, TokenTypes.COMMENT_MULTILINE,
            newStartOffset + currentTokenStart);
        i++; // skip the '/'
        currentTokenType = TokenTypes.NULL;
    }
    break;
```

### 4.3. Number Quantifier Unit

**Sieve-specific:** Numbers can have an optional suffix: `K` (1024), `M` (1024^2), `G` (1024^3).

**Recommendation:** Instead of splitting `100K` into `100` + `K`, handle quantifiers as part of the
number token. Since `K`/`M`/`G` are single letters that would otherwise start an identifier, the
tokenizer should check:

```java
// In LITERAL_NUMBER_DECIMAL_INT state:
// When we encounter a letter, check if it's K, M, or G
if (c == 'K' || c == 'M' || c == 'G') {
    break; // Keep as part of number token
}
// Otherwise, end number token and re-process this character
addToken(...);
i--;
currentTokenType = TokenTypes.NULL;
```

This keeps `100K`, `5M`, `1G` as single `LITERAL_NUMBER_DECIMAL_INT` tokens.

### 4.4. String List Syntax (`["item1", "item2"]`)

**Sieve-specific:** Square brackets denote string lists. They should be `SEPARATOR` tokens.

**Recommendation:** Add `[`, `]`, `(`, `)`, `{`, `}`, `,`, and `;` as `SEPARATOR` tokens. This
enables RSyntaxTextArea's bracket matching and indentation features.

### 4.5. Backslash Escape Sequences in Strings

**Sieve-specific:** RFC 5228 defines `\"` and `\\` escape sequences in quoted strings.

**Recommendation:** In the `LITERAL_STRING_DOUBLE_QUOTE` state, when encountering `\`, skip the
next character (consume it as part of the string without checking if it's `"`):

```java
case TokenTypes.LITERAL_STRING_DOUBLE_QUOTE:
    if (c == '"') {
        addToken(text, currentTokenStart, i, TokenTypes.LITERAL_STRING_DOUBLE_QUOTE,
            newStartOffset + currentTokenStart);
        currentTokenType = TokenTypes.NULL;
    } else if (c == '\\' && i + 1 < end) {
        i++; // Skip the escaped character
    }
    break;
```

### 4.6. Sieve Identifier Rules

Per RFC: `identifier = (ALPHA / "_") *(ALPHA / DIGIT / "_")`

The current tokenizer correctly allows `/` and `_` in identifiers (line 71), but `/` is
NOT a valid Sieve identifier character per the RFC. The `/` was likely included for MIME
type support (e.g., `text/plain`), which in Sieve is actually a string argument, not an
identifier.

**Recommendation:** Remove `/` from identifier characters. MIME types like `text/plain`
appear inside quoted strings in valid Sieve scripts, not as bare identifiers.

### 4.7. Case Insensitivity

Per RFC: Tokens other than strings are case-insensitive. `TokenMap` supports case-insensitive
lookup via its constructor: `new TokenMap(true)`.

**Recommendation:** Use `new TokenMap(true)` in `getWordsToHighlight()` to handle uppercase or
mixed-case Sieve commands.

---

## 5. Summary of Required Changes to `getTokenList()`

| Priority | Change | Effort |
|---|---|---|
| **P0** | Add `SEPARATOR` cases for `;` `[` `]` `(` `)` `{` `}` `,` | Low |
| **P1** | Add colon `:` as `ANNOTATION` token start for tags | Medium |
| **P1** | Expand `getWordsToHighlight()` with all Sieve keywords | Low |
| **P2** | Add `COMMENT_MULTILINE` state for `/* */` block comments | High |
| **P2** | Handle number quantifier suffixes (`K`/`M`/`G`) in number state | Medium |
| **P3** | Handle backslash escapes in `LITERAL_STRING_DOUBLE_QUOTE` state | Medium |
| **P3** | Add `MULTILINE_STRING` cross-line state for `text:` heredocs | Very High |
| **P4** | Remove `/` from valid identifier characters | Low |
| **P4** | Custom state for `encoded-character` hex/unicode escapes | Very High |

### Action Plan for MVPR (Minimum Viable Progress)

1. **Immediately:** Add all keywords to `getWordsToHighlight()` and switch to `TokenMap(true)` for case-insensitive matching. This gives instant visual improvement with zero risk.

2. **Next:** Add separator handling for bracket characters (`[`, `]`, `(`, `)`, `{`, `}`, `;`, `,`) in `getTokenList()`. This enables proper bracket matching.

3. **Soon:** Add `ANNOTATION` token type for colon-prefixed tags in `getTokenList()`, or if keeping the minimal-change approach, add all `:tag` entries to `getWordsToHighlight()`.

4. **Later:** Implement `COMMENT_MULTILINE` handling for `/* */`, number quantifier handling, and escape sequence handling.

5. **Long-term:** Cross-line state tracking for `text:` multi-line strings.
