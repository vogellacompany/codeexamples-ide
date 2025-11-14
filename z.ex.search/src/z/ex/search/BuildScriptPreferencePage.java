package z.ex.search;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Preference page for configuring build script paths.
 */
public class BuildScriptPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public BuildScriptPreferencePage() {
		super(GRID);
	}

	@Override
	public void createFieldEditors() {
		addField(new FileFieldEditor(PreferenceConstants.SCRIPT_PATH,
				"&Build Script Path:",
				getFieldEditorParent()));

		addField(new FileFieldEditor(PreferenceConstants.PDF_OUTPUT_PATH,
				"&PDF Output Path:",
				getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, "z.ex.search"));
		setDescription("Configure paths for the RCP build script and PDF output.");
	}

}
