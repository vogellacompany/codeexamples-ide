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

		String fullStatement = getFullStatementAtOffset(viewer, offset);

		// If "image::" is typed, suggest image files
		if (typedText.startsWith("image") || isCursorInImageSyntax(fullStatement) || isCursorBeforeImage(typedText)) {
			List<String> imageFiles = getImageFiles(typedText.substring(typedText.length()), viewer);

			if (isCursorInImageSyntax(fullStatement)) {

			}
			return imageFiles.stream().map(filePath -> new CompletionProposal("image::" + filePath + "[]", // The
																											// completion
																											// text to
																											// insert
					offset - typedText.length(), // The offset to replace
					typedText.length(), // The length of the typed text to be replaced
					("image::" + filePath + "[]").length() // The cursor position after the inserted text
			)).toArray(ICompletionProposal[]::new);
		}

		// If "include::" is typed, suggest .adoc files
		if (typedText.startsWith("include")) {
			List<String> includeFiles = getIncludeFiles(typedText.substring(typedText.length()));
			return includeFiles.stream()
					.map(filePath -> new CompletionProposal("include::" + filePath + "[]", offset - typedText.length(),
							typedText.length(), offset + "include::".length() + filePath.length() + 2)) // +2 for the
																										// "[]" brackets
					.toArray(ICompletionProposal[]::new);
		}

		// Filter proposals that match the typed text for other cases
		return getMatchingProposals(typedText, offset);
	}

	// Get the full statement at the cursor position
	private String getFullStatementAtOffset(ITextViewer viewer, int offset) {
		try {
			IDocument document = viewer.getDocument();
			int lineStartOffset = document.getLineOffset(document.getLineOfOffset(offset));
			int lineEndOffset = document.getLineOffset(document.getLineOfOffset(offset) + 1) - 1; // Get the end of the
																									// line
			return document.get(lineStartOffset, lineEndOffset - lineStartOffset + 1); // Include full line text
		} catch (BadLocationException e) {
			return "";
		}
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

	private List<String> getImageFiles(String typedText, ITextViewer viewer) {
		String directoryPath = getDirectoryPath();
		if (directoryPath == null) {
			return Collections.emptyList(); // Return empty list if directory is not found
		}

		File folder = new File(directoryPath + File.separator + "img");
		File[] files = folder
				.listFiles((dir, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));
		if (files == null)
			return Collections.emptyList();

		return Arrays.stream(files).map(File::getName).filter(fileName -> fileName.startsWith(typedText))
				.collect(Collectors.toList());
	}

	private boolean isCursorInImageSyntax(String typedText) {
		// Check if the cursor is inside 'image' keyword (but not after the '::')
		return typedText.startsWith("image") && !typedText.startsWith("image::");
	}

	private boolean isCursorBeforeImage(String typedText) {
		// Check if the cursor is before 'image::' and suggest completions
		return typedText.startsWith("image::");
	}

	// Method to get files for include::
	private List<String> getIncludeFiles(String typedText) {
		String directoryPath = getDirectoryPath();
		File folder = new File(directoryPath); // Or another path relative to the current file
		File[] files = folder.listFiles((dir, name) -> name.endsWith(".adoc") && name.startsWith(typedText));

		if (files == null) {
			return Collections.emptyList();
		}

		return Arrays.stream(files).map(File::getName).collect(Collectors.toList());
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
