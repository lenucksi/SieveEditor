package de.febrildur.sieveeditor.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a validation warning or error from parsing Sieve scripts.
 *
 * <p>Warnings have different severity levels:
 * <ul>
 *   <li>ERROR - Critical issues like duplicate or invalid UniqueIds</li>
 *   <li>WARNING - Quality issues like missing UniqueIds in sequence</li>
 * </ul>
 */
public class SieveWarning {

	public enum Severity {
		ERROR,   // Dark red - critical issues
		WARNING  // Dark orange - quality issues
	}

	private final Severity severity;
	private final String message;
	private final List<Integer> lineNumbers;
	private int currentLineIndex = 0; // For cycling through multiple locations

	/**
	 * Creates a warning with a single line number.
	 */
	public SieveWarning(Severity severity, String message, int lineNumber) {
		this.severity = severity;
		this.message = message;
		this.lineNumbers = new ArrayList<>();
		this.lineNumbers.add(lineNumber);
	}

	/**
	 * Creates a warning with multiple line numbers (e.g., for duplicates).
	 */
	public SieveWarning(Severity severity, String message, List<Integer> lineNumbers) {
		this.severity = severity;
		this.message = message;
		this.lineNumbers = new ArrayList<>(lineNumbers);
	}

	/**
	 * Creates a warning without a specific line number (e.g., missing IDs).
	 */
	public SieveWarning(Severity severity, String message) {
		this.severity = severity;
		this.message = message;
		this.lineNumbers = new ArrayList<>();
	}

	public Severity getSeverity() {
		return severity;
	}

	public String getMessage() {
		return message;
	}

	public List<Integer> getLineNumbers() {
		return lineNumbers;
	}

	public boolean hasLineNumbers() {
		return !lineNumbers.isEmpty();
	}

	/**
	 * Gets the current line number for navigation (cycles through all lines).
	 * Returns null if no line numbers are associated.
	 */
	public Integer getCurrentLineNumber() {
		if (lineNumbers.isEmpty()) {
			return null;
		}
		return lineNumbers.get(currentLineIndex);
	}

	/**
	 * Advances to the next line number for cycling navigation.
	 * Wraps around to the first line after the last one.
	 */
	public void cycleToNextLine() {
		if (!lineNumbers.isEmpty()) {
			currentLineIndex = (currentLineIndex + 1) % lineNumbers.size();
		}
	}

	/**
	 * Resets the cycle to the first line number.
	 */
	public void resetCycle() {
		currentLineIndex = 0;
	}

	/**
	 * Gets display text for the warning list.
	 */
	public String getDisplayText() {
		String icon = severity == Severity.ERROR ? "⛔" : "⚠";

		if (lineNumbers.isEmpty()) {
			return icon + " " + message;
		} else if (lineNumbers.size() == 1) {
			return icon + " " + message + " at line " + lineNumbers.get(0);
		} else {
			// Multiple lines - show all locations
			StringBuilder sb = new StringBuilder();
			sb.append(icon).append(" ").append(message).append(" at lines ");
			for (int i = 0; i < lineNumbers.size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(lineNumbers.get(i));
			}
			return sb.toString();
		}
	}

	@Override
	public String toString() {
		return getDisplayText();
	}
}
