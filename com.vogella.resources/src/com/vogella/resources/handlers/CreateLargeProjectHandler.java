package com.vogella.resources.handlers;

import java.io.ByteArrayInputStream;
import java.util.Random;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

public class CreateLargeProjectHandler {
	private final Random random = new Random();

	private static final String CHARSFORCREATION = "abcdefghijklmnopqrstuvwxyz";

	@Execute
	public void execute(IWorkbenchPage page) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject project = root.getProject("performancetest");
		try {
			project.create(new NullProgressMonitor());
			project.open(null);
			for (int i = 0; i < 30; i++) {
				IFolder folder = project.getFolder("test" + i);
				folder.create(true, true, null);
				for (int j = 0; j < 30; j++) {
					IFile file = folder.getFile(createString(10));
					file.create(new ByteArrayInputStream(createBytes(5000)), IResource.NONE, null);
					IFileStore fileStore = EFS.getLocalFileSystem().getStore(file.getFullPath());
					if (!fileStore.fetchInfo().isDirectory()) {
						try {
							IDE.openEditorOnFileStore(page, fileStore);
						} catch (PartInitException e) {
							/* some code */
						}
					}
				}
			}
		} catch (CoreException e) {
			// nothing to do
		}
	}

	private byte[] createBytes(int length) {
		byte[] bytes = new byte[length];
		random.nextBytes(bytes);
		return bytes;
	}

	private String createString(int length) {
		StringBuilder buf = new StringBuilder(length);
		// fill the string with random characters up to the desired length
		for (int i = 0; i < length; i++) {
			buf.append(CHARSFORCREATION.charAt(random.nextInt(CHARSFORCREATION.length())));
		}
		return buf.toString();
	}
		
}