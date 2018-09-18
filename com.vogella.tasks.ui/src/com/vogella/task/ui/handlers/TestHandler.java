package com.vogella.task.ui.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.advanced.MArea;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public class TestHandler {

	@Execute
	public void execute(EPartService service, EModelService modelService, MWindow window) {
		MPart createPart = service.createPart("com.vogella.tasks.ui.parts.playgroundpart");
		MPlaceholder find = (MPlaceholder) modelService.find("org.eclipse.ui.editorss", window);
		MArea ref = (MArea) find.getRef();
		MPartStack stack = (MPartStack) ref.getChildren().get(0);
		stack.getChildren().add(createPart);
	}

	@CanExecute
	public void execute() {

	}
}