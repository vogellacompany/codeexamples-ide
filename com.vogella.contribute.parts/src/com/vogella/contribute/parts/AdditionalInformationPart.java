package com.vogella.contribute.parts;

import javax.annotation.PostConstruct;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class AdditionalInformationPart {
	@PostConstruct
	public void postConstruct(Composite parent) {
		new Text(parent, SWT.BORDER | SWT.MULTI);
	}
}