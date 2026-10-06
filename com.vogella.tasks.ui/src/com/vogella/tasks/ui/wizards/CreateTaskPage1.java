package com.vogella.tasks.ui.wizards;

import java.util.Collections;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.vogella.tasks.model.Task;
import com.vogella.tasks.ui.parts.TodoDetailsPart;

public class CreateTaskPage1 extends WizardPage {

	private Task task;

	public CreateTaskPage1(Task todo) {
		super("page1");
		this.task = todo;
		setTitle("Creates a new task");
		setDescription("Enter the data");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		// created with new, so no dependency injection takes place
		TodoDetailsPart part = new TodoDetailsPart();
		part.createControls(container);
		part.setTasks(Collections.singletonList(task));
		setPageComplete(true);
		setControl(container);
	}

}