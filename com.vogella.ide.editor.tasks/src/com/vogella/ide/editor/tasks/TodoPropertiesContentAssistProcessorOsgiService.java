package com.vogella.ide.editor.tasks;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;

import com.vogella.tasks.model.ITodoService;
import com.vogella.tasks.model.Todo;

public class TodoPropertiesContentAssistProcessorOsgiService implements IContentAssistProcessor {

    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
    	ITodoService service = PlatformUI.getWorkbench().getService(ITodoService.class);
    	List<Todo> list = new ArrayList<Todo>();
    	service.getTodos(list::addAll);
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
        
        List<String> collect = list.stream().map(e->e.getSummary()).collect(Collectors.toList());    	

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