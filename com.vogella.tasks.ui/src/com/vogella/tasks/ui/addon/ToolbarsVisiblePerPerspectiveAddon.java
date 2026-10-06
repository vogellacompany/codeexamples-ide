package com.vogella.tasks.ui.addon;

import java.util.List;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspectiveStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.UIEvents.EventTags;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.osgi.service.event.Event;

import jakarta.inject.Inject;

/**
 * Shows toolbars tagged with "perspective:&lt;id&gt;" only in the perspective with
 * that id, toolbars without such a tag are not changed.
 */
public class ToolbarsVisiblePerPerspectiveAddon {

	private static final String PERSPECTIVE_TAG_PREFIX = "perspective:";

	@Inject
	private EModelService modelService;

	@Inject
	@Optional
	public void subscribeTopicSelectedElement(
			@UIEventTopic(UIEvents.ElementContainer.TOPIC_SELECTEDELEMENT) Event event) {
		Object element = event.getProperty(EventTags.ELEMENT);
		Object newValue = event.getProperty(EventTags.NEW_VALUE);
		if (!(element instanceof MPerspectiveStack) || !(newValue instanceof MPerspective perspective)) {
			return;
		}
		MWindow window = modelService.getTopLevelWindowFor(perspective);
		List<MToolBar> toolbars = modelService.findElements(window, null, MToolBar.class);
		for (MToolBar toolbar : toolbars) {
			// IDE toolbars carry other tags, so only perspective tags decide
			boolean hasPerspectiveTag = toolbar.getTags().stream().anyMatch(t -> t.startsWith(PERSPECTIVE_TAG_PREFIX));
			if (!hasPerspectiveTag) {
				continue;
			}
			boolean visible = toolbar.getTags().contains(PERSPECTIVE_TAG_PREFIX + perspective.getElementId());
			toolbar.setVisible(visible);
			toolbar.setToBeRendered(visible);
		}
	}
}
