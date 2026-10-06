package com.vogella.adapters;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import com.vogella.tasks.model.Task;

public class TodoPropertySource implements IPropertySource {

	private final Task task;

	public TodoPropertySource(Task task) {
		this.task = task;
	}

	@Override
	public boolean isPropertySet(Object id) {
		return false;
	}

	@Override
	public Object getEditableValue() {
		return this;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return new IPropertyDescriptor[] { new TextPropertyDescriptor(Task.FIELD_SUMMARY, "Summary"),
				new TextPropertyDescriptor(Task.FIELD_DESCRIPTION, "Description") };
	}

	@Override
	public Object getPropertyValue(Object id) {
		if (Task.FIELD_SUMMARY.equals(id)) {
			return task.getSummary();
		}
		if (Task.FIELD_DESCRIPTION.equals(id)) {
			return task.getDescription();
		}
		return null;
	}

	@Override
	public void resetPropertyValue(Object id) {
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		String s = (String) value;
		if (Task.FIELD_SUMMARY.equals(id)) {
			task.setSummary(s);
		}
		if (Task.FIELD_DESCRIPTION.equals(id)) {
			task.setDescription(s);
		}
	}

}
