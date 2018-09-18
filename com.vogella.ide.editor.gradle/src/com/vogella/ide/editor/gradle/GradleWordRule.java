package com.vogella.ide.editor.gradle;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class GradleWordRule extends WordRule {

	public GradleWordRule() {
		super(new Detector());
		Color blueColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
		addWord("allprojects", new Token(new TextAttribute(blueColor)));
		addWord("apply", new Token(new TextAttribute(blueColor)));
	}


}
