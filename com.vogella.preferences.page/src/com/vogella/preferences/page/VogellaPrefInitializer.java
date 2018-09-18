package com.vogella.preferences.page;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

public class VogellaPrefInitializer extends AbstractPreferenceInitializer {

	public VogellaPrefInitializer() {
	}

	@Override
	public void initializeDefaultPreferences() {
		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.vogella.preferences.page");
		scopedPreferenceStore.setDefault("MySTRING1", "http://www.vogella.com");
	}

}
