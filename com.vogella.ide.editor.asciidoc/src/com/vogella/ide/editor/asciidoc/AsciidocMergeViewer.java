package com.vogella.ide.editor.asciidoc;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.swt.widgets.Composite;

public class AsciidocMergeViewer extends TextMergeViewer {

    public AsciidocMergeViewer(Composite parent, int style, CompareConfiguration configuration) {
        super(parent, style, configuration);
    }
}
