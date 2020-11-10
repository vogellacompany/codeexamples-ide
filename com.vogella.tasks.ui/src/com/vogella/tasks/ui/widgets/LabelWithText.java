package com.vogella.tasks.ui.widgets;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class LabelWithText extends Composite {

	private Label label;

	public LabelWithText(Composite parent, int style) {
		super(parent, style);
		parent.setLayout(GridLayoutFactory.fillDefaults().create());
		label = new Label(parent, SWT.NONE);
		Text text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	public void setLabelText(String s) {
	}
}
