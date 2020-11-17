package com.vogella.tasks.ui.parts;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.annotation.PostConstruct;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

public class PlaygroundPart {
	private Text text;
	private Browser browser;
	private ControlDecoration deco;
	private Label label;



	@PostConstruct
	public void createPartControl(Composite parent) {
		// more code
		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				"com.vogella.preferences.page");
		String string = scopedPreferenceStore.getString("MySTRING1");
		label = WidgetFactory.label(SWT.NONE).layoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false)).text(string).create(parent);
		// add change listener to the node so that we are notified
		// in case of changes
		scopedPreferenceStore.addPropertyChangeListener(event -> {
			if (event.getProperty().equals("MySTRING1")) {
				label.setText(event.getNewValue().toString());
			}
		});
	}

//	@PostConstruct
//	public void createControls(Composite parent) {
//		parent.setLayout(new GridLayout(2, false));
//		
//		
//
//		text = text(SWT.BORDER).message("Enter City").onModify(e -> toggleDeco(deco, e))
//				.layoutData(fillDefaults().grab(true, false).indent(8, 0).create()).create(parent);
//		deco = new ControlDecoration(text, SWT.TOP | SWT.LEFT);
//		Image image = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
//				.getImage();
//		// set description and image
//		deco.setDescriptionText("Use CTRL + SPACE to see possible values");
//		deco.setImage(image);
//		// always show decoration
//		deco.setShowOnlyOnFocus(false);
//		// Define field assists for the text widget
//		// use "." and " " activate the content proposals
//		char[] autoActivationCharacters = new char[] { '.', ' ' };
//		KeyStroke keyStroke;
//		try {
//			keyStroke = KeyStroke.getInstance("Ctrl+Space");
//			new ContentProposalAdapter(text, new TextContentAdapter(),
//					new SimpleContentProposalProvider("Hamburg", "New Delhi", "Washington, D.C. "), keyStroke,
//					autoActivationCharacters);
//		} catch (ParseException e1) {
//			e1.printStackTrace();
//		}
//		
//		Checkbox box = new Checkbox(parent, SWT.NONE);
//		box.setSelection(true);
//
//		WidgetFactory.button(SWT.PUSH).text("Search").onSelect(e -> updateBrowser()).create(parent);
//
//		Chips chip1 = new Chips(parent, SWT.CLOSE);
//		chip1.setText("Example");
//		chip1.setChipsBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
//		chip1.setLayoutData(GridDataFactory.swtDefaults().grab(false, false).span(1, 1).create());
//
//		FloatingText txt1 = new FloatingText(parent, SWT.BORDER);
//		txt1.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
//		txt1.getText().setMessage("Enter something");
//
//		browser = new Browser(parent, SWT.NONE);
//		browser.setLayoutData(fillDefaults().grab(true, true).span(2, 1).create());
//
//		browser = new Browser(parent, SWT.NONE);
//		browser.setLayoutData(fillDefaults().grab(true, true).span(2, 1).create());
//
//	}

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

//	@Focus
//	public void onFocus() {
//		text.setFocus();
//	}
}