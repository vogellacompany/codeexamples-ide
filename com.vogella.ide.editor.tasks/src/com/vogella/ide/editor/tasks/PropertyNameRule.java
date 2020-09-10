package com.vogella.ide.editor.tasks;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class PropertyNameRule implements IRule {

	private final Token token;

	public PropertyNameRule(Token token) {
		this.token = token;
	}

	@Override
	public IToken evaluate(ICharacterScanner scanner) {
		int c = scanner.read();
		int count = 1;

		while (c != ICharacterScanner.EOF) {

			if (c == ':') {
				return token;
			}

			if ('\n' == c || '\r' == c) {
				break;
			}

			count++;
			c = scanner.read();
		}

		// put the scanner back to the original position if no match
		for (int i = 0; i < count; i++) {
			scanner.unread();
		}

		return Token.UNDEFINED;
	}
}
