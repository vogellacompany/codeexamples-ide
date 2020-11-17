package com.vogella.ide.editor.gradle;

import org.eclipse.jface.text.rules.IWordDetector;

class Detector implements IWordDetector {
	@Override
	public boolean isWordStart(char c) {
		return Character.isAlphabetic(c);
	}

	@Override
	public boolean isWordPart(char c) {
		return Character.isAlphabetic(c);
	}
}