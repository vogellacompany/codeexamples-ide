package com.vogella.tasks.ui.parts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
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
import com.vogella.tasks.model.TaskService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public class TodoDetailsPart {

	private Text txtSummary;
	private Text txtDescription;
	private DateTime dateTime;
	private Button btnDone;

	// define a new field
	private java.util.Optional<Task> task = java.util.Optional.empty();

	// observable placeholder for a task
	private WritableValue<Task> observableTodo = new WritableValue<>();
	private DataBindingContext dbc;

	@Inject
	@Optional
	private MPart part;

	// pause dirty listener when new selection is set
	private boolean pauseDirtyListener;

	@PostConstruct
	public void createControls(Composite parent) {
		parent.setLayout(new GridLayout(2, false));

		GridDataFactory gdFactory = GridDataFactory.fillDefaults();
		LabelFactory labelFactory = LabelFactory.newLabel(SWT.NONE);

		labelFactory.text("Summary").create(parent);

		txtSummary = WidgetFactory.text(SWT.BORDER).layoutData(gdFactory.grab(true, false).create())//
				.create(parent);

		labelFactory.text("Description").create(parent);
		txtDescription = WidgetFactory.text(SWT.BORDER | SWT.MULTI | SWT.V_SCROLL)
				.layoutData(gdFactory.align(SWT.FILL, SWT.FILL).grab(true, true).create()).create(parent);

		labelFactory.text("Due Date").create(parent);

		dateTime = WidgetFactory.dateTime(SWT.BORDER)
				.layoutData(gdFactory.align(SWT.FILL, SWT.CENTER).grab(false, false).create()).create(parent);

		labelFactory.text("").create(parent);

		btnDone = WidgetFactory.button(SWT.CHECK).text("Done").create(parent);

		bindData();
		updateUserInterface(task);
	}

	private void bindData() {
		if (txtSummary != null && !txtSummary.isDisposed()) {

			dbc = new DataBindingContext();

			Map<String, IObservableValue<?>> fields = new HashMap<>();
			fields.put(Task.FIELD_SUMMARY, WidgetProperties.text(SWT.Modify).observe(txtSummary));
			fields.put(Task.FIELD_DESCRIPTION, WidgetProperties.text(SWT.Modify).observe(txtDescription));
			fields.put(Task.FIELD_DUEDATE, WidgetProperties.localDateSelection().observe(dateTime));
			fields.put(Task.FIELD_DONE, WidgetProperties.buttonSelection().observe(btnDone));
			fields.forEach((k, v) -> dbc.bindValue(v, BeanProperties.value(k).observeDetail(observableTodo)));

			// set a dirty state if one of the bindings is changed
			dbc.getBindings().forEach(binding -> binding.getTarget().addChangeListener(e -> {
				if (!pauseDirtyListener && part != null) {
					part.setDirty(true);
				}
			}));
		}
	}

	@Inject
	public void setTasks(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) List<Task> tasks) {
		if (tasks == null || tasks.isEmpty() || tasks.get(0) == null) {
			this.task = java.util.Optional.empty();
		} else {
			this.task = java.util.Optional.of(tasks.get(0));
		}
		// remember the task as field and update the user interface
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
		// the user interface might not yet be created
		if (txtSummary != null && !txtSummary.isDisposed()) {
			pauseDirtyListener = true;
			this.observableTodo.setValue(task.get());
			pauseDirtyListener = false;
		}
	}

	@Persist
	public void save(TaskService taskService) {
		task.ifPresent(taskService::update);
		if (part != null) {
			part.setDirty(false);
		}
	}

	@Focus
	public void setFocus() {
		txtSummary.setFocus();
	}

	@PreDestroy
	public void dispose() {
		if (dbc != null) {
			dbc.dispose();
		}
	}

}