package com.vogella.adapters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.model.WorkbenchAdapter;

import com.vogella.tasks.model.Task;

public class TodoAdapterFactory implements IAdapterFactory {

    // use a static final field so that the adapterList is only instantiated once
    private static final Class<?>[] adapterList = new Class<?>[] { WorkbenchAdapter.class };

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType.isAssignableFrom(WorkbenchAdapter.class) && adaptableObject instanceof Task) {
            return adapterType.cast(new TodoWorkbenchAdapter());
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return adapterList;
    }

}