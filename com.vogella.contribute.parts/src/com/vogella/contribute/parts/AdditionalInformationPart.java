package com.vogella.contribute.parts;

import static org.eclipse.jface.databinding.swt.typed.WidgetProperties.text;

import javax.annotation.PostConstruct;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class AdditionalInformationPart {
	@PostConstruct
	public void postConstruct(Composite parent) {
		Text modelText = new Text(parent, SWT.BORDER | SWT.MULTI);
		Text targetText = new Text(parent, SWT.BORDER | SWT.MULTI);
		DataBindingContext db = new DataBindingContext();
		ISWTObservableValue<String> modelObserve = text(SWT.Modify).observeDelayed(5000, modelText);
		ISWTObservableValue<String> targetObserve = text(SWT.Modify).observe(targetText);
		db.bindValue(targetObserve, modelObserve);
		modelText.setText("Morning");
		targetText.setText("Evening");

//		modelText.addModifyListener(e -> targetText.setText(modelText.getText()));
//		targetText.addModifyListener(e -> modelText.setText(targetText.getText()));
	}
}