package com.vogella.contribute.parts;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.vogella.tasks.model.ITodoService;
import com.vogella.tasks.model.Todo;

public class AdditionalInformationPart {
	@Inject
	public void name(Composite parent, ITodoService service) {
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginWidth = 5;
		gridLayout.marginHeight = 5;
		gridLayout.verticalSpacing = 0;
		gridLayout.horizontalSpacing = 0;
		parent.setLayout(gridLayout);

		Button button = new Button(parent, SWT.PUSH);


		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.setText("Druecke mich");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				System.out.println("moin");
			}
		});
		Text text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setText("Hello");
		List<Todo> list = new ArrayList<>();
		service.getTodos(list::addAll);
//		Combo combo = new Combo(parent, SWT.DROP_DOWN);
//		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		for (Todo todo : list) {
//			combo.add(todo.getSummary());
//		}
//		int selectionIndex = combo.getSelectionIndex();
//		String item = combo.getItem(selectionIndex);

		ComboViewer comboViewer = new ComboViewer(parent);
		comboViewer.setContentProvider(ArrayContentProvider.getInstance());
		comboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				Todo todo = (Todo) element;
				return todo.getSummary();
			}
		});
		comboViewer.setInput(list);
		Object firstElement = comboViewer.getStructuredSelection().getFirstElement();
	}
}