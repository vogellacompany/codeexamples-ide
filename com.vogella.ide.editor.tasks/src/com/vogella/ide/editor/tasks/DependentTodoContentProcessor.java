package com.vogella.ide.editor.tasks;
import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class DependentTodoContentProcessor implements IContentAssistProcessor {

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IDocument document = viewer.getDocument();
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
				int lineOfOffset = document.getLineOfOffset(offset);
				int lineOffset = document.getLineOffset(lineOfOffset);

				String lineProperty = document.get(lineOffset, offset - lineOffset);
				// Content assist should only be used in the dependent line
				if (lineProperty.contains("Dependent:")) {
					IResource[] members = parent.members();
					
					// Only take resources, which have the "todo" file extension and skip the current resource itself
					return Arrays.asList(members).stream().filter(
							res -> !adapter.getName().equals(res.getName()) && "tasks".equals(res.getFileExtension()))
							.map(res -> new CompletionProposal(res.getName(), offset, 0, res.getName().length()))
							.toArray(ICompletionProposal[]::new);
				}
			} catch (CoreException | BadLocationException e) {
				// ignore here and just continue
			}
		}
		return new ICompletionProposal[0];
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

}
