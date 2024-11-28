package com.vogella.ide.editor.tasks;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

public class SimpleContentAssists implements IContentAssistProcessor {


	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		String[] suggestions = { "public", "private", "protected", "class" };
		List<ICompletionProposal> proposals = new ArrayList<>();
		for (String suggestion : suggestions) {
			proposals.add(new CompletionProposal(suggestion, offset, 0, suggestion.length()));
		}
		return proposals.toArray(new ICompletionProposal[0]);
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
