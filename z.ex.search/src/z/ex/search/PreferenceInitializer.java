package z.ex.search;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Initializes default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		ScopedPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, "z.ex.search");
		store.setDefault(PreferenceConstants.SCRIPT_PATH, PreferenceConstants.DEFAULT_SCRIPT_PATH);
		store.setDefault(PreferenceConstants.PDF_OUTPUT_PATH, PreferenceConstants.DEFAULT_PDF_OUTPUT_PATH);
	}

}
