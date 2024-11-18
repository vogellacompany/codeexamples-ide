package com.vogella.tasks.ui.handlers;

import java.util.Collection;
import java.util.List;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.advanced.MArea;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

import com.vogella.tasks.model.Task;

public class OpenEditorHandler {

    private static final String EDITOR_ID = "com.vogella.tasks.ui.partdescriptor.editor";

    @Execute
	public void execute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) List<Task> tasks, MApplication application,
            EModelService modelService, EPartService partService) {

        // sanity check
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

		Task task = tasks.get(0);

        String id = String.valueOf(task.getId());

        // maybe the editor is already open?
        Collection<MPart> parts = partService.getParts();

        // if the editor is open show it
        for (MPart part : parts) {
			String currentId = part.getPersistedState().get(Task.FIELD_ID);
            if (currentId != null && currentId.equals(id)) {
                partService.showPart(part, PartState.ACTIVATE);
                return;
            }
        }

        // editor was not open, create it
        MPart part = partService.createPart(EDITOR_ID);

        part.setElementId(id);
		part.getPersistedState().put(Task.FIELD_ID, id);

        // create a nice label for the part header
        String header = "ID:" + id + " " + task.getSummary();
        part.setLabel(header);

		MPlaceholder stack = (MPlaceholder) modelService.find("org.eclipse.ui.editorss", application);
		MArea ref = (MArea) stack.getRef();
		if (!ref.getChildren().isEmpty()) {
			MPartStack mPartSashContainerElement = (MPartStack) ref.getChildren().get(0);
			mPartSashContainerElement.getChildren().add(part);
		}
        partService.showPart(part, PartState.ACTIVATE);
    }

    @CanExecute
	public boolean canExecute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) List<Task> tasks) {
		return tasks != null && !tasks.isEmpty();
    }
}