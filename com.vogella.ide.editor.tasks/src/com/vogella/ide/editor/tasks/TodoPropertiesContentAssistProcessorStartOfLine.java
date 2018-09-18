package com.vogella.ide.editor.tasks;

import java.util.Arrays;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

public class TodoPropertiesContentAssistProcessorStartOfLine implements IContentAssistProcessor {

    public static final String[] PROPOSALS = new String[] { "ID:", "Summary:", "Description:", "Done:", "Duedate:",
            "Dependent:" };

    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {

        IDocument document = viewer.getDocument();

        try {
            int lineOfOffset = document.getLineOfOffset(offset);
            int lineOffset = document.getLineOffset(lineOfOffset);

            // do not show any content assist in case the offset is not at the
            // beginning of a line
            if (offset != lineOffset) {
                return null;
            }
        } catch (BadLocationException e) {
            // ignore here and just continue
        	  return null;
        }

        return Arrays.asList(PROPOSALS).stream().filter(proposal -> !viewer.getDocument().get().contains(proposal))
                .map(proposal -> new CompletionProposal(proposal, offset, 0, proposal.length()))
                .toArray(ICompletionProposal[]::new);
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
//    	String keys = "abcdABCD";
//        return keys.toCharArray();
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