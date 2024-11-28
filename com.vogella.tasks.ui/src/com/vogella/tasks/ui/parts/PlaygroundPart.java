package com.vogella.tasks.ui.parts;

import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.vogella.tasks.model.TaskService;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class PlaygroundPart {
	private Text modelText;
	private Text targetText;

	@Inject 
	MPart part;
	@PostConstruct
	public void createPartControl(Composite parent) {
		modelText = WidgetFactory.text(SWT.BORDER).message("I'm leading").create(parent);
		modelText.addModifyListener(e -> {
			// NOW I'M DIRTY
			part.setDirty(true);
		});
	}

	@Persist
	public void name(TaskService service) {
		// I save ...
		part.setDirty(false);

	}
}