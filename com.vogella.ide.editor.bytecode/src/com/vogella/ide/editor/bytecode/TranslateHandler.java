package com.vogella.ide.editor.bytecode;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

/** Lets Claude translate the bytecode of the active editor into the language given as parameter. */
public class TranslateHandler extends AbstractHandler {

	public static final String COMMAND_ID = "com.vogella.ide.editor.bytecode.translate";
	public static final String LANGUAGE_PARAMETER = "com.vogella.ide.editor.bytecode.language";

	@Override
	public Object execute(ExecutionEvent event) {
		if (!(HandlerUtil.getActiveEditor(event) instanceof ITextEditor editor)) {
			return null;
		}
		String languageName = event.getParameter(LANGUAGE_PARAMETER);
		if (languageName == null) {
			var dialog = new InputDialog(editor.getSite().getShell(), "Translate with Claude", "Target language:",
					"Python", text -> text.isBlank() ? "Enter a programming language" : null);
			if (dialog.open() != Window.OK) {
				return null;
			}
			languageName = dialog.getValue().strip();
		}
		Translator.translate(editor, Language.of(languageName));
		return null;
	}
}
