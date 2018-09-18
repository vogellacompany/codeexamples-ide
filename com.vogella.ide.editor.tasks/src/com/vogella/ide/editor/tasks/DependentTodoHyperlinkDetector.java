package com.vogella.ide.editor.tasks;
import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class DependentTodoHyperlinkDetector extends AbstractHyperlinkDetector {

	private static final String DEPENDENT_PROPERTY = "Dependent:";

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		IDocument document = textViewer.getDocument();

		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
		if (null == activeWorkbenchWindow) {
			activeWorkbenchWindow = workbench.getWorkbenchWindows()[0];
		}
		IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();

		IEditorPart activeEditor = activePage.getActiveEditor();
		if (activeEditor != null) {
			IEditorInput editorInput = activeEditor.getEditorInput();
			IResource adapter = editorInput.getAdapter(IResource.class);
			IContainer parent = adapter.getParent();
			try {
				int offset = region.getOffset();

				IRegion lineInformationOfOffset = document.getLineInformationOfOffset(offset);
				String lineContent = document.get(lineInformationOfOffset.getOffset(),
						lineInformationOfOffset.getLength());

				// Content assist should only be used in the dependent line
				if (lineContent.startsWith(DEPENDENT_PROPERTY)) {
					String dependentResourceName = lineContent.substring(DEPENDENT_PROPERTY.length()).trim();

					Region targetRegion = new Region(lineInformationOfOffset.getOffset() + DEPENDENT_PROPERTY.length(),
							lineInformationOfOffset.getLength() - DEPENDENT_PROPERTY.length());

					IResource[] members = parent.members();

					// Only take resources, which have the "todo" file extension and skip the
					// current resource itself
					return Arrays.asList(members).stream()
							.filter(res -> res instanceof IFile && dependentResourceName.equals(res.getName()))
							.map(res -> new ResourceHyperlink(targetRegion, res.getName(), (IFile) res))
							.toArray(IHyperlink[]::new);
				}
			} catch (CoreException | BadLocationException e) {
				e.printStackTrace();
			}
		}
		// do not return new IHyperlink[0] because the array may only be null or not
		// empty
		return null;
	}

}
