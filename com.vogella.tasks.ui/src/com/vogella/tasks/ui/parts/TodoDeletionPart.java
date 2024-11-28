package com.vogella.tasks.ui.parts;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.vogella.tasks.model.Task;
import com.vogella.tasks.model.TaskService;

public class TodoDeletionPart {

	@Inject
	private TaskService taskService;

	private ComboViewer viewer;

	@PostConstruct
	public void createControls(Composite parent) {
		parent.setLayout(new GridLayout(2, false));
		viewer = new ComboViewer(parent, SWT.READ_ONLY);
		viewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				Task todo = (Task) element;
				return todo.getSummary();
			}
		});
		viewer.setContentProvider(ArrayContentProvider.getInstance());

		taskService.consume(this::updateViewer);

		Button button = new Button(parent, SWT.PUSH);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = viewer.getStructuredSelection();
				if (!selection.isEmpty()) {
					Task firstElement = (Task) selection.getFirstElement();
					taskService.delete(firstElement.getId());
					taskService.consume(TodoDeletionPart.this::updateViewer);
				}

			}
		});
		button.setText("Delete selected");
	}

	private void updateViewer(List<Task> todos) {
		viewer.setInput(todos);
		if (!todos.isEmpty()) {
			viewer.setSelection(new StructuredSelection(todos.get(0)));
		}
	}

	@Focus
	public void focus() {
		viewer.getControl().setFocus();
	}
}
