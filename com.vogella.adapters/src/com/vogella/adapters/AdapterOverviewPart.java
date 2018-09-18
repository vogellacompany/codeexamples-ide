package com.vogella.adapters;

import javax.annotation.PostConstruct;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.dialogs.filteredtree.FilteredTree;
import org.eclipse.e4.ui.dialogs.filteredtree.PatternFilter;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.vogella.tasks.model.ITodoService;

@SuppressWarnings("restriction")
public class AdapterOverviewPart {

	private FilteredTree filteredTree;

	@PostConstruct
	public void createPartControl(Composite parent, ITodoService todoService, ESelectionService selectionService) {

		PatternFilter patternFilter = new PatternFilter();
		filteredTree = new FilteredTree(parent, SWT.FULL_SELECTION, patternFilter);
		filteredTree.getViewer().setContentProvider(new WorkbenchContentProvider());
		filteredTree.getViewer()
				.setLabelProvider(new DecoratingStyledCellLabelProvider(new WorkbenchLabelProvider(), null, null));

		todoService.getTodos(todos -> {
			filteredTree.getViewer().setInput(new ArrayRootWorkbenchAdapter(todos));
		});

		filteredTree.getViewer().addSelectionChangedListener(e -> {
			// Properties view reacts on post selection
			selectionService.setPostSelection(e.getSelection());
		});
		int operations = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;

		Transfer[] dropTransfers = new Transfer[] { LocalSelectionTransfer.getTransfer() };

		filteredTree.getViewer().addDropSupport(operations, dropTransfers,
				new TodoDropAdapter(filteredTree.getViewer()));

	}

	@Focus
	public void setFocus() {
		filteredTree.setFocus();
	}
}
