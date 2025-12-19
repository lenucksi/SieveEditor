package de.febrildur.sieveeditor.parser;

/**
 * Represents a Sieve filter rule with its position and metadata.
 */
public class SieveRule {
	private final int ruleNumber;
	private final String label;
	private final int lineNumber;
	private final String comment;

	public SieveRule(int ruleNumber, String label, int lineNumber, String comment) {
		this.ruleNumber = ruleNumber;
		this.label = label;
		this.lineNumber = lineNumber;
		this.comment = comment;
	}

	public int getRuleNumber() {
		return ruleNumber;
	}

	public String getLabel() {
		return label;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public String getComment() {
		return comment;
	}

	/**
	 * Gets display text for UI (e.g., "Rule 1: Spam Filter")
	 */
	public String getDisplayText() {
		if (label != null && !label.isEmpty()) {
			return "Rule " + ruleNumber + ": " + label;
		} else {
			return "Rule " + ruleNumber;
		}
	}

	@Override
	public String toString() {
		return getDisplayText();
	}
}
