package com.vogella.adapters;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;

import com.vogella.tasks.model.Todo;

public class TodoDropAdapter extends ViewerDropAdapter {

	private StructuredViewer structuredViewer;

	protected TodoDropAdapter(StructuredViewer viewer) {
		super(viewer);
		structuredViewer = viewer;
	}

	@Override
	public boolean performDrop(Object data) {
		// The target is the object the mouse points to during DnD
		Object target = getCurrentTarget();
		// LocalSelectionTransfer transports ISelections as data
		if (target instanceof Todo && data instanceof IStructuredSelection) {
			Todo todo = (Todo) target;
			IStructuredSelection sSelection = (IStructuredSelection) data;
			// Selections from the Project Explorer are usually already IResource objects,
			// but now the DnD can work for any LocalSelectionTransfer objects, which
			// provides an IResource adapter.
			IResource resource = Adapters.adapt(sSelection.getFirstElement(), IResource.class);
			todo.setResource(resource);
			// refresh element so that the content provider realizes the new child of the
			// todo
			structuredViewer.refresh(target);
			return true;
		}
		return false;
	}

	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		// only drop elements on Todo objects in the viewer and currently only
		// LocalSelectionTransfer is supported
		return target instanceof Todo && LocalSelectionTransfer.getTransfer().isSupportedType(transferType);
	}
}
