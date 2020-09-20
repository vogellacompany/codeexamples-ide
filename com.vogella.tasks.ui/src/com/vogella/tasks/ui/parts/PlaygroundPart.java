package com.vogella.tasks.ui.parts;

import static org.eclipse.jface.layout.GridDataFactory.fillDefaults;
import static org.eclipse.jface.widgets.WidgetFactory.button;
import static org.eclipse.jface.widgets.WidgetFactory.text;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.annotation.PostConstruct;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class PlaygroundPart {
	private Text text;
	private Browser browser;

	@PostConstruct
	public void createControls(Composite parent) {
		parent.setLayout(new GridLayout(2, false));

		text = text(SWT.BORDER).message("Enter City").layoutData(fillDefaults().grab(true, false).create())
				.create(parent);
		button(SWT.PUSH).text("Search").onSelect(e -> updateBrowser()).create(parent);

		browser = new Browser(parent, SWT.NONE);
		browser.setLayoutData(fillDefaults().grab(true, true).span(2, 1).create());

	}

	private void updateBrowser() {
		String city = text.getText();
		if (city.isEmpty()) {
			return;
		}
		try {
			browser.setUrl("https://www.google.com/maps/place/" + URLEncoder.encode(city, "UTF-8") + "/&output=embed");

		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
	}


	@Focus
	public void onFocus() {
		text.setFocus();
	}
}