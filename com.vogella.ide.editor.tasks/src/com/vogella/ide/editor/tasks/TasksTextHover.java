package com.vogella.ide.editor.tasks;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

public class TasksTextHover implements ITextHover {

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		String contents= textViewer.getDocument().get();
		int offset= hoverRegion.getOffset();
		int startIndex= contents.indexOf("ID", offset);
		if (startIndex==-1) {
			return "";
		}
		IDocument document = textViewer.getDocument();
		try {
			// just extract the full line and show as hover
			int lineNumber = document.getLineOfOffset(offset);
			IRegion lineInformation = document.getLineInformation(lineNumber);
			int length = lineInformation.getLength();
			int lineOffSet = lineInformation.getOffset();
			return document.get(lineOffSet, length);
			
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		return "";
	}

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		 return new Region(offset, 0);
	}

}
