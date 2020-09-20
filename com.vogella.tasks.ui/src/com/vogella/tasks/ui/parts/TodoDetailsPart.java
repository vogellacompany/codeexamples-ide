package com.vogella.tasks.ui.parts;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Text;

import com.vogella.tasks.model.Task;


public class TodoDetailsPart {
	private Text txtSummary;
	private Text txtDescription;
	private DateTime dateTime;
	private Button btnDone;

	// define a new field
	private java.util.Optional<Task> task = java.util.Optional.ofNullable(null);

	@PostConstruct
	public void createControls(Composite parent) {
		parent.setLayout(new GridLayout(2, false));
		
		GridDataFactory gdFactory = GridDataFactory.fillDefaults();
		LabelFactory labelFactory = LabelFactory.newLabel(SWT.NONE);

		labelFactory.text("Summary").create(parent);

		txtSummary = WidgetFactory.text(SWT.BORDER).layoutData(gdFactory.grab(true, false).create())//
				.create(parent);

		// NEW
		txtSummary.addModifyListener(e -> { // #<1>
			if (task.isPresent()) {
				task.get().setSummary(txtSummary.getText());
			}

		});

		labelFactory.text("Description").create(parent);
		txtDescription = WidgetFactory.text(SWT.BORDER | SWT.MULTI | SWT.V_SCROLL)
				.layoutData(gdFactory.align(SWT.FILL, SWT.FILL).grab(true, true).create()).create(parent);

		txtDescription.addModifyListener(e -> { // #<2>
			if (task.isPresent()) {
				task.get().setDescription(txtDescription.getText());
			}
		});

		labelFactory.text("Due Date").create(parent);

		// Factory planned for 2020-12 release
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=567110

		dateTime = new DateTime(parent, SWT.BORDER);
		dateTime.setLayoutData(gdFactory.align(SWT.FILL, SWT.CENTER).grab(false, false).create());

		labelFactory.text("").create(parent);

		btnDone = WidgetFactory.button(SWT.CHECK).text("Done").create(parent);
		updateUserInterface(task); // # <1>
	}

	@Focus
	public void setFocus() {
		txtSummary.setFocus();
	}

	// Add the following new methods to your code

	@Inject
	public void setTasks(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) List<Task> tasks) {
		if (tasks == null || tasks.isEmpty()) {
			this.task = java.util.Optional.empty();
		} else {
			this.task = java.util.Optional.of(tasks.get(0));
		}
		// Remember the task as field update the user interface
		updateUserInterface(this.task);
	}
	// allows to disable/ enable the user interface fields
	// if no task is set
	private void enableUserInterface(boolean enabled) {
		if (txtSummary != null && !txtSummary.isDisposed()) {
			txtSummary.setEnabled(enabled);
			txtDescription.setEnabled(enabled);
			dateTime.setEnabled(enabled);
			btnDone.setEnabled(enabled);
		}
	}

	private void updateUserInterface(java.util.Optional<Task> task) {
		if (!task.isPresent()) {
			enableUserInterface(false);
			return; // nothing left to do
		}
		
		enableUserInterface(true);
		// the following check ensures that the user interface is available,
		// it assumes that you have a text widget called "txtSummary"
		if (txtSummary != null && !txtSummary.isDisposed()) {
			enableUserInterface(true);
			txtSummary.setText(task.get().getSummary());
			txtDescription.setText(task.get().getDescription());
			// more code to fill the widgets with data from your task object
			// more code
			// ....
			// ....
		}
	}
}