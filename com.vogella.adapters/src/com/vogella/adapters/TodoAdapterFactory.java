package com.vogella.adapters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter2;
import org.eclipse.ui.model.IWorkbenchAdapter3;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.views.properties.IPropertySource;

import com.vogella.tasks.model.Task;

public class TodoAdapterFactory implements IAdapterFactory {

	// use a static final field so that the adapterList is only instantiated once
	private static final Class<?>[] adapterList = new Class<?>[] { IPropertySource.class, IWorkbenchAdapter.class,
			IWorkbenchAdapter2.class, IWorkbenchAdapter3.class };

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (!(adaptableObject instanceof Task task)) {
			return null;
		}
		if (adapterType == IPropertySource.class) {
			return adapterType.cast(new TodoPropertySource(task));
		}
		if (adapterType.isAssignableFrom(WorkbenchAdapter.class)) {
			return adapterType.cast(new TodoWorkbenchAdapter());
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return adapterList;
	}

}
