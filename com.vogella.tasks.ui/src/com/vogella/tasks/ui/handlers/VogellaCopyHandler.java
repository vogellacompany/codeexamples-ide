
package com.vogella.tasks.ui.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;

public class VogellaCopyHandler {

	public VogellaCopyHandler() {
	}

	@Execute
	public void execute() {
		System.out.println("Testing commands");
	}

	@CanExecute
	public boolean check() {
		return true;
	}

}