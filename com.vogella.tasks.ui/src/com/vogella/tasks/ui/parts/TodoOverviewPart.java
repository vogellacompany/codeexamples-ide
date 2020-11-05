package com.vogella.tasks.ui.parts;

import static org.eclipse.jface.layout.GridLayoutFactory.fillDefaults;
import static org.eclipse.jface.widgets.WidgetFactory.button;
import static org.eclipse.jface.widgets.WidgetFactory.label;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.vogella.tasks.events.TaskEventConstants;
import com.vogella.tasks.model.Task;
import com.vogella.tasks.model.TaskService;

public class TodoOverviewPart {

	@Inject
	TaskService taskService;
	@Inject
	ESelectionService service;
	private WritableList<Task> writableList;

	private TableViewer viewer;

	@PostConstruct
	public void createControls(Composite parent, EMenuService menuService) {
		fillDefaults().numColumns(1).applyTo(parent);

		button(SWT.PUSH).text("Load Data").onSelect(e -> update()).create(parent);
		label(SWT.NONE).text("Number of tasks: ")
        .layoutData(GridDataFactory.fillDefaults().grab(true, false).create()).create(parent);
		
		
		viewer = new TableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION);
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// create column for the summary property
		TableViewerColumn colSummary = new TableViewerColumn(viewer, SWT.NONE);
		colSummary.getColumn().setWidth(100);
		colSummary.getColumn().setText("Summary");

		// create column for description property
		TableViewerColumn colDescription = new TableViewerColumn(viewer, SWT.NONE);

		colDescription.getColumn().setWidth(200);
		colDescription.getColumn().setText("Description");

		// after the viewer is instantiated
		viewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = viewer.getStructuredSelection();
			service.setSelection(selection.toList());
		});

		menuService.registerContextMenu(viewer.getControl(), "com.vogella.tasks.ui.popupmenu.table");

		// use data binding to bind the viewer
		writableList = new WritableList<>();
		// fill the writable list, when Consumer callback is called. Databinding
		// will do the rest once the list is filled
		taskService.consume(writableList::addAll);
		ViewerSupport.bind(viewer, writableList, BeanProperties.values(Task.FIELD_SUMMARY, Task.FIELD_DESCRIPTION));

	}

	public void updateViewer(List<Task> list) {
		if (viewer != null) {
			writableList.clear();
			writableList.addAll(list);
		}
	}

	@Focus
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	private void update() {
		updateViewer(taskService.getAll());
	}

	@Inject
	@Optional
	private void subscribeTopicTaskAllTopics(
			@UIEventTopic(TaskEventConstants.TOPIC_TASKS_ALLTOPICS) Map<String, String> event) {
		if (viewer != null) {
			writableList.clear();
			updateViewer(taskService.getAll());
		}
	}

}
