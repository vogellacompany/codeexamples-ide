package com.vogella.adapters;

import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import com.vogella.tasks.model.Todo;

public class TodoContentOutlinePage extends ContentOutlinePage {

	private Todo adaptableObject;

	public TodoContentOutlinePage(Todo adaptableObject) {
		this.adaptableObject = adaptableObject;
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		getTreeViewer().setContentProvider(new WorkbenchContentProvider());
		getTreeViewer()
				.setLabelProvider(new DecoratingStyledCellLabelProvider(new WorkbenchLabelProvider(), null, null));
		getTreeViewer().setInput(adaptableObject);


	}
}
