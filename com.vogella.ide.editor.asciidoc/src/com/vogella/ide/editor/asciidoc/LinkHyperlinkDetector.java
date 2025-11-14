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
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

public class LinkHyperlinkDetector extends AbstractHyperlinkDetector {

    private static final String HYPERLINK_PROPERTY = "link:";

    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {

        IDocument document = textViewer.getDocument();
        IContainer parent = getParentFolder();

        try {
            int offset = region.getOffset();

            IRegion lineInformationOfOffset = document.getLineInformationOfOffset(offset);
            String lineContent = document.get(lineInformationOfOffset.getOffset(), lineInformationOfOffset.getLength());

            if (lineContent.contains(HYPERLINK_PROPERTY)) {
                int start = lineContent.indexOf(HYPERLINK_PROPERTY);
                int end = lineContent.indexOf("[", start);
                if (end == -1) {
                    return null;
                }

                String target = lineContent.substring(start + HYPERLINK_PROPERTY.length(), end).trim();
                Region targetRegion = new Region(lineInformationOfOffset.getOffset() + start,
                        end - start);

                if (target.startsWith("http://") || target.startsWith("https://")) {
                    // External link → open in browser
                    return new IHyperlink[] {
                        new IHyperlink() {
                            @Override
                            public IRegion getHyperlinkRegion() {
                                return targetRegion;
                            }

                            @Override
                            public String getTypeLabel() {
                                return "Open external link";
                            }

                            @Override
                            public String getHyperlinkText() {
                                return "Open " + target + " in browser";
                            }

                            @Override
                            public void open() {
                                Program.launch(target);
                            }
                        }
                    };
                } else if (target.startsWith("./") || target.startsWith("../")) {
                    // Internal file link → open in Eclipse editor
                    // Use isAccessible to avoid rule conflicts during builds
                    if (parent != null && parent.isAccessible()) {
                        IResource resource = parent.findMember(new Path(target));
                        if (resource instanceof IFile file && file.isAccessible()) {
                            return new IHyperlink[] {
                                new ResourceHyperlink(targetRegion, resource.getName(), file)
                            };
                        }
                    }
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        return null;
    }

    private IContainer getParentFolder() {
        IEclipseContext context = PlatformUI.getWorkbench().getService(IEclipseContext.class);
        Object object = context.get("activeEditor");

        if (object instanceof IEditorPart activeEditor) {
            IEditorInput editorInput = activeEditor.getEditorInput();
            IResource adapter = editorInput.getAdapter(IResource.class);
            if (adapter != null) {
                return adapter.getParent();
            }
        }
        return null;
    }
}
