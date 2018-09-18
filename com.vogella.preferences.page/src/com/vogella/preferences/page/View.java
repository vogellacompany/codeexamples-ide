package com.vogella.preferences.page;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/*
 * Using Eclipse 3.x API view as example, do not forget to register it
 * as an  org.eclipse.ui.views extension.
 */
public class View extends ViewPart {

	private Label label;
	
	@Override
	public void createPartControl(Composite parent) {
		IPreferenceStore preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.vogella.preferences.page");
		String string = preferenceStore.getString("MySTRING1");

		label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,
				false));
		label.setText(string);
		// add change listener to the preferences store so that we are notified
		// in case of changes
		preferenceStore
				.addPropertyChangeListener(new IPropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent event) {
						if (event.getProperty() == "MySTRING1") {
							label.setText(event.getNewValue().toString());
						}
					}
				});
	}

	@Override
	public void setFocus() {
		label.setFocus();
	}
}