package com.vogella.tasks.ui.parts;

import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.vogella.tasks.model.TaskService;

import jakarta.annotation.PostConstruct;

public class PlaygroundPart2 {
	private Text textModel;
	private Text textTarget;

	@PostConstruct
	public void createControls(Composite parent, MPart part) {
		parent.setLayout(new GridLayout(1, false));
		textModel = WidgetFactory.text(SWT.BORDER).layoutData(GridDataFactory.fillDefaults().create()).create(parent);
		textTarget = WidgetFactory.text(SWT.BORDER).layoutData(GridDataFactory.fillDefaults().create()).create(parent);

		textModel.addModifyListener(e -> part.setDirty(true));

		Text text = TextFactory.newText(SWT.NONE).create(parent);
		text.addModifyListener(e -> {

		});

	}

	@Persist
	public void speichere(MPart part, TaskService taskService) {

		part.setDirty(false);

	}
}