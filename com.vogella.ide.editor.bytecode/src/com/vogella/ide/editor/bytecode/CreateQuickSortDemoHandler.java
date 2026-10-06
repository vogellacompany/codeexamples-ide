package com.vogella.ide.editor.bytecode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.FrameworkUtil;

/** Creates a Java project with a quicksort implementation and shows its bytecode. */
public class CreateQuickSortDemoHandler extends AbstractHandler {

	private static final String PROJECT_NAME = "bytecode-demo";
	private static final String JRE_CONTAINER = "org.eclipse.jdt.launching.JRE_CONTAINER";

	@Override
	public Object execute(ExecutionEvent event) {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		Job job = Job.create("Creating the QuickSort demo", (IProgressMonitor monitor) -> {
			try {
				ICompilationUnit unit = createQuickSort(monitor);
				ShowBytecodeHandler.showBytecode(window, (IFile) unit.getResource());
				return Status.OK_STATUS;
			} catch (CoreException | IOException e) {
				return Status.error("Cannot create the QuickSort demo: " + e.getMessage(), e);
			}
		});
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.setUser(true);
		job.schedule();
		return null;
	}

	private static ICompilationUnit createQuickSort(IProgressMonitor monitor) throws CoreException, IOException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		if (!project.exists()) {
			project.create(monitor);
		}
		project.open(monitor);
		IJavaProject javaProject = JavaCore.create(project);
		if (!project.hasNature(JavaCore.NATURE_ID)) {
			IProjectDescription description = project.getDescription();
			description.setNatureIds(new String[] { JavaCore.NATURE_ID });
			project.setDescription(description, monitor);
			IFolder src = project.getFolder("src");
			if (!src.exists()) {
				src.create(true, true, monitor);
			}
			javaProject.setRawClasspath(new IClasspathEntry[] { JavaCore.newSourceEntry(src.getFullPath()),
					JavaCore.newContainerEntry(IPath.fromOSString(JRE_CONTAINER)) }, project.getFullPath().append("bin"),
					monitor);
		}
		IPackageFragment sortPackage = javaProject.getPackageFragmentRoot(project.getFolder("src"))
				.createPackageFragment("com.example.sort", true, monitor);
		return sortPackage.createCompilationUnit("QuickSort.java", readSample(), true, monitor);
	}

	private static String readSample() throws IOException {
		URL sample = FrameworkUtil.getBundle(CreateQuickSortDemoHandler.class).getEntry("samples/QuickSort.java");
		try (InputStream in = sample.openStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
