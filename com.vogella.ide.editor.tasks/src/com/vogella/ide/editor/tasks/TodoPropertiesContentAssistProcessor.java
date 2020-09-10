package com.vogella.ide.editor.tasks;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

public class TodoPropertiesContentAssistProcessor implements IContentAssistProcessor {

    public static final List<String> PROPOSALS = Arrays.asList( "ID:", "Summary:", "Description:", "Done:", "Duedate:", "Dependent:");


	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {

		IDocument document = viewer.getDocument();

		try {
			int lineOfOffset = document.getLineOfOffset(offset);
			int lineOffset = document.getLineOffset(lineOfOffset);

			int lineTextLenght = offset - lineOffset;
			String lineStartToOffsetValue = document.get(lineOffset, lineTextLenght).toLowerCase();

			return PROPOSALS.stream()
					.filter(proposal -> !viewer.getDocument().get().contains(proposal)
							&& proposal.toLowerCase().startsWith(lineStartToOffsetValue))
					.map(proposal -> new CompletionProposal(proposal, lineOffset, lineTextLenght, proposal.length()))
					.toArray(ICompletionProposal[]::new);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		return new ICompletionProposal[0];
	}

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    @Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		String keys = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		return keys.toCharArray();
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		String keys = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		return keys.toCharArray();
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