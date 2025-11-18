package com.vogella.ide.editor.asciidoc.util;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

public class AsciiDocResourceUtil {

    public static IContainer getParentFolder() {
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
