package com.vogella.tasks.ui.parts;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.vogella.swt.widgets.LabelWithText;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class PlaygroundPart {
	private Text modelText;

	@Inject
	private MPart part;

	@PostConstruct
	public void createControls(Composite parent) {
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(parent);
		modelText = WidgetFactory.text(SWT.BORDER).message("Type to make the part dirty")
				.layoutData(GridDataFactory.fillDefaults().grab(true, false).create()).create(parent);
		modelText.addModifyListener(e -> part.setDirty(true));

		LabelWithText labelWithText = new LabelWithText(parent, SWT.NONE);
		labelWithText.setLabel("Custom widget");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(labelWithText);
	}

	@Persist
	public void save() {
		part.setDirty(false);
	}

	@Focus
	public void setFocus() {
		modelText.setFocus();
	}
}
