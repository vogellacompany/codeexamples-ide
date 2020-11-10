package com.vogella.tasks.ui.parts;

import javax.annotation.PostConstruct;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

public class PlaygroundPart2 {
	private Text text;
	private Browser browser;
	private ControlDecoration deco;
	private Text text_1;
	private Table table;

//
//	@Inject
//	public void name(@DirectTask Task task) {
//		System.out.println(task);
//	}
	@PostConstruct
	public void createControls(Composite parent) {
		parent.setLayout(new GridLayout(2, false));

		text_1 = new Text(parent, SWT.BORDER);
		text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(parent, SWT.NONE);

		Button btnNewButton = new Button(parent, SWT.NONE);
		btnNewButton.setText("New Button");

		Label lblNewLabel = new Label(parent, SWT.NONE);
		lblNewLabel.setText("New Label");

		table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		new Label(parent, SWT.NONE);

		
	}
}