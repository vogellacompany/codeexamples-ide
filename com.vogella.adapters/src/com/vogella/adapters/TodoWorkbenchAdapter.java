package com.vogella.adapters;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchAdapter;

import com.vogella.tasks.model.Task;

public class TodoWorkbenchAdapter extends WorkbenchAdapter {

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_ELEMENT);
	}

	@Override
	public String getLabel(Object object) {
		return object instanceof Task task ? task.getSummary() : super.getLabel(object);
	}

	@Override
	public Object[] getChildren(Object object) {
		if (object instanceof Task task) {
			IResource resource = TaskResources.get(task);
			if (resource != null) {
				return new IResource[] { resource };
			}
		}
		return super.getChildren(object);
	}

	@Override
	public StyledString getStyledText(Object object) {
		if (object instanceof Task task) {
			StyledString styledString = new StyledString(task.getSummary());
			if (task.isDone()) {
				Styler styler = new Styler() {
					@Override
					public void applyStyles(TextStyle textStyle) {
						// tasks which are done get a green background
						textStyle.background = Display.getCurrent().getSystemColor(SWT.COLOR_GREEN);
					}
				};
				styledString.setStyle(0, task.getSummary().length(), styler);
			}
			return styledString;
		}
		return super.getStyledText(object);
	}
}
