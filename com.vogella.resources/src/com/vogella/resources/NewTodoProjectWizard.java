package com.vogella.resources;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class NewTodoProjectWizard extends Wizard implements INewWizard {

    private static final String LINE_BREAK = System.getProperty("line.separator");

    private WizardNewProjectCreationPage projectCreationPage;

    public NewTodoProjectWizard() {
        setWindowTitle("New project for task");
        Bundle bundle = FrameworkUtil.getBundle(getClass());
        URL find = FileLocator.find(bundle, new Path("icons/vogella48.jpg"), null);
        setDefaultPageImageDescriptor(ImageDescriptor.createFromURL(find));
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
    }

    @Override
    public void addPages() {
        // Reuse the WizardNewProjectCreationPage from org.eclipse.ui.ide
        projectCreationPage = new WizardNewProjectCreationPage("New task project");
        projectCreationPage.setTitle("Create a tasks project");
        projectCreationPage.setDescription("Creates a general project with *.tasks files.");
        addPage(projectCreationPage);
    }

    @Override
    public boolean performFinish() {
        String projectName = projectCreationPage.getProjectName();
        Job projectCreationJob = Job.create("Creating new tasks project", monitor -> {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IWorkspaceRoot root = workspace.getRoot();
            IProject project = root.getProject(projectName);
            if(!project.exists()) {
                project.create(monitor);
                project.open(monitor);

                createTodoFile(project, "Training.tasks", "1", "", monitor);
                createTodoFile(project, "Training2.tasks", "2", "Training.tasks", monitor);
            }
        });
        projectCreationJob.schedule();

        return true;
    }

    private void createTodoFile(IContainer container, String fileName, String id, String dependent, IProgressMonitor monitor) throws CoreException {
        IFile todoFile = container.getFile(new Path(fileName));
        // Create some sample contents for the todo file
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ");
        sb.append(id);
        sb.append(LINE_BREAK);
        sb.append("Summary: Created by project task wizard");
        sb.append(LINE_BREAK);
        sb.append("Dependent: ");
        sb.append(dependent);
        InputStream inputStream = new ByteArrayInputStream(sb.toString().getBytes());
        // create automatically closes the stream. See JavaDoc
        todoFile.create(inputStream, IResource.NONE, monitor);
    }

}

