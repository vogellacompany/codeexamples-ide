package com.vogella.ide.editor.asciidoc;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

import com.vogella.ide.editor.asciidoc.util.AsciiDocResourceUtil;

public class ImageHover implements ITextHover {

    private static final String IMAGE_PREFIX = "image::";

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


    				IContainer parent = AsciiDocResourceUtil.getParentFolder();
    				if (parent == null || !parent.isAccessible()) {
    					return "";
    				}

    				IContainer imgFolder = parent.getFolder(IPath.fromOSString(AsciiDocConstants.IMG_DIRECTORY));
    				if (!imgFolder.isAccessible()) {
    					return "Image folder '" + AsciiDocConstants.IMG_DIRECTORY + "' not found";
    				}


    				IFile imageFile = imgFolder.getFile(IPath.fromOSString(imageName));


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
}
