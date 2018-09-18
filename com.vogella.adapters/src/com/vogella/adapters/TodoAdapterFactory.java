package com.vogella.adapters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySource;

import com.vogella.tasks.model.Todo;

public class TodoAdapterFactory implements IAdapterFactory {

	// use a static final field so that the adapterList is only instantiated once
	private static final Class<?>[] adapterList = new Class<?>[] { IPropertySource.class, WorkbenchAdapter.class };

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == IPropertySource.class && adaptableObject instanceof Todo) {
			return adapterType.cast(new TodoPropertySource((Todo) adaptableObject));
		} else if (adapterType.isAssignableFrom(WorkbenchAdapter.class) && adaptableObject instanceof Todo) {
			return adapterType.cast(new TodoWorkbenchAdapter());
		} else if (adapterType == IContentOutlinePage.class && adaptableObject instanceof Todo) {
			return adapterType.cast(new TodoContentOutlinePage((Todo) adaptableObject));
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return adapterList;
	}

}