package com.vogella.tasks.ui.parts;

import static org.eclipse.jface.layout.GridLayoutFactory.fillDefaults;
import static org.eclipse.jface.widgets.ButtonFactory.newButton;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.vogella.tasks.model.Task;
import com.vogella.tasks.model.TaskService;

public class TodoOverviewPart {
	@Inject
	TaskService taskService;


	private TableViewer viewer;

	@PostConstruct
	public void createControls(Composite parent) {
		fillDefaults().numColumns(1).applyTo(parent);

		newButton(SWT.PUSH).text("Load Data").onSelect(e -> update()).create(parent);

		viewer = new TableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION);
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setContentProvider(ArrayContentProvider.getInstance());

		// create column for the summary property
		TableViewerColumn colSummary = new TableViewerColumn(viewer, SWT.NONE);
		colSummary.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Task todo = (Task) element;
				return todo.getSummary();
			}
		});
		colSummary.getColumn().setWidth(100);
		colSummary.getColumn().setText("Summary");

		// create column for description property
		TableViewerColumn colDescription = new TableViewerColumn(viewer, SWT.NONE);
		colDescription.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Task todo = (Task) element;
				return todo.getDescription();
			}
		});
		colDescription.getColumn().setWidth(200);
		colDescription.getColumn().setText("Description");

		// initially the table is also filled
		// the btnLoadData is used to update the data if the model changes
		taskService.consume(viewer::setInput);
	}

	@Focus
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	private void update() {
		taskService.consume(viewer::setInput);
	}
}
