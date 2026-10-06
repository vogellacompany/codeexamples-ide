package com.vogella.adapters;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;

import com.vogella.tasks.model.Task;

public class TodoDropAdapter extends ViewerDropAdapter {

	private final StructuredViewer structuredViewer;

	protected TodoDropAdapter(StructuredViewer viewer) {
		super(viewer);
		structuredViewer = viewer;
	}

	@Override
	public boolean performDrop(Object data) {
		// LocalSelectionTransfer transports ISelections as data
		if (getCurrentTarget() instanceof Task task && data instanceof IStructuredSelection selection) {
			// works for every selected element that provides an IResource adapter
			IResource resource = Adapters.adapt(selection.getFirstElement(), IResource.class);
			if (resource == null) {
				return false;
			}
			TaskResources.set(task, resource);
			// refresh so that the content provider picks up the new child of the task
			structuredViewer.refresh(task);
			return true;
		}
		return false;
	}

	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		return target instanceof Task && LocalSelectionTransfer.getTransfer().isSupportedType(transferType);
	}
}
