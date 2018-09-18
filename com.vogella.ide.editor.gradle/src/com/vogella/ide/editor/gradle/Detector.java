package com.vogella.ide.editor.gradle;

import org.eclipse.jface.text.rules.IWordDetector;

class Detector implements IWordDetector {
	public boolean isWordStart(char c) {
		return Character.isAlphabetic(c);
	}

	public boolean isWordPart(char c) {
		return Character.isAlphabetic(c);
	}
}