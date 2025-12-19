package de.febrildur.sieveeditor.util;

import org.junit.jupiter.api.Test;
import de.febrildur.sieveeditor.util.SieveErrorParser.ErrorInfo;

import static org.assertj.core.api.Assertions.*;

class SieveErrorParserTest {

	@Test
	void shouldExtractLineNumberFromDovecotFormat() {
		// Given
		String error = "line 5: syntax error";

		// When
		ErrorInfo info = SieveErrorParser.parseError(error);

		// Then
		assertThat(info.hasLineNumber()).isTrue();
		assertThat(info.getLineNumber()).contains(5);
		assertThat(info.getMessage()).isEqualTo(error);
	}

	@Test
	void shouldExtractLineNumberFromOnLineFormat() {
		// Given
		String error = "syntax error on line 42";

		// When
		ErrorInfo info = SieveErrorParser.parseError(error);

		// Then
		assertThat(info.hasLineNumber()).isTrue();
		assertThat(info.getLineNumber()).contains(42);
	}

	@Test
	void shouldExtractLineNumberFromAtLineFormat() {
		// Given
		String error = "unexpected token at line 10";

		// When
		ErrorInfo info = SieveErrorParser.parseError(error);

		// Then
		assertThat(info.hasLineNumber()).isTrue();
		assertThat(info.getLineNumber()).contains(10);
	}

	@Test
	void shouldExtractLineNumberFromParenthesesFormat() {
		// Given
		String error = "error (line 7): invalid command";

		// When
		ErrorInfo info = SieveErrorParser.parseError(error);

		// Then
		assertThat(info.hasLineNumber()).isTrue();
		assertThat(info.getLineNumber()).contains(7);
	}

	@Test
	void shouldExtractLineNumberFromBracketsFormat() {
		// Given
		String error = "error [line 15]: missing semicolon";

		// When
		ErrorInfo info = SieveErrorParser.parseError(error);

		// Then
		assertThat(info.hasLineNumber()).isTrue();
		assertThat(info.getLineNumber()).contains(15);
	}

	@Test
	void shouldExtractLineNumberFromCompilerFormat() {
		// Given
		String error = "42: syntax error";

		// When
		ErrorInfo info = SieveErrorParser.parseError(error);

		// Then
		assertThat(info.hasLineNumber()).isTrue();
		assertThat(info.getLineNumber()).contains(42);
	}

	@Test
	void shouldExtractLineNumberFromColonFormat() {
		// Given
		String error = "script.sieve:25: unexpected EOF";

		// When
		ErrorInfo info = SieveErrorParser.parseError(error);

		// Then
		assertThat(info.hasLineNumber()).isTrue();
		assertThat(info.getLineNumber()).contains(25);
	}

	@Test
	void shouldHandleMessageWithoutLineNumber() {
		// Given
		String error = "general syntax error";

		// When
		ErrorInfo info = SieveErrorParser.parseError(error);

		// Then
		assertThat(info.hasLineNumber()).isFalse();
		assertThat(info.getLineNumber()).isEmpty();
		assertThat(info.getMessage()).isEqualTo(error);
	}

	@Test
	void shouldHandleNullMessage() {
		// When
		ErrorInfo info = SieveErrorParser.parseError(null);

		// Then
		assertThat(info.hasLineNumber()).isFalse();
		assertThat(info.getMessage()).isEqualTo("Unknown error");
	}

	@Test
	void shouldHandleEmptyMessage() {
		// When
		ErrorInfo info = SieveErrorParser.parseError("");

		// Then
		assertThat(info.hasLineNumber()).isFalse();
		assertThat(info.getMessage()).isEqualTo("Unknown error");
	}

	@Test
	void shouldExtractFirstLineNumberWhenMultiplePresent() {
		// Given
		String error = "line 5: error references line 10";

		// When
		ErrorInfo info = SieveErrorParser.parseError(error);

		// Then
		assertThat(info.hasLineNumber()).isTrue();
		assertThat(info.getLineNumber()).contains(5);
	}

	@Test
	void shouldIgnoreZeroLineNumber() {
		// Given
		String error = "line 0: invalid";

		// When
		ErrorInfo info = SieveErrorParser.parseError(error);

		// Then
		assertThat(info.hasLineNumber()).isFalse();
	}

	@Test
	void shouldIgnoreNegativeLineNumber() {
		// Given - unlikely but defensive check
		String error = "line -5: invalid";

		// When
		ErrorInfo info = SieveErrorParser.parseError(error);

		// Then
		assertThat(info.hasLineNumber()).isFalse();
	}

	@Test
	void shouldWorkWithCaseInsensitivePattern() {
		// Given
		String error = "LINE 42: error";

		// When
		ErrorInfo info = SieveErrorParser.parseError(error);

		// Then
		assertThat(info.hasLineNumber()).isTrue();
		assertThat(info.getLineNumber()).contains(42);
	}

	@Test
	void shouldUseExtractLineNumberConvenienceMethod() {
		// Given
		String error = "line 99: test error";

		// When
		var lineNumber = SieveErrorParser.extractLineNumber(error);

		// Then
		assertThat(lineNumber).contains(99);
	}

	@Test
	void shouldReturnEmptyOptionalWhenNoLineNumberFound() {
		// Given
		String error = "generic error";

		// When
		var lineNumber = SieveErrorParser.extractLineNumber(error);

		// Then
		assertThat(lineNumber).isEmpty();
	}
}
