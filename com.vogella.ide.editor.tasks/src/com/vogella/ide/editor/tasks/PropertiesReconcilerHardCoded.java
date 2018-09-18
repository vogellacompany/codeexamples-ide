package com.vogella.ide.editor.tasks;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

public class PropertiesReconcilerHardCoded extends PresentationReconciler {

    public PropertiesReconcilerHardCoded() {
    	IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
    	ITheme currentTheme = themeManager.getCurrentTheme();
    	ColorRegistry colorRegistry = currentTheme.getColorRegistry();
    	Color color = colorRegistry.get("com.vogella.ide.editor.tasks.key");
        RuleBasedScanner scanner = new RuleBasedScanner();
        IRule rule = new PropertyNameRule(new Token(new TextAttribute(color)));
        scanner.setRules(new IRule[] { rule });
        DefaultDamagerRepairer dr = new DefaultDamagerRepairer(scanner);
        this.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        this.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
    }

}