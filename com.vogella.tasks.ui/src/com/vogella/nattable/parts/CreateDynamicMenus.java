 
package com.vogella.nattable.parts;

import java.util.List;

import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

public class CreateDynamicMenus {

	@AboutToShow
	public void aboutToShow(List<MMenuElement> items, EModelService modelService) {
		MDirectMenuItem createModelElement = modelService.createModelElement(MDirectMenuItem.class);
		createModelElement.setLabel("Tuet");
		items.add(createModelElement);
	}
		
}