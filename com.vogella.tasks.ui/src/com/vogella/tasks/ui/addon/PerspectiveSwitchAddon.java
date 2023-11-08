package com.vogella.tasks.ui.addon;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.UIEvents.EventTags;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.osgi.service.event.Event;

public class PerspectiveSwitchAddon {

	@Inject
	private EModelService modelService;

	@Inject
	@Optional
	public void subscribeTopicSelectedElement(
			@UIEventTopic(UIEvents.ElementContainer.TOPIC_SELECTEDELEMENT) Event event) {
		
		
		Object newValue = event.getProperty(EventTags.NEW_VALUE);

		// only run this, if the NEW_VALUE is a MPerspective
		if (!(newValue instanceof MPerspective)) {
			return;
		}

		MPerspective perspective = (MPerspective) newValue;

		// Get the MWindow, where we want to change the label
		MWindow topLevelWindowOfPerspective = modelService.getTopLevelWindowFor(perspective);

		topLevelWindowOfPerspective.setLabel(perspective.getLabel());
	}
}