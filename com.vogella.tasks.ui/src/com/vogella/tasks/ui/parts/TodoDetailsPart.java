package com.vogella.tasks.ui.parts;

import javax.annotation.PostConstruct;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Text;

public class TodoDetailsPart {
	private Text txtSummary;
	private Text txtDescription;
	private DateTime dateTime;
	private Button btnDone;

	@PostConstruct
	public void createControls(Composite parent) {
		parent.setLayout(new GridLayout(2, false));
		
		GridDataFactory gdFactory = GridDataFactory.fillDefaults();
		LabelFactory labelFactory = LabelFactory.newLabel(SWT.NONE);

		labelFactory.text("Summary").create(parent);

		txtSummary = WidgetFactory.text(SWT.BORDER).layoutData(gdFactory.grab(true, false).create())//
				.create(parent);

		labelFactory.text("Description").create(parent);
		txtDescription = WidgetFactory.text(SWT.BORDER | SWT.MULTI | SWT.V_SCROLL)
				.layoutData(gdFactory.align(SWT.FILL, SWT.FILL).grab(true, true).create()).create(parent);

		labelFactory.text("Due Date").create(parent);

		// Factory planned for 2020-12 release
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=567110

		dateTime = new DateTime(parent, SWT.BORDER);
		dateTime.setLayoutData(gdFactory.align(SWT.FILL, SWT.CENTER).grab(false, false).create());

		labelFactory.text("").create(parent);

		btnDone = WidgetFactory.button(SWT.CHECK).text("Done").create(parent);
	}

	@Focus
	public void setFocus() {
		txtSummary.setFocus();
	}

}