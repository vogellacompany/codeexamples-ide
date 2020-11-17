package com.vogella.preferences.page;

import java.io.IOException;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

public class VogellaPrefInitializer extends AbstractPreferenceInitializer {

	public VogellaPrefInitializer() {
		System.out.println("Called");
	}

	@Override
	public void initializeDefaultPreferences() {
		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.vogella.preferences.page");
		scopedPreferenceStore.setDefault("MySTRING1", "https://www.vogella.com/");
		try {
			scopedPreferenceStore.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
