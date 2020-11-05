 
package com.vogella.tasks.ui.handlers;

import java.util.List;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public class ActiveWhenHandlerForPart {

	@Execute
	public void execute(EModelService modelService, MApplication app, MWindow window, EPartService partService) {
		List<MPart> parts = modelService.findElements(app, null, MPart.class);
		for (MPart mPart : parts) {
			mPart.setLabel("SAP rules " + mPart.getLabel());
		}
		MPart createModelElement = partService.createPart("com.vogella.tasks.ui.partdescriptor.playground");
//		MPart createModelElement = modelService.createModelElement(MPart.class);
//		createModelElement.setLabel("SAP dynamic part");
//		createModelElement.setCloseable(true);
//		createModelElement
//				.setContributionURI("bundleclass://com.vogella.tasks.ui/com.vogella.tasks.ui.parts.PlaygroundPart");
		MPerspective activePerspective = modelService.getActivePerspective(window);
		MPartStack partStack = (MPartStack) modelService.find("topLeft", activePerspective);
		partStack.getChildren().add(createModelElement);

	}
//	@Execute
//	public void execute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) List<Task> tasks,
//			TaskService taskService) {
//		for (Task task : tasks) {
//			taskService.delete(task.getId());
//		}
//	}
//		
}