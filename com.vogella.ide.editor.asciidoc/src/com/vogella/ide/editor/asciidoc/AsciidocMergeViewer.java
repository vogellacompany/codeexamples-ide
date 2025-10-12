package com.vogella.ide.editor.asciidoc;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.hyperlink.MultipleHyperlinkPresenter;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

/**
 * A merge viewer for Asciidoc files that supports hyperlink detection
 * for include statements in compare mode.
 */
public class AsciidocMergeViewer extends TextMergeViewer {

    /** RGB color for hyperlinks (blue) */
    private static final RGB HYPERLINK_COLOR = new RGB(0, 0, 255);

    public AsciidocMergeViewer(Composite parent, int style, CompareConfiguration configuration) {
        super(parent, style, configuration);
    }

    @Override
    protected void configureTextViewer(TextViewer textViewer) {
        super.configureTextViewer(textViewer);
        
        if (textViewer instanceof SourceViewer) {
            SourceViewer sourceViewer = (SourceViewer) textViewer;
            
            // Install hyperlink detectors for include, image, and link statements
            IHyperlinkDetector[] hyperlinkDetectors = new IHyperlinkDetector[] {
                new IncludeHyperlinkDetector(),
                new ImageHyperlinkDetector(),
                new LinkHyperlinkDetector()
            };
            
            sourceViewer.setHyperlinkDetectors(hyperlinkDetectors, IDocument.DEFAULT_CONTENT_TYPE);
            
            // Install hyperlink presenter with blue color
            IHyperlinkPresenter hyperlinkPresenter = new MultipleHyperlinkPresenter(HYPERLINK_COLOR);
            sourceViewer.setHyperlinkPresenter(hyperlinkPresenter);
        }
    }
}
