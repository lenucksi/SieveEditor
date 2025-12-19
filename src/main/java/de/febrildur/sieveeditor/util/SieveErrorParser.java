package de.febrildur.sieveeditor.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Optional;

/**
 * Utility class for parsing Sieve script error messages from ManageSieve servers.
 *
 * According to RFC 5804, error messages SHOULD contain line numbers but the format
 * is not strictly defined. This parser attempts to extract line numbers from
 * common server error message formats.
 */
public class SieveErrorParser {

	/**
	 * Patterns for extracting line numbers from various server error formats.
	 * Covers common formats from Dovecot, Cyrus IMAP, and other implementations.
	 */
	private static final Pattern[] LINE_NUMBER_PATTERNS = {
		// "line 42:" or "line 42 "
		Pattern.compile("line\\s+(\\d+)\\s*[:\\s]", Pattern.CASE_INSENSITIVE),
		// "on line 42" or "at line 42"
		Pattern.compile("(?:on|at)\\s+line\\s+(\\d+)", Pattern.CASE_INSENSITIVE),
		// "(line 42)" or "[line 42]"
		Pattern.compile("[\\(\\[]line\\s+(\\d+)[\\)\\]]", Pattern.CASE_INSENSITIVE),
		// "42:" at start (common compiler format)
		Pattern.compile("^(\\d+)\\s*:"),
		// ":42:" (some formats use this)
		Pattern.compile(":(\\d+):"),
	};

	/**
	 * Result of parsing an error message.
	 */
	public static class ErrorInfo {
		private final String message;
		private final Integer lineNumber;

		public ErrorInfo(String message, Integer lineNumber) {
			this.message = message;
			this.lineNumber = lineNumber;
		}

		public String getMessage() {
			return message;
		}

		public Optional<Integer> getLineNumber() {
			return Optional.ofNullable(lineNumber);
		}

		public boolean hasLineNumber() {
			return lineNumber != null;
		}
	}

	/**
	 * Parses a Sieve error message and attempts to extract line number information.
	 *
	 * @param errorMessage the error message from the ManageSieve server
	 * @return ErrorInfo containing the message and optional line number
	 */
	public static ErrorInfo parseError(String errorMessage) {
		if (errorMessage == null || errorMessage.trim().isEmpty()) {
			return new ErrorInfo("Unknown error", null);
		}

		// Try each pattern to find a line number
		for (Pattern pattern : LINE_NUMBER_PATTERNS) {
			Matcher matcher = pattern.matcher(errorMessage);
			if (matcher.find()) {
				try {
					int lineNumber = Integer.parseInt(matcher.group(1));
					// Line numbers should be positive
					if (lineNumber > 0) {
						return new ErrorInfo(errorMessage, lineNumber);
					}
				} catch (NumberFormatException e) {
					// Continue to next pattern
				}
			}
		}

		// No line number found
		return new ErrorInfo(errorMessage, null);
	}

	/**
	 * Convenience method to extract just the line number if present.
	 *
	 * @param errorMessage the error message from the ManageSieve server
	 * @return Optional containing the line number if found
	 */
	public static Optional<Integer> extractLineNumber(String errorMessage) {
		return parseError(errorMessage).getLineNumber();
	}
}
