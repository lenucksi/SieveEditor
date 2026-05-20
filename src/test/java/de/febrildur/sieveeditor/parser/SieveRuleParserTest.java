package de.febrildur.sieveeditor.parser;
// SPDX-FileCopyrightText: 2025 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SieveRuleParserTest {

	@Test
	void shouldParseBasicRule() {
		// Given
		String script = "## Flag: |UniqueId:1 |Rulename: Spam filter\nif header :contains \"subject\" \"spam\" { discard; }";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		assertThat(result.getRules().get(0).getRuleNumber()).isEqualTo(1);
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("Spam filter");
		assertThat(result.getRules().get(0).getLineNumber()).isEqualTo(1);
	}

	@Test
	void shouldParseMultipleRules() {
		// Given
		String script = """
			## Flag: |UniqueId:1 |Rulename: Spam filter
			if header :contains "subject" "spam" { discard; }

			## Flag: |UniqueId:2 |Rulename: Important mail
			if from "boss@example.com" { fileinto "Important"; }

			## Flag: |UniqueId:3 |Rulename: Newsletter
			if header :contains "list-id" "newsletter" { fileinto "Newsletter"; }
			""";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(3);
		assertThat(result.getRules().get(0).getRuleNumber()).isEqualTo(1);
		assertThat(result.getRules().get(1).getRuleNumber()).isEqualTo(2);
		assertThat(result.getRules().get(2).getRuleNumber()).isEqualTo(3);
	}

	@Test
	void shouldParseCaseInsensitive() {
		// Given
		String script = "## FLAG: |UNIQUEID:1 |RULENAME: Test\n## flag: |uniqueid:2 |rulename: Another";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(2);
	}

	@Test
	void shouldParseRuleWithoutRulename() {
		// Given
		String script = "## Flag: |UniqueId:1 |Rulename:";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		assertThat(result.getRules().get(0).getLabel()).isEmpty();
	}

	@Test
	void shouldDetectDuplicateUniqueIds() {
		// Given
		String script = """
			## Flag: |UniqueId:1 |Rulename: First
			## Flag: |UniqueId:1 |Rulename: Duplicate
			## Flag: |UniqueId:2 |Rulename: OK
			""";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(3);
		assertThat(result.hasWarnings()).isTrue();
		assertThat(result.getWarnings())
			.anyMatch(w -> w.getMessage().contains("Duplicate UniqueId 1"));
	}

	@Test
	void shouldDetectGapsInNumbering() {
		// Given
		String script = """
			## Flag: |UniqueId:1 |Rulename: First
			## Flag: |UniqueId:3 |Rulename: Third (missing 2)
			## Flag: |UniqueId:5 |Rulename: Fifth (missing 4)
			""";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(3);
		assertThat(result.hasWarnings()).isTrue();
		assertThat(result.getWarnings())
			.anyMatch(w -> w.getMessage().contains("Missing UniqueId 2"))
			.anyMatch(w -> w.getMessage().contains("Missing UniqueId 4"));
	}

	@Test
	void shouldDetectMultipleConsecutiveMissingIds() {
		// Given
		String script = """
			## Flag: |UniqueId:1 |Rulename: First
			## Flag: |UniqueId:5 |Rulename: Fifth (missing 2, 3, 4)
			""";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.hasWarnings()).isTrue();
		assertThat(result.getWarnings())
			.anyMatch(w -> w.getMessage().contains("Missing UniqueIds 2 to 4"));
	}

	@Test
	void shouldHandleEmptyScript() {
		// When
		var result = SieveRuleParser.parseRules("");

		// Then
		assertThat(result.getRules()).isEmpty();
		assertThat(result.getWarnings()).isEmpty();
	}

	@Test
	void shouldHandleNullScript() {
		// When
		var result = SieveRuleParser.parseRules(null);

		// Then
		assertThat(result.getRules()).isEmpty();
		assertThat(result.getWarnings()).isEmpty();
	}

	@Test
	void shouldIgnoreNonRuleComments() {
		// Given
		String script = """
			# This is just a comment
			## Flag: |UniqueId:1 |Rulename: Real rule
			# Another comment
			# Not a rule comment
			## Flag: |UniqueId:2 |Rulename: Another rule
			""";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(2);
	}

	@Test
	void shouldHandleIndentedComments() {
		// Given
		String script = """
			  ## Flag: |UniqueId:1 |Rulename: Indented
			\t## Flag: |UniqueId:2 |Rulename: Tab indented
			""";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(2);
	}

	@Test
	void shouldTrackCorrectLineNumbers() {
		// Given
		String script = """
			# Some comment
			## Flag: |UniqueId:1 |Rulename: First (line 2)

			# Another comment
			## Flag: |UniqueId:2 |Rulename: Second (line 5)
			""";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(2);
		assertThat(result.getRules().get(0).getLineNumber()).isEqualTo(2);
		assertThat(result.getRules().get(1).getLineNumber()).isEqualTo(5);
	}

	@Test
	void shouldGenerateDisplayText() {
		// Given
		String script = """
			## Flag: |UniqueId:1 |Rulename: With label
			## Flag: |UniqueId:2 |Rulename:
			""";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules().get(0).getDisplayText()).isEqualTo("Rule 1: With label");
		assertThat(result.getRules().get(1).getDisplayText()).isEqualTo("Rule 2");
	}

	@Test
	void shouldExtractRulesConvenienceMethod() {
		// Given
		String script = "## Flag: |UniqueId:1 |Rulename: Test\n## Flag: |UniqueId:2 |Rulename: Another";

		// When
		var rules = SieveRuleParser.extractRules(script);

		// Then
		assertThat(rules).hasSize(2);
	}

	@Test
	void shouldHandleNoWarningsCase() {
		// Given
		String script = """
			## Flag: |UniqueId:1 |Rulename: First
			## Flag: |UniqueId:2 |Rulename: Second
			## Flag: |UniqueId:3 |Rulename: Third
			""";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.hasWarnings()).isFalse();
		assertThat(result.getWarnings()).isEmpty();
	}

	@Test
	void shouldParseRuleWithSpacesInRulename() {
		// Given
		String script = "## Flag: |UniqueId:1 |Rulename: This is a longer rule name with spaces";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("This is a longer rule name with spaces");
	}

	@Test
	void shouldHandleExtraWhitespace() {
		// Given
		String script = "##  Flag:  |UniqueId:42  |Rulename:  Test Rule  ";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		assertThat(result.getRules().get(0).getRuleNumber()).isEqualTo(42);
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("Test Rule");
	}

	@Test
	void shouldHandleWindowsLineEndings() {
		// Given
		String script = "## Flag: |UniqueId:1 |Rulename: First\r\n## Flag: |UniqueId:2 |Rulename: Second\r\n";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(2);
		assertThat(result.getRules().get(0).getRuleNumber()).isEqualTo(1);
		assertThat(result.getRules().get(1).getRuleNumber()).isEqualTo(2);
	}

	@Test
	void shouldHandleMixedLineEndings() {
		// Given
		String script = "## Flag: |UniqueId:1 |Rulename: Unix\n## Flag: |UniqueId:2 |Rulename: Windows\r\n## Flag: |UniqueId:3 |Rulename: Another Unix\n";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(3);
	}

	@Test
	void shouldHandleWhitespaceAroundPipes() {
		// Given - whitespace between | and UniqueId/Rulename
		String script = "## Flag: | UniqueId:1 | Rulename: Test";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		assertThat(result.getRules().get(0).getRuleNumber()).isEqualTo(1);
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("Test");
	}

	@Test
	void shouldHandleCompactFormat() {
		// Given - minimal whitespace (should still work)
		String script = "##Flag:|UniqueId:1|Rulename:Test Rule";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		assertThat(result.getRules().get(0).getRuleNumber()).isEqualTo(1);
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("Test Rule");
	}

	@Test
	void shouldHandleTrailingWhitespace() {
		// Given - trailing whitespace and tabs
		String script = "## Flag: |UniqueId:1 |Rulename: Test\t\t  \n## Flag: |UniqueId:2 |Rulename: Another  \t";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(2);
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("Test");
		assertThat(result.getRules().get(1).getLabel()).isEqualTo("Another");
	}


	@Test
	void shouldHandleOxFlagWithEmptyContent() {
		// Given - OX format with empty Flag field (backward compatibility)
		String script = "## Flag: |UniqueId:1|Rulename: Empty Flag Test";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		assertThat(result.getRules().get(0).getRuleNumber()).isEqualTo(1);
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("Empty Flag Test");
	}

	@Test
	void shouldHandleOxFlagWithVacationContent() {
		// Given - OX format with 'vacation' in Flag field
		String script = "## Flag: vacation|UniqueId:1|Rulename: Out of Office";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		assertThat(result.getRules().get(0).getRuleNumber()).isEqualTo(1);
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("Out of Office");
	}

	@Test
	void shouldHandleOxFlagWithSyscategoryContent() {
		// Given - OX format with 'syscategory' in Flag field (real-world example)
		String script = "## Flag: syscategory|UniqueId:1|Rulename: $purchases";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		assertThat(result.getRules().get(0).getRuleNumber()).isEqualTo(1);
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("$purchases");
	}

	@Test
	void shouldHandleMixedFlagContentInMultipleRules() {
		// Given - Mix of empty, vacation, and syscategory flags
		String script = """
			## Flag: |UniqueId:1|Rulename: Normal Rule
			## Flag: vacation|UniqueId:2|Rulename: Vacation Response
			## Flag: syscategory|UniqueId:3|Rulename: System Filter
			## Flag: custom_value|UniqueId:4|Rulename: Custom Rule
			""";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(4);
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("Normal Rule");
		assertThat(result.getRules().get(1).getLabel()).isEqualTo("Vacation Response");
		assertThat(result.getRules().get(2).getLabel()).isEqualTo("System Filter");
		assertThat(result.getRules().get(3).getLabel()).isEqualTo("Custom Rule");
	}

	@Test
	void shouldHandleOxFlagWithSpacesInContent() {
		// Given - Flag field with spaces (edge case)
		String script = "## Flag: some value with spaces|UniqueId:1|Rulename: Spaced Flag";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		assertThat(result.getRules().get(0).getRuleNumber()).isEqualTo(1);
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("Spaced Flag");
	}

	@Test
	void shouldHandleOxFlagWithSpecialCharacters() {
		// Given - Flag field with special characters (edge case)
		String script = "## Flag: sys:category-type_v2|UniqueId:42|Rulename: Special Chars";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		assertThat(result.getRules().get(0).getRuleNumber()).isEqualTo(42);
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("Special Chars");
	}

	@Test
	void shouldHandleVacationRuleWithLastModifiedAndModifiedBy() {
		// Given - Real-world vacation format with metadata fields
		String script = "## Flag: vacation|UniqueId:2|Rulename: Abwesenheitsbenachrichtigung|LastModified: 2025-12-18T13:54:27Z|ModifiedBy: 111.111.111.111";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		SieveRule rule = result.getRules().get(0);
		assertThat(rule.getRuleNumber()).isEqualTo(2);
		assertThat(rule.getLabel()).isEqualTo("Abwesenheitsbenachrichtigung");
		assertThat(rule.getDisplayText()).isEqualTo("Rule 2: Abwesenheitsbenachrichtigung");

		// Verify metadata was extracted
		assertThat(rule.hasMetadata()).isTrue();
		assertThat(rule.getLastModified()).isEqualTo("2025-12-18T13:54:27Z");
		assertThat(rule.getModifiedBy()).isEqualTo("111.111.111.111");
	}

	@Test
	void shouldHandleVacationRuleWithOnlyLastModified() {
		// Given - Vacation format with only LastModified (edge case)
		String script = "## Flag: vacation|UniqueId:3|Rulename: Out of Office|LastModified: 2025-12-20T10:00:00Z";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		SieveRule rule = result.getRules().get(0);
		assertThat(rule.getRuleNumber()).isEqualTo(3);
		assertThat(rule.getLabel()).isEqualTo("Out of Office");

		// Verify only LastModified is present
		assertThat(rule.hasMetadata()).isTrue();
		assertThat(rule.getLastModified()).isEqualTo("2025-12-20T10:00:00Z");
		assertThat(rule.getModifiedBy()).isNull();
	}

	@Test
	void shouldHandleVacationRuleWithoutMetadata() {
		// Given - Vacation format without metadata (backward compatibility)
		String script = "## Flag: vacation|UniqueId:1|Rulename: Simple Vacation";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		SieveRule rule = result.getRules().get(0);
		assertThat(rule.getRuleNumber()).isEqualTo(1);
		assertThat(rule.getLabel()).isEqualTo("Simple Vacation");

		// Verify no metadata present (backward compatibility)
		assertThat(rule.hasMetadata()).isFalse();
		assertThat(rule.getLastModified()).isNull();
		assertThat(rule.getModifiedBy()).isNull();
	}

	@Test
	void shouldHandleMixedRulesWithAndWithoutMetadata() {
		// Given - Mix of rules with and without metadata
		String script = """
			## Flag: |UniqueId:1|Rulename: Normal Rule
			## Flag: vacation|UniqueId:2|Rulename: Vacation With Metadata|LastModified: 2025-12-18T13:54:27Z|ModifiedBy: 192.168.1.1
			## Flag: syscategory|UniqueId:3|Rulename: System Rule
			## Flag: vacation|UniqueId:4|Rulename: Vacation Without Metadata
			""";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(4);
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("Normal Rule");
		assertThat(result.getRules().get(1).getLabel()).isEqualTo("Vacation With Metadata");
		assertThat(result.getRules().get(2).getLabel()).isEqualTo("System Rule");
		assertThat(result.getRules().get(3).getLabel()).isEqualTo("Vacation Without Metadata");
	}

	@Test
	void shouldHandleVacationRuleWithComplexTimestamp() {
		// Given - Vacation with timezone offset in timestamp
		String script = "## Flag: vacation|UniqueId:5|Rulename: Urlaub|LastModified: 2025-12-25T14:30:00+01:00|ModifiedBy: 10.0.0.1";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		assertThat(result.getRules().get(0).getRuleNumber()).isEqualTo(5);
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("Urlaub");
	}

	@Test
	void shouldHandleVacationRuleWithIPv6Address() {
		// Given - Vacation with IPv6 address in ModifiedBy
		String script = "## Flag: vacation|UniqueId:6|Rulename: Holiday|LastModified: 2025-12-26T08:00:00Z|ModifiedBy: 2001:0db8:85a3:0000:0000:8a2e:0370:7334";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		assertThat(result.getRules().get(0).getRuleNumber()).isEqualTo(6);
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("Holiday");
	}

	@Test
	void shouldHandleVacationRuleWithWhitespaceInRulename() {
		// Given - Rule name with trailing/leading spaces before metadata
		String script = "## Flag: vacation|UniqueId:7|Rulename:  Spaced Rule Name  |LastModified: 2025-12-27T12:00:00Z|ModifiedBy: 172.16.0.1";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		assertThat(result.getRules().get(0).getRuleNumber()).isEqualTo(7);
		// trim() should handle leading/trailing spaces
		assertThat(result.getRules().get(0).getLabel()).isEqualTo("Spaced Rule Name");
	}

	@Test
	void shouldHandleEmptyRulenameWithMetadata() {
		// Given - Empty rule name but with metadata (edge case)
		String script = "## Flag: vacation|UniqueId:8|Rulename:|LastModified: 2025-12-28T00:00:00Z|ModifiedBy: 127.0.0.1";

		// When
		var result = SieveRuleParser.parseRules(script);

		// Then
		assertThat(result.getRules()).hasSize(1);
		assertThat(result.getRules().get(0).getRuleNumber()).isEqualTo(8);
		assertThat(result.getRules().get(0).getLabel()).isEmpty();
		assertThat(result.getRules().get(0).getDisplayText()).isEqualTo("Rule 8");
	}
}
