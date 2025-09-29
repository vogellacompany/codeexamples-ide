package com.vogella.ide.editor.asciidoc;

import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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

				// Check if the dependent resource starts with "./" or "../"
				if (dependentResourceName.startsWith("../") || dependentResourceName.startsWith("./")) {
					
					if (containsSubfolder(dependentResourceName)) {
						// Handle cases with subdirectories
						int lastSlashIndex = dependentResourceName.lastIndexOf("/");
						String folder = dependentResourceName.substring(0, lastSlashIndex);
						dependentResourceName = dependentResourceName.substring(lastSlashIndex + 1);
						IContainer subfolder = parent.getFolder(new Path(folder));
						parent = subfolder;
					} else {
						// Handle cases like "../exercise_settings.adoc" without a folder structure
						dependentResourceName = dependentResourceName.substring(3); // Remove "../"
					}
				} else if (dependentResourceName.contains("/") || dependentResourceName.contains("\\")) {
					// Handle simple relative paths like "res/practical/asciidoc_validator.py"
					int lastSlashIndex = Math.max(dependentResourceName.lastIndexOf("/"), 
												  dependentResourceName.lastIndexOf("\\"));
					String folder = dependentResourceName.substring(0, lastSlashIndex);
					dependentResourceName = dependentResourceName.substring(lastSlashIndex + 1);
					IContainer subfolder = parent.getFolder(new Path(folder));
					parent = subfolder;
				}

				if (!parent.exists()) {
					return null;
				}

				fileName = dependentResourceName;
				IResource[] members = parent.members();
				// Only take resources with the "adoc" file extension and skip the current
				// resource itself
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

	public static boolean containsSubfolder(String relativePath) {
		String normalizedPath = relativePath.replace("\\", "/"); // Normalize for cross-platform
		int lastIndexOfParent = normalizedPath.lastIndexOf("../");
		return lastIndexOfParent != -1 && normalizedPath.indexOf("/", lastIndexOfParent + 3) != -1;
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