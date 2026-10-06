package com.vogella.ide.editor.bytecode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.texteditor.ITextEditor;

/** Shows the target languages as a clickable header above the bytecode. */
public class TranslateCodeMiningProvider extends AbstractCodeMiningProvider {

	// short enough to fit an editor that shares the window with the translation
	private static final List<String> HEADER_LANGUAGES = List.of("Java", "Python", "JavaScript", "Dart", "Rust",
			"Kotlin", "Go");

	@Override
	public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer,
			IProgressMonitor monitor) {
		ITextEditor editor = getAdapter(ITextEditor.class);
		IFile file = editor == null ? null : Adapters.adapt(editor.getEditorInput(), IFile.class);
		IDocument document = viewer.getDocument();
		if (file == null || document.getNumberOfLines() < 2) {
			return CompletableFuture.completedFuture(List.of());
		}
		List<ICodeMining> minings = new ArrayList<>();
		try {
			minings.add(new Mining(document, "Translate with Claude", null));
			for (String name : HEADER_LANGUAGES) {
				Language language = Language.of(name);
				String label = Translator.isRunning(file, language) ? language.name() + " (translating...)"
						: language.name();
				minings.add(new Mining(document, label, _ -> Translator.translate(editor, language)));
			}
			minings.add(new Mining(document, "Other...", _ -> askForLanguage(editor)));
		} catch (BadLocationException e) {
			return CompletableFuture.completedFuture(List.of());
		}
		return CompletableFuture.completedFuture(minings);
	}

	private static void askForLanguage(ITextEditor editor) {
		var dialog = new InputDialog(editor.getSite().getShell(), "Translate with Claude", "Target language:", "Python",
				text -> text.isBlank() ? "Enter a programming language" : null);
		if (dialog.open() == Window.OK) {
			Translator.translate(editor, Language.of(dialog.getValue().strip()));
		}
	}

	private class Mining extends LineHeaderCodeMining {

		Mining(IDocument document, String label, Consumer<MouseEvent> action)
				throws BadLocationException {
			// a header above the first line is scrolled out of view, the second line holds the class declaration
			super(1, document, TranslateCodeMiningProvider.this, action);
			setLabel(label);
		}
	}
}
