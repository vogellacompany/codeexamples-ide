package com.vogella.tasks.ui.parts;

import static org.eclipse.jface.layout.GridDataFactory.fillDefaults;
import static org.eclipse.jface.widgets.WidgetFactory.button;
import static org.eclipse.jface.widgets.WidgetFactory.text;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.vogella.tasks.extendedsupplier.DirectTask;
import com.vogella.tasks.model.Task;

public class PlaygroundPart {
	private Text text;
	private Browser browser;
	private ControlDecoration deco;

	@Inject
	public void name(@DirectTask Task task) {
		System.out.println(task);
	}
	@PostConstruct
	public void createControls(Composite parent) {
		parent.setLayout(new GridLayout(2, false));



		text = text(SWT.BORDER).message("Enter City").onModify(e -> toggleDeco(deco, e))
				.layoutData(fillDefaults().grab(true, false).indent(8, 0).create()).create(parent);
		deco = new ControlDecoration(text, SWT.TOP | SWT.LEFT);
		Image image = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
				.getImage();
		// set description and image
		deco.setDescriptionText("Use CTRL + SPACE to see possible values");
		deco.setImage(image);
		// always show decoration
		deco.setShowOnlyOnFocus(false);
		// Define field assists for the text widget
		// use "." and " " activate the content proposals
		char[] autoActivationCharacters = new char[] { '.', ' ' };
		KeyStroke keyStroke;
		try {
			keyStroke = KeyStroke.getInstance("Ctrl+Space");
			new ContentProposalAdapter(text, new TextContentAdapter(),
					new SimpleContentProposalProvider("Hamburg", "New Delhi", "Washington, D.C. "), keyStroke,
					autoActivationCharacters);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		


		button(SWT.PUSH).text("Search").onSelect(e -> updateBrowser()).create(parent);

		browser = new Browser(parent, SWT.NONE);
		browser.setLayoutData(fillDefaults().grab(true, true).span(2, 1).create());

		browser = new Browser(parent, SWT.NONE);
		browser.setLayoutData(fillDefaults().grab(true, true).span(2, 1).create());

	}

	private void toggleDeco(ControlDecoration deco, ModifyEvent e) {
		Text source = (Text) e.getSource();
		if (!source.getText().isEmpty()) {
			deco.hide();
		} else {
			deco.show();
		}
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