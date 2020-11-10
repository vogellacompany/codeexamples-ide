package com.vogella.adapters;

import javax.annotation.PostConstruct;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.dialogs.filteredtree.FilteredTree;
import org.eclipse.e4.ui.dialogs.filteredtree.PatternFilter;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.vogella.tasks.model.TaskService;

public class AdapterOverviewPart {

    private FilteredTree filteredTree;

    @PostConstruct
    public void createPartControl(Composite parent, TaskService todoService, ESelectionService selectionService) {

        PatternFilter patternFilter = new PatternFilter();
        filteredTree = new FilteredTree(parent, SWT.FULL_SELECTION, patternFilter);
        filteredTree.getViewer().setContentProvider(new WorkbenchContentProvider());
        filteredTree.getViewer().setLabelProvider(new DecoratingStyledCellLabelProvider(new WorkbenchLabelProvider(), null, null));

		todoService.consume(todos -> {
            filteredTree.getViewer().setInput(new ArrayRootWorkbenchAdapter(todos));
        });

        filteredTree.getViewer().addSelectionChangedListener(e -> {
            // Properties view reacts on post selection
            selectionService.setPostSelection(e.getSelection());
        });
    }

    @Focus
    public void setFocus() {
        filteredTree.setFocus();
    }
}