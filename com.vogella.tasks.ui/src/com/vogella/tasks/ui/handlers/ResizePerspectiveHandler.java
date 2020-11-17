
package com.vogella.tasks.ui.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

public class ResizePerspectiveHandler {

	@Execute
	public void execute(EModelService modelService, MApplication app) {


		MWindow window = modelService.createModelElement(MWindow.class);
		window.setWidth(200);
		window.setHeight(300);
		MPartStack createModelElement = modelService.createModelElement(MPartStack.class);
		MPart createModelElement2 = modelService.createModelElement(MPart.class);
		createModelElement2.setLabel("Lars sein dynamischer part");
		createModelElement2.setCloseable(true);
		createModelElement.getChildren().add(createModelElement2);
		window.getChildren().add(createModelElement);
		
		// add the new window to the application
		app.getChildren().add(window);
//		List<MPartSashContainerElement> children = activePerspective.getChildren();
//		if (!children.isEmpty()) {
//			// only resize if first child is a sash
//			if (children.get(0) instanceof MPartSashContainer) {
//				MPartSashContainer sash = (MPartSashContainer) children.get(0);
//				List<MPartSashContainerElement> children2 = sash.getChildren();
//				// first one should get 30
//				// second one should get 70
//				// TODO ask product owner what to do if we have more than 2 children. ;-)
//
//				if (children2.size() == 2) {
//					children2.get(0).setContainerData("3000");
//					children2.get(1).setContainerData("7000");
//				}
//			}
//		}
	}

	// neue anforderng, resize soll nur enabled sein, wenn wir in der .. Java
	// perspective sind

	@CanExecute
	public boolean canIDoIt(MPerspective perspective) {
		return true;
		// (perspective.getElementId().equals("org.eclipse.jdt.ui.JavaPerspective"));
	}
}