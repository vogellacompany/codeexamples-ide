package com.vogella.swt.widgets;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Compound widget showing a label next to a single-line text field.
 */
public class LabelWithText extends Composite {

	private final Label label;
	private final Text text;

	public LabelWithText(Composite parent, int style) {
		super(parent, style);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(this);
		label = new Label(this, SWT.NONE);
		text = new Text(this, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(text);
	}

	public String getLabel() {
		checkWidget();
		return label.getText();
	}

	public void setLabel(String value) {
		checkWidget();
		label.setText(value);
		layout();
	}

	public String getText() {
		checkWidget();
		return text.getText();
	}

	public void setText(String value) {
		checkWidget();
		text.setText(value);
	}
}
