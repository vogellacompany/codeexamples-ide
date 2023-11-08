package com.vogella.tasks.ui.parts;

import javax.annotation.PostConstruct;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class PlaygroundPart {
	private Text modelText;
	private Text targetText;

	@PostConstruct
	public void createPartControl(Composite parent) {
		modelText = WidgetFactory.text(SWT.BORDER).message("I'm leading").create(parent);
		targetText = WidgetFactory.text(SWT.BORDER).message("Me tool").create(parent);
		// modify lister an modelText and update targetText
		// modify lister an targetText and update modelText
		// Bedingung einbauen, ob Inhalt gleich ist und nur Update wenn nicht gleich
		// Triggered irgendwas (wait), wait wird neu gestartet
		// FocusLister.out auf beide Widget setzen und
		DataBindingContext dbx = new DataBindingContext();
		ISWTObservableValue<String> observe = WidgetProperties.text(SWT.Modify).observeDelayed(3000, modelText);
		ISWTObservableValue<String> observetarget = WidgetProperties.text(SWT.Modify).observe(targetText);
		dbx.bindValue(observetarget, observe);
		modelText.setText("Moin");
		targetText.setText("Servus");
	}
}