package com.vogella.ide.editor.asciidoc.util;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

public class AsciiDocResourceUtil {

    public static IContainer getParentFolder() {
        // Use AtomicReference to hold the result from the UI thread
        final AtomicReference<IContainer> containerRef = new AtomicReference<>();

        // Ensure UI-related code runs on the UI thread
        Display.getDefault().syncExec(() -> {
            IEclipseContext context = PlatformUI.getWorkbench().getService(IEclipseContext.class);
            Object object = context.get("activeEditor");

            if (object instanceof IEditorPart activeEditor) {
                IEditorInput editorInput = activeEditor.getEditorInput();
                IResource adapter = editorInput.getAdapter(IResource.class);
                if (adapter != null) {
                    containerRef.set(adapter.getParent());
                }
            }
        });

        return containerRef.get();
    }
}
