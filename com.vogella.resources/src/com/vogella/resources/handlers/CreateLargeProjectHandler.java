package com.vogella.resources.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
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
		IProject project = workspace.getRoot().getProject("performancetest");
		List<IFile> files = new ArrayList<>();
		try {
			// batch all resource changes into a single resource change event
			workspace.run(monitor -> {
				project.create(monitor);
				project.open(monitor);
				for (int i = 0; i < 30; i++) {
					IFolder folder = project.getFolder("test" + i);
					folder.create(true, true, monitor);
					for (int j = 0; j < 30; j++) {
						IFile file = folder.getFile(createString(10));
						file.create(createBytes(5000), IResource.NONE, monitor);
						files.add(file);
					}
				}
			}, null);
		} catch (CoreException e) {
			ILog.get().error("Could not create the test project", e);
			return;
		}
		for (IFile file : files) {
			try {
				IDE.openEditor(page, file);
			} catch (PartInitException e) {
				ILog.get().error("Could not open an editor for " + file.getName(), e);
			}
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
