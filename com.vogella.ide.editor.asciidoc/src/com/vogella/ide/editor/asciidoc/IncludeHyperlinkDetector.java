package com.vogella.ide.editor.asciidoc;

import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.core.contexts.IEclipseContext;
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

public class IncludeHyperlinkDetector extends AbstractHyperlinkDetector {

	private static final String HYPERLINK_PROPERTY = "include::";

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {

		IDocument document = textViewer.getDocument();
		IContainer parent = getParentFolder();

		try {
			int offset = region.getOffset();

			IRegion lineInformationOfOffset = document.getLineInformationOfOffset(offset);
			String lineContent = document.get(lineInformationOfOffset.getOffset(), lineInformationOfOffset.getLength());

			if (lineContent.startsWith(HYPERLINK_PROPERTY)) {
				String dependentResourceName = lineContent
						.substring(HYPERLINK_PROPERTY.length(), lineContent.indexOf("[")).trim();

				Region targetRegion = new Region(lineInformationOfOffset.getOffset() + HYPERLINK_PROPERTY.length(),
						lineInformationOfOffset.getLength() - HYPERLINK_PROPERTY.length());

				String fileName;
//				// we support subfolder in the same level as we are
				if (dependentResourceName.startsWith("../")||dependentResourceName.startsWith("./") ) {

					String folder = dependentResourceName.substring(0, dependentResourceName.lastIndexOf("/"));
					dependentResourceName = dependentResourceName.substring(dependentResourceName.lastIndexOf("/")+1, dependentResourceName.length());
					IContainer subfolder = parent.getFolder(new Path(folder));
					parent = subfolder;

				}
				if (!parent.exists()) {
					return null;
				}
				
				fileName= dependentResourceName;
				IResource[] members = parent.members();
				// Only take resources, which have the "adoc" file extension and skip the
				// current resource itself
				IHyperlink[] result = Arrays.stream(members)
						.filter(res -> res instanceof IFile && res.getName().equals(fileName))
						.map(res -> new ResourceHyperlink(targetRegion, res.getName(), (IFile) res))
						.toArray(IHyperlink[]::new);

				return result.length == 0 ? null : result;
			}

		} catch (CoreException | BadLocationException e) {
			e.printStackTrace();
		}
		// do not return new IHyperlink[0] because the array may only be null or not
		// empty
		return null;
	}

	private IContainer getParentFolder() {
		IEclipseContext context = PlatformUI.getWorkbench().getService(IEclipseContext.class);
		Object object = context.get("activeEditor");

		if (object instanceof IEditorPart activeEditor) {

			IEditorInput editorInput = activeEditor.getEditorInput();
			IResource adapter = editorInput.getAdapter(IResource.class);
			IContainer parent = adapter.getParent();
			return parent;
		}
		return null;
	}

}