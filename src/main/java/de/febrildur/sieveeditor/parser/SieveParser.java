// aislop-ignore-file complexity/function-too-long -- Parser-Validierung ist ein State-Machine-Bündel
package de.febrildur.sieveeditor.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.text.Document;
import javax.swing.text.Element;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;

public class SieveParser extends AbstractParser {

	private static final Logger LOGGER = Logger.getLogger(SieveParser.class.getName());

	@Override
	public ParseResult parse(RSyntaxDocument doc, String style) {
		DefaultParseResult result = new DefaultParseResult(this);

		try {
			String text = doc.getText(0, doc.getLength());
			List<ParserNotice> notices = new ArrayList<>();

			notices.addAll(checkBalancedBraces(text));
			notices.addAll(checkBalancedParens(text));
			notices.addAll(checkBalancedBrackets(text));
			notices.addAll(checkStringQuotes(text));

			for (ParserNotice notice : notices) {
				result.addNotice(notice);
			}
			result.setParsedLines(0, doc.getDefaultRootElement().getElementCount() - 1);
		} catch (Exception e) {
			LOGGER.log(java.util.logging.Level.FINE, "SieveParser error", e);
		}

		return result;
	}

	private int lineOf(String text, int offset) {
		int line = 0;
		for (int i = 0; i < offset && i < text.length(); i++) {
			if (text.charAt(i) == '\n') {
				line++;
			}
		}
		return line;
	}

	private List<ParserNotice> checkBalancedBraces(String text) {
		List<ParserNotice> notices = new ArrayList<>();
		List<int[]> stack = new ArrayList<>(); // [offset, line]

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '{') {
				stack.add(new int[]{i, lineOf(text, i)});
			} else if (c == '}') {
				if (stack.isEmpty()) {
					notices.add(new DefaultParserNotice(this,
						"Unmatched closing brace }", lineOf(text, i)));
				} else {
					stack.remove(stack.size() - 1);
				}
			}
		}
		for (int[] brace : stack) {
			notices.add(new DefaultParserNotice(this,
				"Unmatched opening brace { at line " + (brace[1] + 1), brace[1]));
		}
		return notices;
	}

	private List<ParserNotice> checkBalancedParens(String text) {
		List<ParserNotice> notices = new ArrayList<>();
		List<int[]> stack = new ArrayList<>();

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '(') {
				stack.add(new int[]{i, lineOf(text, i)});
			} else if (c == ')') {
				if (stack.isEmpty()) {
					notices.add(new DefaultParserNotice(this,
						"Unmatched closing parenthesis )", lineOf(text, i)));
				} else {
					stack.remove(stack.size() - 1);
				}
			}
		}
		for (int[] paren : stack) {
			notices.add(new DefaultParserNotice(this,
				"Unmatched opening parenthesis ( at line " + (paren[1] + 1), paren[1]));
		}
		return notices;
	}

	private List<ParserNotice> checkBalancedBrackets(String text) {
		List<ParserNotice> notices = new ArrayList<>();
		List<int[]> stack = new ArrayList<>();

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '[') {
				stack.add(new int[]{i, lineOf(text, i)});
			} else if (c == ']') {
				if (stack.isEmpty()) {
					notices.add(new DefaultParserNotice(this,
						"Unmatched closing bracket ]", lineOf(text, i)));
				} else {
					stack.remove(stack.size() - 1);
				}
			}
		}
		for (int[] bracket : stack) {
			notices.add(new DefaultParserNotice(this,
				"Unmatched opening bracket [ at line " + (bracket[1] + 1), bracket[1]));
		}
		return notices;
	}

	private List<ParserNotice> checkStringQuotes(String text) {
		List<ParserNotice> notices = new ArrayList<>();
		boolean inString = false;
		int stringStartOffset = -1;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\\' && inString && i + 1 < text.length()) {
				i++;
				continue;
			}
			if (c == '"') {
				if (inString) {
					inString = false;
				} else {
					inString = true;
					stringStartOffset = i;
				}
			}
		}
		if (inString) {
			notices.add(new DefaultParserNotice(this,
				"Unclosed string literal starting at line " + (lineOf(text, stringStartOffset) + 1),
				lineOf(text, stringStartOffset)));
		}
		return notices;
	}
}
