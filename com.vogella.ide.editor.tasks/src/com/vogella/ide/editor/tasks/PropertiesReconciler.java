package com.vogella.ide.editor.tasks;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

public class PropertiesReconciler extends PresentationReconciler {

	private ColorRegistry colorRegistry;
	private RuleBasedScanner scanner;
	private IRule rule;
	private IPreferenceChangeListener listener;

	@Override
	public void install(ITextViewer viewer) {
		super.install(viewer);

		listener = event -> {
			updateRule();
			viewer.invalidateTextPresentation();
		};
		getWorkbenchPreferences().addPreferenceChangeListener(listener);
	}

	@Override
	public void uninstall() {
		if (listener != null) {
			getWorkbenchPreferences().removePreferenceChangeListener(listener);
			listener = null;
		}
		super.uninstall();
	}

	private IEclipsePreferences getWorkbenchPreferences() {
		return InstanceScope.INSTANCE.getNode("org.eclipse.ui.workbench");
	}

	private void updateRule() {
		Color color = colorRegistry.get("com.vogella.ide.editor.tasks.key");
		TextAttribute tagAttribute = new TextAttribute(color);
		rule = new PropertyNameRule(new Token(tagAttribute));
		scanner.setRules(new IRule[] { rule });

	}

    public PropertiesReconciler() {

    	IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
    	ITheme currentTheme = themeManager.getCurrentTheme();
    	colorRegistry = currentTheme.getColorRegistry();

		scanner = new RuleBasedScanner();
		updateRule();

        DefaultDamagerRepairer dr = new DefaultDamagerRepairer(scanner);
        this.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        this.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
    }
}