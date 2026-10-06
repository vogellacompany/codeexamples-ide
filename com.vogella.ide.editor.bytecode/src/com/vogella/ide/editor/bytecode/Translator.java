package com.vogella.ide.editor.bytecode;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MArea;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension5;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.ITextEditor;

/** Translates the bytecode of an editor with Claude and shows the result beside it. */
public final class Translator {

	private static final String GENERIC_EDITOR_ID = "org.eclipse.ui.genericeditor.GenericEditor";
	private static final String OUTPUT_FOLDER = "translated";
	private static final String EDITOR_STACK_TAG = "EditorStack";

	private static final Set<String> RUNNING = ConcurrentHashMap.newKeySet();

	private Translator() {
	}

	public static boolean isRunning(IFile bytecodeFile, Language language) {
		return RUNNING.contains(key(bytecodeFile, language));
	}

	/** Starts the translation in the background, the result opens beside the editor. */
	public static void translate(ITextEditor editor, Language language) {
		IFile bytecodeFile = Adapters.adapt(editor.getEditorInput(), IFile.class);
		if (bytecodeFile == null || !RUNNING.add(key(bytecodeFile, language))) {
			return;
		}
		IWorkbenchWindow window = editor.getSite().getWorkbenchWindow();
		String bytecode = editor.getDocumentProvider().getDocument(editor.getEditorInput()).get();
		Job job = Job.create("Translating " + bytecodeFile.getName() + " to " + language.name() + " with Claude",
				(IProgressMonitor monitor) -> {
					try {
						String code = ClaudeCli.translate(bytecode, language.name(), monitor);
						// a project level folder, so that a Java translation never overwrites the original source
						IFile target = Bytecode.writeProjectFile(bytecodeFile.getProject(), OUTPUT_FOLDER,
								Bytecode.baseName(bytecodeFile) + "." + language.extension(), code, monitor);
						window.getShell().getDisplay().asyncExec(() -> openBeside(editor, target));
						return Status.OK_STATUS;
					} catch (OperationCanceledException e) {
						return Status.CANCEL_STATUS;
					} catch (IOException | CoreException e) {
						// a system job reports errors only in the log
						StatusManager.getManager().handle(
								Status.error("Translation to " + language.name() + " failed: " + e.getMessage(), e),
								StatusManager.SHOW | StatusManager.LOG);
						return Status.OK_STATUS;
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return Status.CANCEL_STATUS;
					} finally {
						RUNNING.remove(key(bytecodeFile, language));
						window.getShell().getDisplay().asyncExec(() -> updateCodeMinings(editor));
					}
				});
		job.setSystem(true);
		job.schedule();
		updateCodeMinings(editor);
	}

	private static void updateCodeMinings(ITextEditor editor) {
		if (editor.getAdapter(ITextViewer.class) instanceof ISourceViewerExtension5 viewer) {
			viewer.updateCodeMinings();
		}
	}

	private static void openBeside(IEditorPart source, IFile file) {
		try {
			// the Generic Editor avoids launching an external program for unknown file types
			IEditorPart result = IDE.openEditor(source.getSite().getPage(), file, GENERIC_EDITOR_ID);
			moveBeside(source, result);
		} catch (PartInitException e) {
			StatusManager.getManager().handle(e.getStatus(), StatusManager.SHOW);
		}
	}

	/** Moves the result into an editor stack next to the source, so both stay visible. */
	private static void moveBeside(IEditorPart source, IEditorPart result) {
		MPart sourcePart = source.getSite().getService(MPart.class);
		MPart resultPart = result.getSite().getService(MPart.class);
		if (sourcePart == null || resultPart == null || sourcePart.getParent() != resultPart.getParent()
				|| !((Object) sourcePart.getParent() instanceof MPartStack sourceStack)) {
			return;
		}
		EModelService modelService = source.getSite().getService(EModelService.class);
		MUIElement area = sourceStack;
		while (area != null && !(area instanceof MArea)) {
			area = area.getParent();
		}
		if (area == null) {
			return;
		}
		MPartStack target = modelService.findElements(area, null, MPartStack.class, List.of(EDITOR_STACK_TAG))
				.stream()
				.filter(stack -> stack != sourceStack && stack.isToBeRendered())
				.findFirst()
				.orElse(null);
		if (target == null) {
			target = modelService.createModelElement(MPartStack.class);
			target.getTags().add(EDITOR_STACK_TAG);
			modelService.insert(target, (MPartSashContainerElement) sourceStack, EModelService.RIGHT_OF, 0.5f);
		}
		@SuppressWarnings("unchecked")
		var newParent = (MElementContainer<MUIElement>) (MElementContainer<?>) target;
		modelService.move(resultPart, newParent);
		source.getSite().getService(EPartService.class).activate(resultPart);
	}

	private static String key(IFile bytecodeFile, Language language) {
		return bytecodeFile.getFullPath() + "#" + language.name();
	}
}
