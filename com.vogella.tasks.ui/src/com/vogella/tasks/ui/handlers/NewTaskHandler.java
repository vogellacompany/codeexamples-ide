package com.vogella.tasks.ui.handlers;

import java.util.List;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

public class NewTaskHandler {
	@Execute
	public void execute(EModelService service, MApplication w) {
//		// use -1 to indicate a not existing id
//		Task task = new Task(-1);
//		task.setDueDate(LocalDate.now());
//		WizardDialog dialog = new WizardDialog(shell, new TaskWizard(task));
//		if (dialog.open() == Window.OK) {
//			// call service to save task object
//			taskService.update(task);
//		}
//		List<MPart> elements = service.findElements(w, null, MPart.class);
//		for (MPart mPart : elements) {
//			mPart.setCloseable(!mPart.isCloseable());
//			mPart.setLabel("Lars" + mPart.getLabel());
//		}

		MPart editor = service.createModelElement(MPart.class);
		editor.setLabel("Michaels dynamic Part");
		editor.setContributionURI("1");
		editor.setCloseable(true);
		editor.setIconURI("platform:/plugin/com.vogella.tasks.ui/icons/taskdetail.png");
//		List<MArea> elements = service.findElements(w, "org.eclipse.ui.editorss", MArea.class);
		List<MPartStack> elements = service.findElements(w, "org.eclipse.e4.primaryDataStack", MPartStack.class);
		MPartStack mArea = elements.get(0);
		mArea.getChildren().add(editor);
	}
}
