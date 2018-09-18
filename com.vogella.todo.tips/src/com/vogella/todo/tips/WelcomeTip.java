package com.vogella.todo.tips;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;

public class WelcomeTip extends Tip implements IHtmlTip {

	public WelcomeTip(String providerId) {
		super(providerId);
	}

	@Override
	public Date getCreationDate() {
		SimpleDateFormat sdf = new SimpleDateFormat("09/01/2018");
		try {
			Date parse = sdf.parse("pYYMMDD");
			return parse;
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getSubject() {
		return "Welcome to the tips framework";
	}

	@Override
	public String getHTML() {
		return "<h2>Welcome to the Tips Framework</h2>It can show tips from various tip providers. This provider has tips about tips which will show you how to navigate this UI."
				+ " The dialog is this Tip UI. Tips appear here in various forms. They can come from Twitter, a Wiki, a Website, a file or even from Java, like this one."
				+ "<br><br>" + "Press <b><i>Next Tip</i></b> to see how to start tips manually.";
	}

	@Override
	public TipImage getImage() {
		return null;
	}
}