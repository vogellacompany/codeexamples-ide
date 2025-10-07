package com.vogella.ide.editor.asciidoc;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.ITextViewer;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

public class AsciidocContentAssistProcessor implements IContentAssistProcessor {

	// Common AsciiDoc syntax elements for completion
	public static final List<String> PROPOSALS = Arrays.asList("= Title", // Header level 1
			"== Subtitle", // Header level 2
			"=== Subsubtitle", // Header level 3
			"* List item", // Unordered list
			"1. Ordered list item", // Ordered list
			"""
				[source, java]
				----
				
				----
					""", ":attribute:", // Attribute
			"image::[]", // Image
			"include::[]" // Included Files
	);

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		String typedText = getTypedTextAtOffset(viewer, offset);

		// If "image::" is typed, suggest image files
		if (typedText.startsWith("image::")) {
			String pathAfterImage = typedText.substring("image::".length());
			List<String> imageFiles = getImageFiles(pathAfterImage);
			return imageFiles.stream()
					.map(filePath -> new CompletionProposal("image::" + filePath + "[]", 
							offset - typedText.length(), 
							typedText.length(), 
							("image::" + filePath + "[]").length()))
					.toArray(ICompletionProposal[]::new);
		}

		// If "include::" is typed, suggest .adoc files
		if (typedText.startsWith("include::")) {
			String pathAfterInclude = typedText.substring("include::".length());
			List<String> includeFiles = getIncludeFiles(pathAfterInclude);
			return includeFiles.stream()
					.map(filePath -> new CompletionProposal("include::" + filePath + "[]", 
							offset - typedText.length(),
							typedText.length(), 
							("include::" + filePath + "[]").length()))
					.toArray(ICompletionProposal[]::new);
		}

		// Filter proposals that match the typed text for other cases
		return getMatchingProposals(typedText, offset);
	}

	private ICompletionProposal[] getMatchingProposals(String typedText, int offset) {
		// Match proposals that start with the typed text
		List<String> filteredProposals = PROPOSALS.stream()
				.filter(proposal -> proposal.toLowerCase().startsWith(typedText.toLowerCase()))
				.collect(Collectors.toList());

		return filteredProposals.stream().map(proposal -> new CompletionProposal(proposal, offset - typedText.length(),
				typedText.length(), proposal.length())).toArray(ICompletionProposal[]::new);
	}

	private String getTypedTextAtOffset(ITextViewer viewer, int offset) {
		try {
			IDocument document = viewer.getDocument();
			int lineStartOffset = document.getLineOffset(document.getLineOfOffset(offset));
			return document.get(lineStartOffset, offset - lineStartOffset);
		} catch (BadLocationException e) {
			return "";
		}
	}

	private List<String> getImageFiles(String pathAfterImage) {
		String directoryPath = getDirectoryPath();
		if (directoryPath == null) {
			return Collections.emptyList();
		}

		// Images are expected in an "img" subdirectory
		File folder = new File(directoryPath, "img");
		if (!folder.exists() || !folder.isDirectory()) {
			return Collections.emptyList();
		}

		File[] files = folder.listFiles((dir, name) -> 
			(name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg")) 
			&& name.startsWith(pathAfterImage)
		);
		
		if (files == null) {
			return Collections.emptyList();
		}

		return Arrays.stream(files).map(File::getName).collect(Collectors.toList());
	}

	// Method to get files for include::
	private List<String> getIncludeFiles(String pathAfterInclude) {
		String directoryPath = getDirectoryPath();
		if (directoryPath == null) {
			return Collections.emptyList();
		}
		
		// Parse the path to extract directory and filename prefix
		String targetDirectory = directoryPath;
		String filePrefix = pathAfterInclude;
		
		// Handle paths with directory separators
		if (pathAfterInclude.contains("/") || pathAfterInclude.contains("\\")) {
			int lastSeparatorIndex = Math.max(
				pathAfterInclude.lastIndexOf("/"),
				pathAfterInclude.lastIndexOf("\\")
			);
			
			String relativePath = pathAfterInclude.substring(0, lastSeparatorIndex);
			filePrefix = pathAfterInclude.substring(lastSeparatorIndex + 1);
			
			// Build the target directory path
			targetDirectory = new File(directoryPath, relativePath).getAbsolutePath();
		}
		
		File folder = new File(targetDirectory);
		if (!folder.exists() || !folder.isDirectory()) {
			return Collections.emptyList();
		}
		
		// Filter files by extension and prefix
		final String finalPrefix = filePrefix;
		File[] files = folder.listFiles((dir, name) -> 
			name.endsWith(".adoc") && name.startsWith(finalPrefix)
		);
		
		if (files == null) {
			return Collections.emptyList();
		}
		
		// Build the full path for each file (including the directory part)
		String pathPrefix = pathAfterInclude.substring(0, pathAfterInclude.length() - filePrefix.length());
		return Arrays.stream(files)
			.map(file -> pathPrefix + file.getName())
			.collect(Collectors.toList());
	}

	private String getDirectoryPath() {

		IEditorPart activeEditor = Util.getActiveEditor();
		// Get the editor file's path (assuming the document is backed by a file)

		if (activeEditor == null) {
			return null;
		}
		IEditorInput editorInput = activeEditor.getEditorInput();

		if (!(editorInput instanceof IFileEditorInput)) {
			return null; // Return null if the editor is not backed by a file
		}

		IFile file = ((IFileEditorInput) editorInput).getFile();
		return file.getParent().getLocation().toOSString(); // Return the directory path of the file
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[0];
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}

}
