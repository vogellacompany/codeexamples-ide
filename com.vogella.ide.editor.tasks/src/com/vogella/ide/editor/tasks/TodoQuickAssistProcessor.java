package com.vogella.ide.editor.tasks;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

public class TodoQuickAssistProcessor implements IQuickAssistProcessor {

    private static final String TODO_PROPERTY = "todoProperty";

    @Override
    public boolean canAssist(IQuickAssistInvocationContext context) {
        // Check if the quick assist can be triggered (e.g., there is a marker)
		IDocument document = context.getSourceViewer().getDocument();
        return document.getLength() > 0;
    }


    @Override
    public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext context) {
        // Get the document and offset where the quick fix is triggered
        int offset = context.getOffset();

        // Get the active editor resource
        IResource resource = getResourceFromEditor(context);
        if (resource == null) {
            return new ICompletionProposal[0];
        }

        try {
            // Check for markers in the document
            List<IMarker> markers = Arrays.asList(resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE));
            Optional<IMarker> todoMarker = markers.stream()
                    .filter(m -> m.getAttribute(TODO_PROPERTY, "").isEmpty())
                    .findFirst();

            if (todoMarker.isPresent()) {
                // Return a quick fix proposal to insert a "TODO property" in the document
                String fixMessage = "Add missing TODO property";
                return new ICompletionProposal[]{
                        new CompletionProposal(fixMessage, offset, 0, fixMessage.length())
                };
            } else {
                // Handle case when there is no marker needing a fix
                return new ICompletionProposal[0];
            }

		} catch (CoreException e) {
            e.printStackTrace();
            return new ICompletionProposal[0];
        }
    }

    @Override
    public String getErrorMessage() {
        return "Quick fix for TODO markers";
    }

	public IResource getResourceFromEditor(IQuickAssistInvocationContext context) {
		// Get the active editor part
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if (editor != null) {
			IEditorInput editorInput = editor.getEditorInput();
			// Get the resource associated with the editor input (for example, a file
			// resource)
			return Adapters.adapt(editorInput, IResource.class);
		}

		return null;
    }

	@Override
	public boolean canFix(Annotation annotation) {
		return true;
//		// Check if the marker can be fixed (based on its attributes)
//		try {
//			String todoProperty = annotation.getAttribute().getType(), "");
//			return todoProperty != null && !todoProperty.isEmpty();
//		} catch (CoreException e) {
//			e.printStackTrace();
//			return false;
//		}
	}
}
