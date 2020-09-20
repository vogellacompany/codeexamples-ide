package com.vogella.tasks.ui.handlers;

import java.time.LocalDate;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.vogella.tasks.model.Task;
import com.vogella.tasks.model.TaskService;
import com.vogella.tasks.ui.wizards.TaskWizard;

public class NewTaskHandler {
	@Execute
	public void execute(Shell shell, TaskService todoService) {
		// use -1 to indicate a not existing id
		Task task = new Task(-1);
		task.setDueDate(LocalDate.now());
		WizardDialog dialog = new WizardDialog(shell, new TaskWizard(task));
		if (dialog.open() == Window.OK) {
			// call service to save task object
			todoService.update(task);
		}

	}
}
