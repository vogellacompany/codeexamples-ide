package com.vogella.tasks.ui.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

public class TestHandler {

	public TestHandler() {
	}
    @Execute
	public void execute(@Optional Shell shell) {
        MessageDialog.openInformation(shell, "First", "Hello, e4 API world");
    }
    
    @CanExecute
	public boolean test() {
		return true;
	}
}