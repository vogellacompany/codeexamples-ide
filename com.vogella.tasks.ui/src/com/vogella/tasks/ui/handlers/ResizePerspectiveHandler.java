package com.vogella.tasks.ui.handlers;

import java.util.List;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;

/**
 * Splits the top-level sash of the active perspective 30/70.
 */
public class ResizePerspectiveHandler {

	@Execute
	public void execute(MPerspective perspective) {
		List<MPartSashContainerElement> sashChildren = ((MPartSashContainer) perspective.getChildren().get(0))
				.getChildren();
		sashChildren.get(0).setContainerData("3000");
		for (int i = 1; i < sashChildren.size(); i++) {
			sashChildren.get(i).setContainerData(String.valueOf(7000 / (sashChildren.size() - 1)));
		}
	}

	@CanExecute
	public boolean canExecute(@Optional MPerspective perspective) {
		return perspective != null && !perspective.getChildren().isEmpty()
				&& perspective.getChildren().get(0) instanceof MPartSashContainer sash
				&& sash.getChildren().size() > 1;
	}
}
