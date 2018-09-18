package com.vogella.resources.handlers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Random;

import javax.inject.Named;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

public class OpenLotsOfEditorsHandler {
	private final Random random = new Random();

	private String filePrefix = "tempFile";
	private String fileExtension = ".txt";

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell s) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IProject project = root.getProject("performancetest");
		try {
			if (!project.exists()) {
				project.create(new NullProgressMonitor());
			}
			if (!project.isOpen()) {
				project.open(null);
			}
			for (int i = 0; i < 50; i++) {
				String fileName = filePrefix + i + fileExtension;
				IFile file = project.getFile(fileName);
				if (!file.exists()) {
					String str = " ";
					InputStream in = new ByteArrayInputStream(str.getBytes());

					file.create(in, true, null);
				}
				IEditorInput editorInput = new FileEditorInput(project.getFile(fileName));
				activePage.openEditor(editorInput, "org.eclipse.ui.DefaultTextEditor");
			}

		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public static IFile createFile(String name, IProject proj) throws CoreException {
		IFile file = proj.getFile(name);
		if (!file.exists()) {
			String str = " ";
			InputStream in = new ByteArrayInputStream(str.getBytes());
			file.create(in, true, null);
		}
		return file;
	}

}