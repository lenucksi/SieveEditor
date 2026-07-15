##
## ============================================================
## SECTION: Sieve Script Structure Overview
## ============================================================
##
## A Sieve script is a sequence of filter rules executed top-to-bottom.
## Each rule has a test (condition) and one or more actions.
## The last action that matches determines the message's fate.
##
## Basic structure:
##   require ["capabilities"];
##   if test_expression {
##       action;
##   } elsif other_test {
##       action;
##   } else {
##       action;
##   }
##
## Example — a complete script with two rules:
##
require ["fileinto", "reject"];

## Flag: |UniqueId:1|Rulename: File important senders
if header :contains "From" "boss@example.com" {
    fileinto "INBOX.Boss";
    stop;
}

## Flag: |UniqueId:2|Rulename: Discard spam
if header :contains "Subject" "**SPAM**" {
    discard;
}

##
## ============================================================
## SECTION: Autocomplete — How to Trigger
## ============================================================
##
## The autocomplete popup helps you write Sieve code faster.
## It appears automatically after you type (auto-activation)
## or manually with Ctrl+Space.
##
## Completions are organized into these categories:
##
##   • Control Flow Keywords  — if, elsif, else, stop, require
##   • Action Commands        — keep, discard, fileinto, redirect
##   • Test Commands          — header, address, size, exists
##   • Tags (colon-prefixed)  — :is, :contains, :matches, :over
##   • Logical Operators      — allof, anyof, not
##   • Mail Header Names      — in strings after header tests
##   • Rule Comment Templates — when typing ## at line start
##
## Auto-activation fires after any letter or colon (:).
## Typing ":" alone shows all 40+ tag completions.
## Use Up/Down arrows in the popup, Enter/Tab to select.
## Press Escape to close the popup without selecting.
## Left/Right arrows move the cursor and the popup filters live.
##
## Example — typing "if" triggers autocomplete:
##
if header :contains "Subject" "hello" {
    keep;
}

##
## ============================================================
## SECTION: Control Flow Keywords
## ============================================================
##
## These keywords control script execution flow.
##
##   if         Start a conditional block
##   elsif      Else-if condition (requires prior if)
##   else       Fallback branch
##   stop       Stop script execution for this message
##   return     Return from included script (RFC 6609)
##   break      Break out of foreverypart loop (RFC 5703)
##   foreverypart Iterate over MIME parts (RFC 5703)
##   require    Declare required Sieve capabilities
##
## Example — if/elsif/else with stop:
##
require ["fileinto"];

if header :is "From" "alice@example.com" {
    fileinto "INBOX.Family";
    stop;
} elsif header :contains "Subject" "newsletter" {
    fileinto "INBOX.News";
} else {
    keep;
}

##
## ============================================================
## SECTION: Action Commands
## ============================================================
##
## Actions do something with the message. Each action is a complete
## statement ending with semicolon.
##
##   keep         Keep message in inbox (default implicit action)
##   discard      Silently delete the message
##   fileinto     File into a specific mailbox folder
##   redirect     Forward to another email address
##   reject       Reject with delivery status notification (RFC 5429)
##   ereject      Reject with SMTP error code (RFC 5429)
##   vacation     Send auto-reply vacation notice (RFC 5230)
##   notify       Send notification (RFC 5435)
##   denotify     Cancel a notification (RFC 5435)
##   addflag      Add IMAP keyword flag (RFC 5232)
##   removeflag   Remove IMAP keyword flag (RFC 5232)
##   setflag      Set IMAP keyword flags (RFC 5232)
##   set          Set variable value (RFC 5229)
##   include      Include another Sieve script (RFC 6609)
##   global       Declare global variable (RFC 6609)
##   mailbox      Create or check mailbox (RFC 5490)
##   duplicate    Handle duplicate suppression (RFC 8579)
##   cancel       Cancel duplicate tracking (RFC 8579)
##   convert      Convert message parts (RFC 6558)
##   replace      Replace MIME part content (RFC 5703)
##   enclose      Enclose original message (RFC 5703)
##   extracttext  Extract text from message (RFC 5703)
##   deleteheader Delete header fields (RFC 5293)
##   addheader    Add header fields (RFC 5293)
##
## Examples:
##
keep;
discard;
require ["fileinto"]; fileinto :create "INBOX.Projects";
require ["copy"]; redirect :copy "archive@example.com";

require ["vacation"];
vacation :days 7 :subject "Out of office" "I am away until March 1st.";

require ["enotify"];
notify :message "New email" :importance "high" "mailto:admin@example.com";

##
## ============================================================
## SECTION: Test Commands
## ============================================================
##
## Tests are used inside if/elsif conditions to examine messages.
##
##   header         Test message headers (RFC 5228)
##   address        Test structured address headers (RFC 5228)
##   envelope       Test SMTP envelope (RFC 5228)
##   exists         Test if header exists (RFC 5228)
##   size           Test message size (RFC 5228)
##   true           Always true (RFC 5228)
##   false          Always false (RFC 5228)
##   body           Test message body (RFC 5173)
##   date           Test date/time conditions (RFC 5260)
##   currentdate    Test current date/time (RFC 5260)
##   environment    Test environment variables (RFC 5183)
##   spamtest       Test spam score (RFC 5235)
##   virustest      Test virus scan result (RFC 5235)
##   hasflag        Test IMAP flag (RFC 5232)
##   mailboxexists  Test mailbox existence (RFC 5490)
##   metadata       Test mailbox metadata (RFC 5490)
##   metadataexists Test metadata existence (RFC 5490)
##   servermetadata Test server metadata (RFC 5490)
##   ihave          Test capability availability (RFC 5463)
##   regex          Test with regex (draft)
##   list           Test list membership (RFC 6134)
##
## Examples:
##
if header :contains "Subject" "urgent" { fileinto "INBOX.Urgent"; }
if address :is :domain "From" "example.com" { fileinto "INBOX.Example"; }
if envelope :matches "From" "*-bounce@*" { discard; }
if exists "X-Spam-Flag" { fileinto "INBOX.Spam"; }
if size :over 100K { fileinto "INBOX.Large"; }
require ["body"]; if body :contains "meeting" { fileinto "INBOX.Meetings"; }

require ["date"];
if date :value "ge" :zone "-0500" "date" "year" "2024" {
    fileinto "INBOX.ThisYear";
}
if currentdate :matches "month" "12" { fileinto "INBOX.December"; }

require ["spamtest", "relational", "comparator-i;ascii-numeric"];
if spamtest :value "gt" :comparator "i;ascii-numeric" "5" {
    fileinto "INBOX.Spam";
}

require ["imapflags"];
if hasflag "\\Flagged" { fileinto "INBOX.Starred"; }

require ["ihave"];
if ihave "vacation-seconds" {
    vacation :seconds 3600 "I'm away.";
} else {
    vacation :days 7 "I'm away.";
}

##
## ============================================================
## SECTION: Tags (Colon-Prefixed Modifiers)
## ============================================================
##
## Tags modify how tests and actions behave. They always start
## with colon (:) and are highlighted as annotations.
##
## Match-type tags (RFC 5228):
##   :is         Exact string comparison
##   :contains   Substring match (default for most tests)
##   :matches    Wildcard glob match (* = any, ? = one)
##   :regex      Regular expression match (draft)
##
## Relational tags (RFC 5231):
##   :count      Compare count of matches
##   :value      Compare numeric value
##
## Address-part tags:
##   :localpart  Compare local part (before @)
##   :domain     Compare domain part (after @)
##   :all        Compare entire address (default)
##   :user       Compare user portion (RFC 5233)
##   :detail     Compare detail portion (RFC 5233)
##
## Size tags:
##   :over       Greater than specified size
##   :under      Less than specified size
##
## Vacation tags:
##   :days, :from, :subject, :handle, :mime, :text
##   :addresses, :noreply, :reply_regex, :reply_prefix
##   :sender, :database, :return_address, :header
##   :always_reply, :rfc2822, :file
##
## Action modifier tags:
##   :copy, :create, :flags, :fcc, :list, :permissions
##
## Date/Timezone tags:
##   :zone, :originalzone, :index, :last
##
## MIME tags (RFC 5703):
##   :anychild, :type, :subtype, :contenttype, :param
##
## Foreverypart tags:
##   :outer, :first, :atleast
##
## Duplicate tags (RFC 8579):
##   :uniqueid, :seconds, :limit
##
## Editheader tags (RFC 5293):
##   :deleteheaders, :addheaders, :changefield, :newfield
##
## Examples:
##
if header :is "From" "noreply@example.com" { keep; }
if header :contains "Subject" "alert" { keep; }
if header :matches "To" "*@example.com" { keep; }
if address :domain "From" "external.com" { fileinto "INBOX.External"; }
if address :localpart "To" "admin" { fileinto "INBOX.Admin"; }
if size :over 100K { discard; }
redirect :copy "backup@example.com";

##
## ============================================================
## SECTION: Logical Operators
## ============================================================
##
## Logical operators combine multiple test expressions.
##
##   allof(cond1, cond2, ...)   AND — all must be true
##   anyof(cond1, cond2, ...)   OR — at least one must be true
##   not condition              NOT — invert the condition
##
## Example — allof (all conditions must match):
##
if allof(
    header :contains "From" "newsletter@example.com",
    header :contains "Subject" "weekly"
) {
    fileinto "INBOX.WeeklyNews";
}

## Example — anyof (any condition matching):
##
if anyof(
    header :contains "From" "boss@example.com",
    header :contains "From" "ceo@example.com"
) {
    fileinto "INBOX.Executives";
}

## Example — not (negation):
##
if not header :contains "From" "spammer.com" {
    keep;
}

##
## ============================================================
## SECTION: Mail Header Completions
## ============================================================
##
## When the caret is inside a quoted string after a header, address,
## exists, or date test (or addheader/deleteheader actions), the
## autocomplete popup suggests common mail header names.
##
## Core mail headers (RFC 5322):
##   From, Sender, Reply-To, To, Cc, Bcc
##   Date, Message-ID, In-Reply-To, References, Subject
##
## Delivery / Trace headers:
##   Received, Return-Path, Delivered-To, X-Original-To, Received-SPF
##
## Mailing list headers (RFC 2369 / 2919):
##   List-Id, List-Help, List-Subscribe, List-Unsubscribe
##   List-Post, List-Owner, List-Archive, List-Unsubscribe-Post
##
## Authentication headers:
##   Authentication-Results, DKIM-Signature
##   ARC-Seal, ARC-Message-Signature, ARC-Authentication-Results
##
## Spam headers:
##   X-Spam-Flag, X-Spam-Status, X-Spam-Level (SpamAssassin)
##   X-Spamd-Result, X-Spamd-Bar, X-Rspamd-Server (Rspamd)
##   X-Spam, X-Rspamd-Action, X-Rspamd-Queue-Id
##
## Auto-reply / Priority:
##   Precedence, Auto-Submitted, X-Auto-Response-Suppress
##   X-Loop, X-Priority
##
## MIME:
##   Content-Type, Content-Disposition, Content-Transfer-Encoding
##   MIME-Version
##
## Service-specific:
##   X-Mailer, X-Campaign, X-ME-VSCategory (Fastmail)
##   X-ME-CMCategory (Fastmail), X-Complaints-To
##
## Examples — typing "Sub" after "header :contains " shows "Subject":
##
if header :contains "Subject" "urgent" { keep; }
if address :domain "From" "example.com" { keep; }
if exists "X-Spam-Flag" { discard; }
require ["editheader"]; addheader "X-Custom" "myvalue";

##
## ============================================================
## SECTION: Rule Comment Format
## ============================================================
##
## Rule comments use a structured format that the Rule Navigator
## panel reads to display rule names and flags:
##
##   ## Flag: <flag>|UniqueId:<number>|Rulename: <description>
##
## Autocomplete for rule comments appears when you type "##" at
## the start of a line. It offers templates with the next unique ID.
##
## Flag options:
##   vacation    — Auto-reply / vacation rule
##   syscategory — System category rule
##   (blank)     — Standard filter rule
##
## Examples:
##
## Flag: |UniqueId:10|Rulename: File important mail
if header :contains "From" "boss@example.com" {
    fileinto "INBOX.Boss";
}

## Flag: vacation|UniqueId:11|Rulename: Out of office reply
require ["vacation"];
vacation :days 7 "I am unavailable until next week.";

## Flag: syscategory|UniqueId:12|Rulename: Social notifications
if header :contains "X-ME-VSCategory" "social" {
    fileinto "INBOX.Social";
}

##
## ============================================================
## SECTION: Syntax Highlighting Colors
## ============================================================
##
## The editor highlights Sieve code with these token types:
##
##   RESERVED_WORD    Blue bold      — if, elsif, else, stop, require
##   COMMENT_EOL      Green          — # and ## comments
##   FUNCTION         Orange         — keep, discard, fileinto, etc.
##   RESERVED_WORD_2  Teal/cyan      — header, address, size, exists
##   OPERATOR         Red            — allof, anyof, not
##   ANNOTATION       Purple/brown   — :is, :contains, :over, :copy
##   SEPARATOR        Dark gray      — { } ( ) [ ] ;
##   LITERAL_STRING   Blue           — "double-quoted strings"
##   LITERAL_NUMBER   Black/navy     — 100K, 5M, 7
##   IDENTIFIER       Black          — mailbox names, variables
##
require ["fileinto", "vacation"];

## Flag: |UniqueId:42|Rulename: Color demo
if anyof(
    header :contains "Subject" "demo",
    address :is :domain "From" "example.com"
) {
    fileinto "INBOX.Demo";
    stop;
}

##
## ============================================================
## SECTION: Syntax Validation (Red Squiggles)
## ============================================================
##
## The built-in SieveParser checks your script for structural
## errors and shows them as red squiggly underlines plus a red
## X icon in the line number gutter.
##
## Checks performed:
##   • { } brace balance — every { needs a matching }
##   • ( ) paren balance — every ( needs a matching )
##   • [ ] bracket balance — every [ needs a closing ]
##   • " " string quotes — every " needs a closing "
##
## Correct syntax (no squiggles):
##
if header :contains "Subject" "ok" {
    keep;
}
require ["fileinto", "vacation", "copy"];

if header :matches "To" ["user1@a.com", "user2@b.com"] {
    keep;
}

if header :contains "Subject" "Quote: \"important\"" {
    keep;
}

## UNCOMMENT TO SEE ERRORS:
## if header :contains "Subject" "broken { keep;
## (missing closing brace — red squiggle on line 1)

##
## ============================================================
## SECTION: Bracket Matching and Auto-Close
## ============================================================
##
## When you type {, (, [, or ", the editor automatically inserts
## the matching closing character. The matched pair is highlighted.
##
## Auto-close works for:
##   { } — code blocks
##   ( ) — test arguments
##   [ ] — string lists
##   " " — string literals
##
## To include a literal " inside a string, use backslash: \"
##
if header :contains "From" "user@example.com" {
    ## The closing } was inserted automatically
    keep;
}
require ["fileinto"];
if header :contains "Subject" "Quote: \"important\"" {
    keep;
}

##
## ============================================================
## SECTION: Code Folding
## ============================================================
##
## Code blocks delimited by { } can be collapsed (folded) to hide
## their contents. Click the triangle in the gutter to collapse.
##
## Folding works for: if, elsif, else, foreverypart blocks.
##
## Example — foldable blocks:
##
if header :contains "Subject" "very long" {
    ## This block can be collapsed via gutter
    fileinto "INBOX.LongMessages";
    stop;
}

## Nested folding (both blocks are foldable):
if header :contains "From" "boss" {
    if header :contains "Subject" "urgent" {
        fileinto "INBOX.Boss.Urgent";
        stop;
    }
}
