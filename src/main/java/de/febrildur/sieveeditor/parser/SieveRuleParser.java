package de.febrildur.sieveeditor.parser;

import java.util.ArrayList;
import java.util.List;
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
	 * Pattern to match rule comments: ## Flag: [content]|UniqueId:N|Rulename: Description
	 * Captures:
	 *   - group 1 = Flag field content (optional)
	 *   - group 2 = unique ID number
	 *   - group 3 = rule name (without metadata)
	 *   - group 4 = LastModified timestamp (optional, vacation rules only)
	 *   - group 5 = ModifiedBy IP address (optional, vacation rules only)
	 *
	 * Made robust to handle:
	 * - Optional Flag field content (e.g., 'syscategory', 'vacation', or empty)
	 * - Variable whitespace between components
	 * - Optional trailing metadata fields (LastModified, ModifiedBy) - captured for tooltips
	 * - Optional trailing whitespace
	 * - Windows and Unix line endings
	 *
	 * Example formats supported:
	 * - Basic: ## Flag: |UniqueId:1|Rulename: My Rule
	 * - With Flag: ## Flag: vacation|UniqueId:2|Rulename: Out of Office
	 * - With metadata: ## Flag: vacation|UniqueId:2|Rulename: Abwesenheit|LastModified: 2025-12-18T13:54:27Z|ModifiedBy: 111.111.111.111
	 */
	private static final Pattern RULE_PATTERN = Pattern.compile(
		"^\\s*##\\s*Flag:\\s*([^|]*)\\|\\s*UniqueId:\\s*(\\d+)\\s*\\|\\s*Rulename:\\s*([^|]*)(?:\\|\\s*LastModified:\\s*([^|]+))?(?:\\|\\s*ModifiedBy:\\s*([^|\\s]+))?\\s*$",
		Pattern.CASE_INSENSITIVE
	);

	/**
	 * Result of parsing a Sieve script.
	 */
	public static class ParseResult {
		private final List<SieveRule> rules;
		private final List<SieveWarning> warnings;

		public ParseResult(List<SieveRule> rules, List<SieveWarning> warnings) {
			this.rules = rules;
			this.warnings = warnings;
		}

		public List<SieveRule> getRules() {
			return rules;
		}

		public List<SieveWarning> getWarnings() {
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
		List<SieveWarning> warnings = new ArrayList<>();
		java.util.Map<Integer, List<Integer>> ruleNumberToLines = new java.util.HashMap<>();

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
					String flag = matcher.group(1);          // Optional Flag field content
					int ruleNumber = Integer.parseInt(matcher.group(2));
					String ruleName = matcher.group(3).trim();
					String lastModified = matcher.group(4); // Optional vacation metadata
					String modifiedBy = matcher.group(5);   // Optional vacation metadata

					// Track all line numbers for each rule number
					ruleNumberToLines.computeIfAbsent(ruleNumber, k -> new ArrayList<>()).add(lineNumber);

					rules.add(new SieveRule(ruleNumber, ruleName, lineNumber, line.trim(), lastModified, modifiedBy, flag));
				} catch (NumberFormatException e) {
					// Shouldn't happen due to regex, but be defensive
					warnings.add(new SieveWarning(
						SieveWarning.Severity.ERROR,
						"Invalid UniqueId",
						lineNumber
					));
				}
			}
		}

		// Check for duplicate rule numbers (ERROR severity)
		for (java.util.Map.Entry<Integer, List<Integer>> entry : ruleNumberToLines.entrySet()) {
			int ruleNumber = entry.getKey();
			List<Integer> linesList = entry.getValue();
			if (linesList.size() > 1) {
				warnings.add(new SieveWarning(
					SieveWarning.Severity.ERROR,
					"Duplicate UniqueId " + ruleNumber,
					linesList
				));
			}
		}

		// Check for gaps in numbering (WARNING severity)
		if (!rules.isEmpty()) {
			detectNumberingGaps(rules, warnings);
		}

		return new ParseResult(rules, warnings);
	}

	/**
	 * Detects gaps in rule numbering sequence.
	 */
	private static void detectNumberingGaps(List<SieveRule> rules, List<SieveWarning> warnings) {
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
				// Gap detected - this is a quality WARNING, not an ERROR
				String message;
				if (number - expectedNext == 1) {
					message = "Missing UniqueId " + expectedNext;
				} else {
					message = "Missing UniqueIds " + expectedNext + " to " + (number - 1);
				}
				warnings.add(new SieveWarning(SieveWarning.Severity.WARNING, message));
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
