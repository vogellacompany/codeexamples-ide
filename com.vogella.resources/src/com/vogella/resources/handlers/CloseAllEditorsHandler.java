package com.vogella.resources.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

public class CloseAllEditorsHandler {

	@Execute
	public void execute(IWorkbenchPage page) {
		List<IEditorReference> editorsToClose = new ArrayList<>();
		for (IEditorReference editorReference : page.getEditorReferences()) {
			try {
				// only close editors which show a file from the workspace
				if (editorReference.getEditorInput().getAdapter(IFile.class) != null) {
					editorsToClose.add(editorReference);
				}
			} catch (PartInitException e) {
				// the editor input cannot be restored, so the editor does not show a workspace file
			}
		}
		page.closeEditors(editorsToClose.toArray(IEditorReference[]::new), true);
	}

}
