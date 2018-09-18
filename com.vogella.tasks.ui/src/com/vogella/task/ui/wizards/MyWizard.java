package com.vogella.task.ui.wizards;

import org.eclipse.jface.wizard.Wizard;

public class MyWizard extends Wizard {

	public MyWizard() {
		setWindowTitle("New Wizard");
	}

	@Override
	public void addPages() {
		addPage(new MyWPage());
	}

	@Override
	public boolean performFinish() {
		return false;
	}

}
