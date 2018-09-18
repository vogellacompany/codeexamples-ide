package com.vogella.ide.editor.companylink;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.swt.program.Program;

public class VogellaHyperlink implements IHyperlink {

	private final IRegion fUrlRegion;

	public VogellaHyperlink(IRegion urlRegion) {
		fUrlRegion = urlRegion;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		return fUrlRegion;
	}

	@Override
	public String getTypeLabel() {
		return null;
	}

	@Override
	public String getHyperlinkText() {
		return "Open vogella website";
	}

	@Override
	public void open() {
		Program.launch("http://www.vogella.com");
	}
}