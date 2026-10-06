package com.vogella.ide.editor.bytecode;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;

/** Shows the bytecode of the selected or edited Java source or class file. */
public class ShowBytecodeHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IFile file = selectedFile(HandlerUtil.getActivePart(event), HandlerUtil.getCurrentSelection(event));
		if (file == null) {
			MessageDialog.openInformation(window.getShell(), "Show Bytecode",
					"Select a .java or .class file, or open a Java editor.");
			return null;
		}
		showBytecode(window, file);
		return null;
	}

	/** Builds the workspace, writes the .bytecode file and opens it. */
	public static void showBytecode(IWorkbenchWindow window, IFile file) {
		Job job = Job.create("Disassembling " + file.getName(), (IProgressMonitor monitor) -> {
			try {
				file.getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
				IFile bytecode = Bytecode.writeBytecodeFile(file, monitor);
				window.getShell().getDisplay().asyncExec(() -> open(window, bytecode));
				return Status.OK_STATUS;
			} catch (CoreException | IOException e) {
				return Status.error("Cannot show the bytecode of " + file.getName() + ": " + e.getMessage(), e);
			}
		});
		job.setUser(true);
		job.schedule();
	}

	private static void open(IWorkbenchWindow window, IFile file) {
		try {
			IDE.openEditor(window.getActivePage(), file);
		} catch (PartInitException e) {
			StatusManager.getManager().handle(e.getStatus(), StatusManager.SHOW);
		}
	}

	private static IFile selectedFile(IWorkbenchPart part, ISelection selection) {
		if (part instanceof IEditorPart editor && isJavaOrClassFile(Adapters.adapt(editor.getEditorInput(), IFile.class))) {
			return Adapters.adapt(editor.getEditorInput(), IFile.class);
		}
		if (selection instanceof IStructuredSelection structured
				&& Adapters.adapt(structured.getFirstElement(), IResource.class) instanceof IFile file
				&& isJavaOrClassFile(file)) {
			return file;
		}
		return null;
	}

	private static boolean isJavaOrClassFile(IFile file) {
		return file != null && ("java".equals(file.getFileExtension()) || "class".equals(file.getFileExtension()));
	}
}
