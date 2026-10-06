package com.vogella.preferences.page;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;

public class VogellaPrefInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		DefaultScope.INSTANCE.getNode("com.vogella.preferences.page").put("MySTRING1", "https://www.vogella.com/");
	}

}
