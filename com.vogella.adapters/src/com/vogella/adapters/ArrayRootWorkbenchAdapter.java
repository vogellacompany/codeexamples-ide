package com.vogella.adapters;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.ui.model.WorkbenchAdapter;

public class ArrayRootWorkbenchAdapter extends WorkbenchAdapter {

	private Object root;

	public ArrayRootWorkbenchAdapter(Object root) {
		this.root = root;
	}

	@Override
	public Object[] getChildren(Object object) {
		return ArrayContentProvider.getInstance().getElements(root);
	}
}
