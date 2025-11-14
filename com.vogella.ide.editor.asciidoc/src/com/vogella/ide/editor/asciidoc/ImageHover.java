package com.vogella.ide.editor.asciidoc;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import java.io.File;

public class ImageHover implements ITextHover {

    private static final String IMAGE_PREFIX = "image::";
    private static final String IMAGE_DIRECTORY = "img"; // Directory for images

    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        int offset = hoverRegion.getOffset();
        IDocument document = textViewer.getDocument();

        try {
            int lineNumber = document.getLineOfOffset(offset);
            IRegion lineInformation = document.getLineInformation(lineNumber);
            String lineContent = document.get(lineInformation.getOffset(), lineInformation.getLength());

            // Check if the line contains the image syntax
            if (lineContent.contains(IMAGE_PREFIX)) {
                // Extract the image name
                int startIndex = lineContent.indexOf(IMAGE_PREFIX) + IMAGE_PREFIX.length();
                int endIndex = lineContent.indexOf("[]", startIndex);

                if (endIndex > startIndex) {
                    String imageName = lineContent.substring(startIndex, endIndex).trim();


    				IContainer parent = getParentFolder();
    				if (parent == null || !parent.isAccessible()) {
    					return "";
    				}

    				IContainer imgFolder = parent.getFolder(IPath.fromOSString("img"));
    				if (!imgFolder.isAccessible()) {
    					return "Image folder 'img' not found";
    				}


    				IFile imageFile = imgFolder.getFile(IPath.fromOSString(imageName)); // Replace "filename.ext" with your actual file name


                    // Check if image file exists - use isAccessible to avoid rule conflicts
                    if (imageFile.isAccessible()) {
                        // Load and display the image in the hover (assuming HTML rendering is supported)
                        return "<img src=\"" + imageFile.getFullPath()+ "\" alt=\"" + imageName + "\">";
                    } else {
                        return "Image not found in 'img' directory: " + imageName;
                    }
                }
            }

            // If no image syntax, return the full line
            return lineContent;

        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        return "";
    }

    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        return new Region(offset, 0);
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
