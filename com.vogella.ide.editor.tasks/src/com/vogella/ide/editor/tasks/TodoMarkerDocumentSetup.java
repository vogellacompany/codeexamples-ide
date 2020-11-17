package com.vogella.ide.editor.tasks;
import static com.vogella.ide.editor.tasks.Util.getActiveEditor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

public class TodoMarkerDocumentSetup implements IDocumentSetupParticipant {

	private static final String TODO_PROPERTY = "todoProperty";

	@Override
	public void setup(IDocument document) {
		document.addDocumentListener(new IDocumentListener() {

			private Job markerJob;

			@Override
			public void documentChanged(DocumentEvent event) {
				IEditorPart activeEditor = getActiveEditor();

				if (activeEditor != null) {
					IEditorInput editorInput = activeEditor.getEditorInput();
					IResource adapter = editorInput.getAdapter(IResource.class);
					if (markerJob != null) {
						markerJob.cancel();
					}
					markerJob = Job.create("Adding Marker", (ICoreRunnable) monitor -> createMarker(event, adapter));
					markerJob.setUser(false);
					markerJob.setPriority(Job.DECORATE);
					// set a delay before reacting to user action to handle continuous typing
					markerJob.schedule(500);
				}
			}


			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
				// not needed
			}
		});
	}
	
	private void createMarker(DocumentEvent event, IResource adapter) throws CoreException {
		String docText = event.getDocument().get();

		for (String todoProperty : TodoPropertiesContentAssistProcessor.PROPOSALS) {
			List<IMarker> markers = Arrays
					.asList(adapter.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE));
			Optional<IMarker> findAny = markers.stream()
					.filter(m -> todoProperty.equals(m.getAttribute(TODO_PROPERTY, ""))).findAny();
			if (docText.contains(todoProperty) && findAny.isPresent()) {
				findAny.get().delete();
			} else if (!docText.contains(todoProperty) && !findAny.isPresent()) {
				IMarker marker = adapter.createMarker(IMarker.PROBLEM);
				marker.setAttribute(IMarker.MESSAGE, todoProperty + " property is not set");
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
				marker.setAttribute(IMarker.LOCATION, "Missing line");
				marker.setAttribute(TODO_PROPERTY, todoProperty);
			}
		}
	}

}
