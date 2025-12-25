package de.febrildur.sieveeditor.parser;

/**
 * Represents a Sieve filter rule with its position and metadata.
 */
public class SieveRule {
	private final int ruleNumber;
	private final String label;
	private final int lineNumber;
	private final String comment;
	private final String lastModified;  // Optional: LastModified timestamp (vacation rules)
	private final String modifiedBy;    // Optional: ModifiedBy IP address (vacation rules)
	private final String flag;          // Optional: Flag field content (vacation, syscategory, etc.)

	public SieveRule(int ruleNumber, String label, int lineNumber, String comment) {
		this(ruleNumber, label, lineNumber, comment, null, null, null);
	}

	/**
	 * Constructor with optional vacation metadata.
	 *
	 * @param ruleNumber the unique rule ID
	 * @param label the rule name/label
	 * @param lineNumber the line number in the script
	 * @param comment the full comment line
	 * @param lastModified ISO 8601 timestamp of last modification (may be null)
	 * @param modifiedBy IP address of last modifier (may be null)
	 * @param flag Flag field content (vacation, syscategory, etc., may be null or empty)
	 */
	public SieveRule(int ruleNumber, String label, int lineNumber, String comment,
					 String lastModified, String modifiedBy, String flag) {
		this.ruleNumber = ruleNumber;
		this.label = label;
		this.lineNumber = lineNumber;
		this.comment = comment;
		this.lastModified = lastModified;
		this.modifiedBy = modifiedBy;
		this.flag = flag;
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
	 * Gets the LastModified timestamp (vacation rules only).
	 *
	 * @return ISO 8601 timestamp or null if not a vacation rule
	 */
	public String getLastModified() {
		return lastModified;
	}

	/**
	 * Gets the ModifiedBy IP address (vacation rules only).
	 *
	 * @return IP address or null if not a vacation rule
	 */
	public String getModifiedBy() {
		return modifiedBy;
	}

	/**
	 * Gets the Flag field content.
	 *
	 * @return Flag content (vacation, syscategory, etc.) or null/empty if not set
	 */
	public String getFlag() {
		return flag;
	}

	/**
	 * Checks if this rule has vacation metadata.
	 *
	 * @return true if lastModified or modifiedBy is present
	 */
	public boolean hasMetadata() {
		return lastModified != null || modifiedBy != null;
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
