package com.vogella.ide.editor.companylink;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;

public class VogellaHyperlinkDetector extends AbstractHyperlinkDetector {

	public VogellaHyperlinkDetector() {
	}

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {

		IDocument document = textViewer.getDocument();
		int offset = region.getOffset();

		// extract relevant characters
		IRegion lineRegion;
		String candidate;
		try {
			lineRegion = document.getLineInformationOfOffset(offset);
			candidate = document.get(lineRegion.getOffset(), lineRegion.getLength());
		} catch (BadLocationException ex) {
			return null;
		}

		// look for keyword
		int index = candidate.indexOf("vogella");
		if (index != -1) {

			// detect region containing keyword
			IRegion targetRegion = new Region(lineRegion.getOffset() + index, "vogella".length());
			if ((targetRegion.getOffset() <= offset)
					&& ((targetRegion.getOffset() + targetRegion.getLength()) > offset))
				// create link
				return new IHyperlink[] { new VogellaHyperlink(targetRegion) };
		}

		return null;
	}

}
