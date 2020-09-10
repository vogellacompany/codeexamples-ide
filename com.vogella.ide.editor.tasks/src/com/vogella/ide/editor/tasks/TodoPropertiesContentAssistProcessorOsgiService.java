package com.vogella.ide.editor.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.PlatformUI;

import com.vogella.tasks.model.Task;
import com.vogella.tasks.model.TaskService;

public class TodoPropertiesContentAssistProcessorOsgiService implements IContentAssistProcessor {

    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
    	TaskService service = PlatformUI.getWorkbench().getService(TaskService.class);
    	List<Task> list = service.getAll();
    	IDocument document = viewer.getDocument();
    	String summary= "Summary:";
        try {
        	if (summary.length()>offset) {
        		return null;
        	}
            String currentText= document.get(offset-summary.length(), summary.length());
            if (!currentText.equals(summary)) {
            	return null;
            }
        } catch (BadLocationException e) {
            // ignore here and just continue
        	  return null;
        }
        
        List<String> collect = list.stream().map(Task::getSummary).collect(Collectors.toList());    	

        return collect.stream().filter(proposal -> !viewer.getDocument().get().contains(proposal))
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