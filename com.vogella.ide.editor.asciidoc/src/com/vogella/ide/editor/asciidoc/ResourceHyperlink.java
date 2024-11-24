package com.vogella.ide.editor.asciidoc;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

public class ResourceHyperlink implements IHyperlink {

    private IRegion region;
    private String hyperlinkText;
    private IFile resource;

    public ResourceHyperlink(IRegion region, String hyperlinkText, IFile resource) {
        this.region = region;
        this.hyperlinkText = hyperlinkText;
        this.resource = resource;
    }

    @Override
    public IRegion getHyperlinkRegion() {
        return region;
    }

    @Override
    public String getTypeLabel() {
        return null;
    }

    @Override
    public String getHyperlinkText() {
        return hyperlinkText;
    }

    @Override
    public void open() {
        IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            IDE.openEditor(activePage, resource);
        } catch (PartInitException e) {
            e.printStackTrace();
        }
    }
}