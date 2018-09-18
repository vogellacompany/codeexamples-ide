package com.vogella.ide.editor.gradle;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.PatternRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class GradlePresentationReconciler extends PresentationReconciler {

	 public GradlePresentationReconciler() {
		 Color redColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
		 Color commentColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
	        RuleBasedScanner scanner = new RuleBasedScanner();
	        
	        SingleLineRule quote = new SingleLineRule("'", "'", new Token(new TextAttribute(redColor)));
	        SingleLineRule quote2 = new SingleLineRule("\"", "\"", new Token(new TextAttribute(redColor)));
	        GradleWordRule wordRule = new GradleWordRule();
	        PatternRule patternRule = new PatternRule("//", null, new Token(new TextAttribute(commentColor)), (char)0, true);
	        scanner.setRules(new IRule[] { quote, quote2, wordRule, patternRule});
	        DefaultDamagerRepairer dr = new DefaultDamagerRepairer(scanner);
	        this.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
	        this.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
	    }

}
