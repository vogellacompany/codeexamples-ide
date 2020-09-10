package com.vogella.ide.editor.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;

public class TaskCodeMiningProvider implements ICodeMiningProvider {

	@Override
	public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer,
			IProgressMonitor monitor) {
		return CompletableFuture.supplyAsync(() -> {
			List<ICodeMining> minings = new ArrayList<>();
			IDocument document = viewer.getDocument();
			try {
				minings.add(new TaskCodeMining(0, document, this));
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
			return minings;
		});
	}

	@Override
	public void dispose() {
	}
}