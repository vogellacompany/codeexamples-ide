package com.vogella.ide.editor.gradle;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class GradlePresentationReconciler extends PresentationReconciler implements IPresentationReconciler {

	private IToken quoteToken = new Token(new TextAttribute(new Color(Display.getCurrent(), new RGB(139, 69, 19))));
	private IToken numberToken = new Token(new TextAttribute(new Color(Display.getCurrent(), new RGB(0, 0, 255))));
	private IToken commentToken = new Token(new TextAttribute(new Color(Display.getCurrent(), new RGB(0, 100, 0))));

	public GradlePresentationReconciler() {
		var scanner = new RuleBasedScanner();

		var rules = new IRule[1];

		rules[0] = new GradleKeywordRule();


		scanner.setRules(rules);

		var ddr = new DefaultDamagerRepairer(scanner);
		this.setDamager(ddr, IDocument.DEFAULT_CONTENT_TYPE);
		this.setRepairer(ddr, IDocument.DEFAULT_CONTENT_TYPE);
	}

}
