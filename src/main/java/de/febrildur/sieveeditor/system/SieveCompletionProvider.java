package de.febrildur.sieveeditor.system;
// SPDX-FileCopyrightText: 2025 Claude
// SPDX-FileCopyrightText: 2025 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;

/**
 * A completion provider for Sieve language keywords.
 * Provides autocomplete suggestions for Sieve control flow commands,
 * action commands, test commands, logical operators, and tags.
 * <p>
 * Keywords and their RFC origins are documented in each completion's
 * short description for easy reference.
 * </p>
 */
public class SieveCompletionProvider extends DefaultCompletionProvider {

	/**
	 * Constructs a new SieveCompletionProvider and populates it with
	 * all Sieve language keywords.
	 */
	public SieveCompletionProvider() {
		setAutoActivationRules(true, ":");

		addControlFlowCompletions();
		addActionCompletions();
		addTestCompletions();
		addLogicalOperatorCompletions();
		addTagCompletions();
	}

	/**
	 * Include ':' so that ":cont" extracts ":cont" as input
	 * and matches completions like ":contains". Without this,
	 * DefaultCompletionProvider only sees "cont" which never
	 * matches any tag completion (all start with ':').
	 */
	@Override
	protected boolean isValidChar(char ch) {
		return super.isValidChar(ch) || ch == ':';
	}

	/**
	 * Adds Sieve control flow commands.
	 *
	 * RFC 5228: Sieve base specification control structures.
	 */
	private void addControlFlowCompletions() {
		addCompletion(new BasicCompletion(this, "if",
			"if - Control flow conditional (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "elsif",
			"elsif - Else-if conditional (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "else",
			"else - Else conditional (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "stop",
			"stop - Stop script execution (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "return",
			"return - Return from included script (RFC 6609)"));
		addCompletion(new BasicCompletion(this, "break",
			"break - Break out of foreverypart loop (RFC 5703)"));
		addCompletion(new BasicCompletion(this, "foreverypart",
			"foreverypart - Iterate over MIME parts (RFC 5703)"));
		addCompletion(new BasicCompletion(this, "require",
			"require - Declare capability requirements (RFC 5228)"));
	}

	/**
	 * Adds Sieve action commands.
	 *
	 * These are built-in action functions that the Sieve engine executes.
	 */
	private void addActionCompletions() {
		addCompletion(new BasicCompletion(this, "keep",
			"keep - Keep message in mailbox (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "discard",
			"discard - Silently discard message (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "fileinto",
			"fileinto - File message into mailbox (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "redirect",
			"redirect - Redirect message to another address (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "reject",
			"reject - Reject message with MDN (RFC 5429)"));
		addCompletion(new BasicCompletion(this, "ereject",
			"ereject - Reject message with SMTP error (RFC 5429)"));
		addCompletion(new BasicCompletion(this, "vacation",
			"vacation - Send auto-reply vacation notice (RFC 5230)"));
		addCompletion(new BasicCompletion(this, "notify",
			"notify - Send notification (RFC 5435)"));
		addCompletion(new BasicCompletion(this, "denotify",
			"denotify - Cancel notification (RFC 5435)"));
		addCompletion(new BasicCompletion(this, "addflag",
			"addflag - Add flag to message (RFC 5232)"));
		addCompletion(new BasicCompletion(this, "removeflag",
			"removeflag - Remove flag from message (RFC 5232)"));
		addCompletion(new BasicCompletion(this, "setflag",
			"setflag - Set flags on message (RFC 5232)"));
		addCompletion(new BasicCompletion(this, "set",
			"set - Set variable value (RFC 5229)"));
		addCompletion(new BasicCompletion(this, "include",
			"include - Include another Sieve script (RFC 6609)"));
		addCompletion(new BasicCompletion(this, "global",
			"global - Declare global variables (RFC 6609)"));
		addCompletion(new BasicCompletion(this, "mailbox",
			"mailbox - Create or check mailbox (RFC 5490)"));
		addCompletion(new BasicCompletion(this, "duplicate",
			"duplicate - Handle duplicate detection (RFC 8579)"));
		addCompletion(new BasicCompletion(this, "cancel",
			"cancel - Cancel duplicate tracking (RFC 8579)"));
		addCompletion(new BasicCompletion(this, "convert",
			"convert - Convert message parts (RFC 6558)"));
		addCompletion(new BasicCompletion(this, "replace",
			"replace - Replace MIME part content (RFC 5703)"));
		addCompletion(new BasicCompletion(this, "enclose",
			"enclose - Enclose original message (RFC 5703)"));
		addCompletion(new BasicCompletion(this, "extracttext",
			"extracttext - Extract text from message (RFC 5703)"));
		addCompletion(new BasicCompletion(this, "deleteheader",
			"deleteheader - Delete message header fields (RFC 5293)"));
		addCompletion(new BasicCompletion(this, "addheader",
			"addheader - Add message header fields (RFC 5293)"));
	}

	/**
	 * Adds Sieve test commands.
	 *
	 * Tests are used within conditional contexts to examine message properties.
	 */
	private void addTestCompletions() {
		addCompletion(new BasicCompletion(this, "address",
			"address - Test address header fields (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "envelope",
			"envelope - Test SMTP envelope (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "exists",
			"exists - Test if header exists (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "header",
			"header - Test message header fields (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "size",
			"size - Test message size (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "true",
			"true - Boolean true test (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "false",
			"false - Boolean false test (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "body",
			"body - Test message body content (RFC 5173)"));
		addCompletion(new BasicCompletion(this, "date",
			"date - Test date and time conditions (RFC 5260)"));
		addCompletion(new BasicCompletion(this, "currentdate",
			"currentdate - Test current date/time (RFC 5260)"));
		addCompletion(new BasicCompletion(this, "environment",
			"environment - Test environment variables (RFC 5183)"));
		addCompletion(new BasicCompletion(this, "spamtest",
			"spamtest - Test spam score (RFC 5235)"));
		addCompletion(new BasicCompletion(this, "virustest",
			"virustest - Test virus scan result (RFC 5235)"));
		addCompletion(new BasicCompletion(this, "hasflag",
			"hasflag - Test if flags are set (RFC 5232)"));
		addCompletion(new BasicCompletion(this, "mailboxexists",
			"mailboxexists - Test if mailbox exists (RFC 5490)"));
		addCompletion(new BasicCompletion(this, "metadata",
			"metadata - Test mailbox metadata (RFC 5490)"));
		addCompletion(new BasicCompletion(this, "metadataexists",
			"metadataexists - Test if metadata exists (RFC 5490)"));
		addCompletion(new BasicCompletion(this, "servermetadata",
			"servermetadata - Test server metadata (RFC 5490)"));
		addCompletion(new BasicCompletion(this, "servermetadataexists",
			"servermetadataexists - Test server metadata existence (RFC 5490)"));
		addCompletion(new BasicCompletion(this, "ihave",
			"ihave - Test if capability is available (RFC 5463)"));
		addCompletion(new BasicCompletion(this, "regex",
			"regex - Test with regular expressions (draft)"));
		addCompletion(new BasicCompletion(this, "list",
			"list - Test list membership (RFC 6134)"));
	}

	/**
	 * Adds Sieve logical operators for composing test expressions.
	 */
	private void addLogicalOperatorCompletions() {
		addCompletion(new BasicCompletion(this, "allof",
			"allof - Logical AND of tests (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "anyof",
			"anyof - Logical OR of tests (RFC 5228)"));
		addCompletion(new BasicCompletion(this, "not",
			"not - Logical NOT of test (RFC 5228)"));
	}

	/**
	 * Adds Sieve colon-prefixed tags.
	 *
	 * Tags modify how commands and tests behave and are a core part
	 * of Sieve's syntax for parameterizing operations.
	 */
	private void addTagCompletions() {
		// --- Match-Type Tags (RFC 5228) ---
		addCompletion(new BasicCompletion(this, ":is",
			":is - Exact string match (RFC 5228)"));
		addCompletion(new BasicCompletion(this, ":contains",
			":contains - Substring match (RFC 5228)"));
		addCompletion(new BasicCompletion(this, ":matches",
			":matches - Wildcard glob match (RFC 5228)"));
		addCompletion(new BasicCompletion(this, ":regex",
			":regex - Regular expression match (draft)"));

		// --- Relational Tags (RFC 5231) ---
		addCompletion(new BasicCompletion(this, ":count",
			":count - Relational count comparison (RFC 5231)"));
		addCompletion(new BasicCompletion(this, ":value",
			":value - Relational value comparison (RFC 5231)"));

		// --- Address-Part Tags (RFC 5228) ---
		addCompletion(new BasicCompletion(this, ":localpart",
			":localpart - Compare local part of address (RFC 5228)"));
		addCompletion(new BasicCompletion(this, ":domain",
			":domain - Compare domain part of address (RFC 5228)"));
		addCompletion(new BasicCompletion(this, ":all",
			":all - Compare entire address (RFC 5228)"));
		addCompletion(new BasicCompletion(this, ":user",
			":user - Compare user part (RFC 5228)"));
		addCompletion(new BasicCompletion(this, ":detail",
			":detail - Compare address detail (RFC 5228)"));

		// --- Comparator Tag (RFC 5228) ---
		addCompletion(new BasicCompletion(this, ":comparator",
			":comparator - Specify comparison algorithm (RFC 5228)"));

		// --- Size Tags (RFC 5228) ---
		addCompletion(new BasicCompletion(this, ":over",
			":over - Size greater than (RFC 5228)"));
		addCompletion(new BasicCompletion(this, ":under",
			":under - Size less than (RFC 5228)"));

		// --- Action Modifier Tags ---
		addCompletion(new BasicCompletion(this, ":copy",
			":copy - Copy message without removing original (RFC 3894)"));
		addCompletion(new BasicCompletion(this, ":create",
			":create - Create mailbox if needed (RFC 5490)"));
		addCompletion(new BasicCompletion(this, ":flags",
			":flags - Specify mailbox flags (RFC 5232)"));
		addCompletion(new BasicCompletion(this, ":fcc",
			":fcc - File copy of redirect into folder (RFC 3894)"));
		addCompletion(new BasicCompletion(this, ":list",
			":list - Specify list for notification (RFC 5435)"));
		addCompletion(new BasicCompletion(this, ":permissions",
			":permissions - Set mailbox permissions (RFC 5490)"));

		// --- Vacation Tags (RFC 5230) ---
		addCompletion(new BasicCompletion(this, ":days",
			":days - Vacation reply interval (RFC 5230)"));
		addCompletion(new BasicCompletion(this, ":from",
			":from - Vacation from address (RFC 5230)"));
		addCompletion(new BasicCompletion(this, ":subject",
			":subject - Vacation subject line (RFC 5230)"));
		addCompletion(new BasicCompletion(this, ":handle",
			":handle - Vacation response handle (RFC 5230)"));
		addCompletion(new BasicCompletion(this, ":mime",
			":mime - Vacation MIME response (RFC 5230)"));
		addCompletion(new BasicCompletion(this, ":text",
			":text - Vacation text response (RFC 5230)"));
		addCompletion(new BasicCompletion(this, ":addresses",
			":addresses - Vacation matching addresses (RFC 5230)"));
		addCompletion(new BasicCompletion(this, ":noreply",
			":noreply - Do not send vacation reply (RFC 5230)"));
		addCompletion(new BasicCompletion(this, ":reply_regex",
			":reply_regex - Vacation reply regex filter (RFC 5230)"));
		addCompletion(new BasicCompletion(this, ":reply_prefix",
			":reply_prefix - Vacation reply prefix (RFC 5230)"));
		addCompletion(new BasicCompletion(this, ":sender",
			":sender - Vacation sender address (RFC 5230)"));
		addCompletion(new BasicCompletion(this, ":database",
			":database - Vacation database path (RFC 5230)"));
		addCompletion(new BasicCompletion(this, ":return_address",
			":return_address - Vacation return address (RFC 5230)"));
		addCompletion(new BasicCompletion(this, ":header",
			":header - Vacation header matching (RFC 5230)"));
		addCompletion(new BasicCompletion(this, ":always_reply",
			":always_reply - Always send vacation reply (RFC 5230)"));
		addCompletion(new BasicCompletion(this, ":rfc2822",
			":rfc2822 - Vacation RFC 2822 date format (RFC 5260)"));
		addCompletion(new BasicCompletion(this, ":file",
			":file - Vacation file response template (RFC 5230)"));

		// --- Notification Tags (RFC 5435) ---
		addCompletion(new BasicCompletion(this, ":importance",
			":importance - Notification importance level (RFC 5435)"));
		addCompletion(new BasicCompletion(this, ":message",
			":message - Notification message text (RFC 5435)"));
		addCompletion(new BasicCompletion(this, ":options",
			":options - Notification method options (RFC 5435)"));
		addCompletion(new BasicCompletion(this, ":priority",
			":priority - Notification priority (RFC 5435)"));

		// --- Date/Timezone Tags (RFC 5260) ---
		addCompletion(new BasicCompletion(this, ":zone",
			":zone - Specify timezone for date test (RFC 5260)"));
		addCompletion(new BasicCompletion(this, ":originalzone",
			":originalzone - Use original timezone (RFC 5260)"));
		addCompletion(new BasicCompletion(this, ":index",
			":index - Header field index (RFC 5293)"));
		addCompletion(new BasicCompletion(this, ":last",
			":last - Last occurrence (RFC 5293)"));

		// --- MIME Tags (RFC 5703) ---
		addCompletion(new BasicCompletion(this, ":anychild",
			":anychild - Match any MIME child part (RFC 5703)"));
		addCompletion(new BasicCompletion(this, ":type",
			":type - MIME content type (RFC 5703)"));
		addCompletion(new BasicCompletion(this, ":subtype",
			":subtype - MIME content subtype (RFC 5703)"));
		addCompletion(new BasicCompletion(this, ":contenttype",
			":contenttype - Full MIME content type (RFC 5703)"));
		addCompletion(new BasicCompletion(this, ":param",
			":param - MIME parameter value (RFC 5703)"));

		// --- Foreverypart Tags (RFC 5703) ---
		addCompletion(new BasicCompletion(this, ":outer",
			":outer - Match outer MIME part (RFC 5703)"));
		addCompletion(new BasicCompletion(this, ":first",
			":first - Match first MIME part (RFC 5703)"));
		addCompletion(new BasicCompletion(this, ":atleast",
			":atleast - Match at least N parts (RFC 5703)"));

		// --- Duplicate Tags (RFC 8579) ---
		addCompletion(new BasicCompletion(this, ":uniqueid",
			":uniqueid - Duplicate unique ID (RFC 8579)"));
		addCompletion(new BasicCompletion(this, ":seconds",
			":seconds - Duplicate tracking window (RFC 8579)"));
		addCompletion(new BasicCompletion(this, ":limit",
			":limit - Duplicate limit count (RFC 8579)"));

		// --- Editheader Tags (RFC 5293) ---
		addCompletion(new BasicCompletion(this, ":deleteheaders",
			":deleteheaders - Delete matching headers (RFC 5293)"));
		addCompletion(new BasicCompletion(this, ":addheaders",
			":addheaders - Add multiple headers (RFC 5293)"));
		addCompletion(new BasicCompletion(this, ":changefield",
			":changefield - Change header field value (RFC 5293)"));
		addCompletion(new BasicCompletion(this, ":newfield",
			":newfield - New header field value (RFC 5293)"));

		// --- Extlists Tag (RFC 6134) ---
		addCompletion(new BasicCompletion(this, ":addrbook",
			":addrbook - Address book list (RFC 6134)"));

		// --- Convert Tag (RFC 6558) ---
		addCompletion(new BasicCompletion(this, ":modifier",
			":modifier - Convert modifier parameter (RFC 6558)"));
	}
}
