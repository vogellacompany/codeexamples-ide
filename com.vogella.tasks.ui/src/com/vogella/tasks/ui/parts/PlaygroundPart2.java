package com.vogella.tasks.ui.parts;

import jakarta.annotation.PostConstruct;

import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.vogella.tasks.model.TaskService;

public class PlaygroundPart2 {
	private Text textModel;
	private Text textTarget;


	@PostConstruct
	public void createControls(Composite parent, MPart part) {
		parent.setLayout(new GridLayout(1, false));
		textModel = WidgetFactory.text(SWT.BORDER).layoutData(GridDataFactory.fillDefaults().create()).create(parent);
		textTarget = WidgetFactory.text(SWT.BORDER).layoutData(GridDataFactory.fillDefaults().create()).create(parent);


		textModel.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				part.setDirty(true);
			}
		});
		
	}

	@Persist
	public void speichere(MPart part, TaskService taskService) {

		part.setDirty(false);

	}
}