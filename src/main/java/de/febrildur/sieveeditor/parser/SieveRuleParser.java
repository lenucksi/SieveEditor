package de.febrildur.sieveeditor.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for extracting Sieve filter rules from script text.
 *
 * <p>This parser recognizes rule comments in the format:
 * <pre>
 * ## Flag: |UniqueId:1 |Rulename: My Rule Description
 * ## Flag: |UniqueId:2 |Rulename: Another Rule
 * </pre>
 *
 * <p>It also detects numbering inconsistencies (duplicates, gaps).
 */
public class SieveRuleParser {

	/**
	 * Pattern to match rule comments: ## Flag: |UniqueId:N |Rulename: Description
	 * Captures: group 1 = unique ID number, group 2 = rule name
	 *
	 * Made robust to handle:
	 * - Variable whitespace between components
	 * - Optional trailing whitespace
	 * - Windows and Unix line endings
	 */
	private static final Pattern RULE_PATTERN = Pattern.compile(
		"^\\s*##\\s*Flag:\\s*\\|\\s*UniqueId:\\s*(\\d+)\\s*\\|\\s*Rulename:\\s*(.*)$",
		Pattern.CASE_INSENSITIVE
	);

	/**
	 * Result of parsing a Sieve script.
	 */
	public static class ParseResult {
		private final List<SieveRule> rules;
		private final List<String> warnings;

		public ParseResult(List<SieveRule> rules, List<String> warnings) {
			this.rules = rules;
			this.warnings = warnings;
		}

		public List<SieveRule> getRules() {
			return rules;
		}

		public List<String> getWarnings() {
			return warnings;
		}

		public boolean hasWarnings() {
			return !warnings.isEmpty();
		}
	}

	/**
	 * Parses a Sieve script and extracts rule information.
	 *
	 * @param scriptText the Sieve script text
	 * @return ParseResult containing rules and any warnings
	 */
	public static ParseResult parseRules(String scriptText) {
		List<SieveRule> rules = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		Set<Integer> seenNumbers = new HashSet<>();

		if (scriptText == null || scriptText.isEmpty()) {
			return new ParseResult(rules, warnings);
		}

		// Handle both Unix (\n) and Windows (\r\n) line endings
		String[] lines = scriptText.split("\\r?\\n");
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			int lineNumber = i + 1; // 1-based line numbers

			Matcher matcher = RULE_PATTERN.matcher(line);
			if (matcher.matches()) {
				try {
					int ruleNumber = Integer.parseInt(matcher.group(1));
					String ruleName = matcher.group(2).trim();

					// Check for duplicate rule numbers
					if (seenNumbers.contains(ruleNumber)) {
						warnings.add("Duplicate UniqueId " + ruleNumber + " at line " + lineNumber);
					}
					seenNumbers.add(ruleNumber);

					rules.add(new SieveRule(ruleNumber, ruleName, lineNumber, line.trim()));
				} catch (NumberFormatException e) {
					// Shouldn't happen due to regex, but be defensive
					warnings.add("Invalid UniqueId at line " + lineNumber);
				}
			}
		}

		// Check for gaps in numbering
		if (!rules.isEmpty()) {
			detectNumberingGaps(rules, warnings);
		}

		return new ParseResult(rules, warnings);
	}

	/**
	 * Detects gaps in rule numbering sequence.
	 */
	private static void detectNumberingGaps(List<SieveRule> rules, List<String> warnings) {
		// Sort by rule number to check sequence
		List<Integer> numbers = rules.stream()
			.map(SieveRule::getRuleNumber)
			.sorted()
			.distinct()
			.toList();

		if (numbers.isEmpty()) {
			return;
		}

		int expectedNext = 1;
		for (int number : numbers) {
			if (number > expectedNext) {
				// Gap detected
				if (number - expectedNext == 1) {
					warnings.add("Missing UniqueId " + expectedNext);
				} else {
					warnings.add("Missing UniqueIds " + expectedNext + " to " + (number - 1));
				}
			}
			expectedNext = number + 1;
		}
	}

	/**
	 * Convenience method to just get the list of rules without warnings.
	 *
	 * @param scriptText the Sieve script text
	 * @return list of parsed rules
	 */
	public static List<SieveRule> extractRules(String scriptText) {
		return parseRules(scriptText).getRules();
	}
}
