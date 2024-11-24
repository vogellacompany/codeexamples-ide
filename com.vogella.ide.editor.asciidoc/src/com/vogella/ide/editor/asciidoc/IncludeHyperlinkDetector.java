package com.vogella.ide.editor.asciidoc;

import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
	private static final String IMAGE_PROPERTY = "image::";

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		
		IDocument document = textViewer.getDocument();
		IEclipseContext context = PlatformUI.getWorkbench().getService(IEclipseContext.class);
		Object object = context.get("activeEditor");
		
		if (object instanceof IEditorPart activeEditor) {
			
			IEditorInput editorInput = activeEditor.getEditorInput();
			IResource adapter = editorInput.getAdapter(IResource.class);
			IContainer parent = adapter.getParent();
			
			try {
				int offset = region.getOffset();

				IRegion lineInformationOfOffset = document.getLineInformationOfOffset(offset);
				String lineContent = document.get(lineInformationOfOffset.getOffset(),
						lineInformationOfOffset.getLength());

				// Content assist should only be used in the dependent line
				if (lineContent.startsWith(HYPERLINK_PROPERTY)) {
					String dependentResourceName = lineContent
							.substring(HYPERLINK_PROPERTY.length(), lineContent.indexOf("[")).trim();

					Region targetRegion = new Region(lineInformationOfOffset.getOffset() + HYPERLINK_PROPERTY.length(),
							lineInformationOfOffset.getLength() - HYPERLINK_PROPERTY.length());

					IResource[] members = parent.members();

					// Only take resources, which have the "adoc" file extension and skip the
					// current resource itself
					return Arrays.stream(members)
							.filter(res -> res instanceof IFile && res.getName().equals(dependentResourceName)
									&& res.getFileExtension().equals("adoc"))
							.map(res -> new ResourceHyperlink(targetRegion, res.getName(), (IFile) res))
							.toArray(IHyperlink[]::new);
				}
				// Content assist should only be used in the dependent line
				if (lineContent.startsWith(IMAGE_PROPERTY)) {
					String dependentResourceName = lineContent
							.substring(IMAGE_PROPERTY.length(), lineContent.indexOf("[")).trim();

					Region targetRegion = new Region(lineInformationOfOffset.getOffset() + IMAGE_PROPERTY.length(),
							lineInformationOfOffset.getLength() - IMAGE_PROPERTY.length());

					IContainer imgFolder = parent.getFolder(IPath.fromOSString("img"));


					// Only take resources, which have the "adoc" file extension and skip the
					// current resource itself
					return Arrays.stream(imgFolder.members())
							.filter(res -> res instanceof IFile && res.getName().equals(dependentResourceName)
									&& res.getFileExtension().equals("png"))
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