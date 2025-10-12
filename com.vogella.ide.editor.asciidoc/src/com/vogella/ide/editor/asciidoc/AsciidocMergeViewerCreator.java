package com.vogella.ide.editor.asciidoc;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IViewerCreator;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * Creator for Asciidoc merge viewer that supports hyperlinks and syntax highlighting
 * in Eclipse compare mode.
 */
public class AsciidocMergeViewerCreator implements IViewerCreator {

    @Override
    public Viewer createViewer(Composite parent, CompareConfiguration config) {
        return new AsciidocMergeViewer(parent, SWT.NONE, config);
    }
}
