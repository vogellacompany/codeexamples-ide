
package com.vogella.tasks.ui.handlers;

import java.util.List;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

public class ActiveWhenHandlerForPart {

	@Execute
	public void execute(EModelService modelService, MWindow window) {
		MPerspective activePerspective = modelService.getActivePerspective(window);
		List<MPartSashContainerElement> children = activePerspective.getChildren();
		if (!children.isEmpty()) {

		}
	}
}