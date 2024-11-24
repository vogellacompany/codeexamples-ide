package com.vogella.ide.editor.asciidoc;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class Util {

    private Util() {
        // only helper
    }
    
    public static boolean testRun;

    public static IEditorPart getActiveEditor() {
    	if (testRun) {
    		return null;
    	}
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
        if (null == activeWorkbenchWindow) {
            activeWorkbenchWindow = workbench.getWorkbenchWindows()[0];
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage == null) {
            return null;
        }
        return activePage.getActiveEditor();
    }
}