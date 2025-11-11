package com.vogella.ide.editor.asciidoc;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

public class AsciidocMergeViewer extends TextMergeViewer {

    public AsciidocMergeViewer(Composite parent, int style, CompareConfiguration configuration) {
        super(parent, style, configuration);
        configuration.setProperty("org.eclipse.compare.INCOMING_COLOR", new RGB(0, 255, 0));
        configuration.setProperty("org.eclipse.compare.OUTGOING_COLOR", new RGB(255, 0, 0));
    }
}
