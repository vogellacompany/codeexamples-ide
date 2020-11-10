package com.vogella.tasks.ui.parts;

import static org.eclipse.jface.layout.GridLayoutFactory.fillDefaults;
import static org.eclipse.jface.widgets.WidgetFactory.button;
import static org.eclipse.jface.widgets.WidgetFactory.label;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

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

//		Combo combo = new Combo(parent, SWT.READ_ONLY);
//		List<Task> tasks = taskService.getAll();
//		int i = 0;
//		for (Task task : tasks) {
//			combo.setItem(i++, task.getSummary());
//		}

		TableViewer viewer = new TableViewer(parent, SWT.READ_ONLY);
		viewer.getTable().setHeaderVisible(true);


		TableViewerColumn c = new TableViewerColumn(viewer, SWT.NONE);
		c.getColumn().setText("Summary");
		c.getColumn().setWidth(200);

		c.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Task t = (Task) element;
				return t.getSummary() + " " + t.getId();
			};
		});

		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setInput(taskService.getAll());
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
