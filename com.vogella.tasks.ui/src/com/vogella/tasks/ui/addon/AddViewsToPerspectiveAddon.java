package com.vogella.tasks.ui.addon;

import java.util.List;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import jakarta.inject.Inject;
import jakarta.inject.Named;

public class AddViewsToPerspectiveAddon {

	@Inject
	private EPartService partService;

	@Inject
	private EModelService modelService;
	private String taskOverviewPartId = "com.vogella.tasks.ui.partdescriptor.overview";
	private String taskDetailsPartId = "com.vogella.tasks.ui.partdescriptor.details";

	private String id;

	@Inject
	@Optional
	private void subscribeApplicationCompleted(
			@UIEventTopic(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE) MApplication app) {
		MUIElement perspective = modelService.find(id, app);
		if (perspective instanceof MPerspective) {
			List<MPartStack> stack = modelService.findElements(perspective, null, MPartStack.class);
			MPart overviewPart = null;
			MPart detailsPart = null;
			
			if (modelService.find(taskOverviewPartId, perspective)==null) {
				overviewPart = partService.createPart(taskOverviewPartId);
			}
			if (modelService.find(taskDetailsPartId, perspective)==null) {
				 detailsPart = partService.createPart(taskDetailsPartId);
			}

			if (stack.size() > 0) {
				if (overviewPart!=null) {
					stack.get(0).getChildren().add(overviewPart);
				}
				if (detailsPart!=null) {
					stack.get(0).getChildren().add(detailsPart);
				}
			}
			
		}
	}

	@Inject
	@Optional
	private void subscribeApplicationCompleted(@Named("activeWorkbenchWindow.activePerspective") String id,
			MApplication app) {
		this.id = id;
	}

}