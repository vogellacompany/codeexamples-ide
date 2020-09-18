package com.vogella.tasks.ui.parts;

import static org.eclipse.jface.layout.GridLayoutFactory.fillDefaults;
import static org.eclipse.jface.widgets.ButtonFactory.newButton;
import static org.eclipse.jface.widgets.LabelFactory.newLabel;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.vogella.tasks.model.TaskService;

public class TodoOverviewPart {
	@Inject
	TaskService taskService;

	private Label label;

	@PostConstruct
	public void createControls(Composite parent) {
		fillDefaults().numColumns(2).applyTo(parent);
		newButton(SWT.PUSH).text("Load Data").onSelect(e -> updateLabel())
				.create(parent);
		label = newLabel(SWT.NONE).text("Number of tasks: ")
				.layoutData(GridDataFactory.fillDefaults().grab(true, false).create()).create(parent);

	}

	private void updateLabel() {
		label.setText("Number of tasks: " + taskService.getAll().size());

	}
}
