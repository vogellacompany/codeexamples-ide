package com.vogella.ide.editor.tasks;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;
import org.eclipse.swt.events.MouseEvent;

public class TaskCodeMining extends LineHeaderCodeMining {


	private ITextViewer viewer;

	public TaskCodeMining(int beforeLineNumber, IDocument document, ICodeMiningProvider provider, boolean isValid)
			throws BadLocationException {
		super(beforeLineNumber, document, provider);
	}

	@Override
	protected CompletableFuture<Void> doResolve(ITextViewer viewer, IProgressMonitor monitor) {
		this.viewer = viewer;
		return CompletableFuture.runAsync(() -> {
			super.setLabel("This is additional information about the tasks");
		});
	}
	@Override
	public Consumer<MouseEvent> getAction() {
		return r->showMessageDialog();
	}

	private void showMessageDialog() {
		MessageDialog.openInformation(viewer.getTextWidget().getShell(), "Clicked", "You clidcked on the code mining annotation");
	}
}
