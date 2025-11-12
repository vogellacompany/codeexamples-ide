package com.vogella.lsp.asciidoc.server;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

public class AsciidocTextDocumentService implements TextDocumentService {
	private static final Logger LOGGER = Logger.getLogger(AsciidocTextDocumentService.class.getName());

	// Regex patterns for link detection (compiled once for performance)
	// Note: Brackets are optional to handle partial/malformed syntax gracefully
	private static final Pattern INCLUDE_PATTERN = Pattern.compile("include::([^\\[\\s]+)(?:\\[.*?\\])?");
	private static final Pattern IMAGE_PATTERN = Pattern.compile("image::([^\\[\\s]+)(?:\\[.*?\\])?");
	private static final Pattern LINK_PATTERN = Pattern.compile("link:([^\\[\\s]+)(?:\\[.*?\\])?");

	// Default directory for images (can be configured if needed)
	private static final String DEFAULT_IMAGE_DIR = "img/";

	private final Map<String, AsciidocDocumentModel> docs = Collections.synchronizedMap(new HashMap<>());

	// Cache for document links to avoid repeated file I/O operations
	private final Map<String, List<DocumentLink>> linkCache = Collections.synchronizedMap(new HashMap<>());

	private final AsciidocLanguageServer languageServer;

	public AsciidocTextDocumentService(AsciidocLanguageServer languageServer) {
		this.languageServer = languageServer;
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		return CompletableFuture.supplyAsync(() -> {
			// Example: Provide completions for AsciiDoc elements

			CompletionItem item1 = new CompletionItem();
			item1.setLabel("image::");

			CompletionItem item2 = new CompletionItem();
			item2.setLabel("include::");
			List<CompletionItem> completionItems = List.of(item1, item2);

			return Either.forLeft(completionItems);
		});
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
			DocumentSymbolParams params) {
		return CompletableFuture.supplyAsync(() -> {
			String uri = params.getTextDocument().getUri();
			AsciidocDocumentModel model = docs.get(uri);

			if (model == null) {
				return Collections.emptyList();
			}

			List<String> lines = model.getLines();
			List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();
			List<DocumentSymbol> headerStack = new ArrayList<>();

			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				String trimmedLine = line.trim();
				DocumentSymbol symbol = null;

				// Parse headers (= Title, == Section, === Subsection, etc.)
				// Note: In AsciiDoc, ==== is a delimiter (not a header), so we exclude it
				if (line.startsWith("=") && !line.trim().equals("====")) {
					int level = 0;
					while (level < line.length() && line.charAt(level) == '=') {
						level++;
					}

					if (level < line.length() && line.charAt(level) == ' ') {
						String title = line.substring(level).trim();
						symbol = createSymbol(title, SymbolKind.String, i, line, level);

						// Build hierarchy based on header level
						while (!headerStack.isEmpty() && getHeaderLevel(headerStack.get(headerStack.size() - 1)) >= level) {
							headerStack.remove(headerStack.size() - 1);
						}

						if (headerStack.isEmpty()) {
							symbols.add(Either.forRight(symbol));
						} else {
							DocumentSymbol parent = headerStack.get(headerStack.size() - 1);
							if (parent.getChildren() == null) {
								parent.setChildren(new ArrayList<>());
							}
							parent.getChildren().add(symbol);
						}

						headerStack.add(symbol);
					}
				}
				// Parse include statements (must be at start of line after trimming)
				else if (trimmedLine.startsWith("include::")) {
					int startIdx = trimmedLine.indexOf("include::");
					int endIdx = trimmedLine.indexOf("[", startIdx);
					if (endIdx != -1) {
						String includePath = trimmedLine.substring(startIdx + 9, endIdx);
						symbol = createSymbol("include: " + includePath, SymbolKind.File, i, line, 0);
						addSymbolToHierarchy(symbol, headerStack, symbols);
					}
				}
				// Parse image references (must be at start of line after trimming)
				else if (trimmedLine.startsWith("image::")) {
					int startIdx = trimmedLine.indexOf("image::");
					int endIdx = trimmedLine.indexOf("[", startIdx);
					if (endIdx != -1) {
						String imagePath = trimmedLine.substring(startIdx + 7, endIdx);
						symbol = createSymbol("image: " + imagePath, SymbolKind.File, i, line, 0);
						addSymbolToHierarchy(symbol, headerStack, symbols);
					}
				}
				// Parse source code blocks
				else if (trimmedLine.startsWith("[source")) {
					int commaIdx = trimmedLine.indexOf(",");
					int bracketIdx = trimmedLine.indexOf("]");
					String language = "";
					if (commaIdx != -1 && bracketIdx != -1 && commaIdx < bracketIdx) {
						// Extract only the first parameter (language), not additional attributes
						String afterComma = trimmedLine.substring(commaIdx + 1, bracketIdx).trim();
						int nextComma = afterComma.indexOf(",");
						language = nextComma != -1 ? afterComma.substring(0, nextComma).trim() : afterComma;
					}
					String label = language.isEmpty() ? "source block" : "source: " + language;
					symbol = createSymbol(label, SymbolKind.Module, i, line, 0);
					addSymbolToHierarchy(symbol, headerStack, symbols);
				}
				// Parse tables
				else if (trimmedLine.equals("|===")) {
					symbol = createSymbol("table", SymbolKind.Array, i, line, 0);
					addSymbolToHierarchy(symbol, headerStack, symbols);
				}
			}

			return symbols;
		});
	}

	private DocumentSymbol createSymbol(String name, SymbolKind kind, int lineNumber, String lineText, int level) {
		DocumentSymbol symbol = new DocumentSymbol();
		symbol.setName(name);
		symbol.setKind(kind);

		// Store the header level in the detail field for hierarchy management
		if (level > 0) {
			symbol.setDetail("level:" + level);
		}

		Position start = new Position(lineNumber, 0);
		Position end = new Position(lineNumber, lineText.length());
		Range range = new Range(start, end);

		symbol.setRange(range);
		symbol.setSelectionRange(range);

		return symbol;
	}

	private void addSymbolToHierarchy(DocumentSymbol symbol, List<DocumentSymbol> headerStack,
			List<Either<SymbolInformation, DocumentSymbol>> symbols) {
		if (headerStack.isEmpty()) {
			symbols.add(Either.forRight(symbol));
		} else {
			DocumentSymbol parent = headerStack.get(headerStack.size() - 1);
			if (parent.getChildren() == null) {
				parent.setChildren(new ArrayList<>());
			}
			parent.getChildren().add(symbol);
		}
	}

	private int getHeaderLevel(DocumentSymbol symbol) {
		String detail = symbol.getDetail();
		if (detail != null && detail.startsWith("level:")) {
			try {
				return Integer.parseInt(detail.substring(6));
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		return CompletableFuture.supplyAsync(() -> {
			// Get the position where the hover request was made
			Position position = params.getPosition();
			// get file if necessary
//			String uri = params.getTextDocument().getUri();

			// We hover only after the first line
			if (position.getLine() > 0) {

				String content = """
						![Info Icon]

						**Important AsciiDoc Elements:**

						* `image::` - Defines an image element in AsciiDoc files.
						* `include::` - Includes other AsciiDoc files into the current one.

						**Usage Example:**
						```asciidoc
						image::path/to/image.png[]
						include::example.adoc[]
						```
						""";

				// Create the Hover object with content in markdown format
				Hover hover = new Hover();
				hover.setContents(new MarkupContent(MarkupKind.MARKDOWN, content));
				return hover;
			}

			// If no specific syntax is matched, return null or empty hover
			return null;
		});
	}
	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
			DefinitionParams params) {

		// Get the document URI and retrieve the model
		AsciidocDocumentModel model = this.docs.get(params.getTextDocument().getUri());
		if (model == null) {
			return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
		}

		// Get the line where the cursor is located
		int line = params.getPosition().getLine();
		int character = params.getPosition().getCharacter();

		// Retrieve the content of the line
		String lineContent = model.getLineContent(line);
		if (lineContent == null) {
			return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
		}

		// Find the word under the cursor. You can extract a word by splitting the line
		// based on spaces or punctuation.
		String wordUnderCursor = getWordAtPosition(lineContent, character);

		// Now you can resolve this word and create locations for the definition.
		// This logic depends on your specific requirements (e.g., file-based links,
		// symbol resolution).
		List<Location> locations = findDefinitionLocations(wordUnderCursor);

		return CompletableFuture.completedFuture(Either.forLeft(locations));
	}


	/**
	 * Utility method to find the word under the cursor in a given line of text.
	 */
	private String getWordAtPosition(String lineContent, int character) {
		// Define word boundaries (spaces or punctuation) to split the line into words.
		// This example assumes simple word boundaries.
		int start = character;
		int end = character;

		// Find the start of the word (left of the cursor)
		while (start > 0 && Character.isLetterOrDigit(lineContent.charAt(start - 1))) {
			start--;
		}

		// Find the end of the word (right of the cursor)
		while (end < lineContent.length() && Character.isLetterOrDigit(lineContent.charAt(end))) {
			end++;
		}

		// Extract the word
		return lineContent.substring(start, end);
	}

	/**
	 * Resolves the definition location(s) for a given word. This is a stub method,
	 * and you need to implement your own resolution logic.
	 */
	private List<Location> findDefinitionLocations(String word) {
		// This logic would vary based on how you resolve definitions.
		// For example, if the word corresponds to a file or symbol in your system,
		// you would look it up and return a list of Location objects.

		List<Location> locations = new ArrayList<>();
		// Example: You could create a location based on a predefined definition file or
		// symbol.
		Location location = new Location();
		location.setUri("file:///path/to/definitionFile");
		location.setRange(new Range(new Position(5, 0), new Position(5, 10))); // Example range for the definition
		locations.add(location);

		return locations;
	}

	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		return null;
	}


	@Override
	public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
		List<Either<Command, CodeAction>> actions = new ArrayList<>();

		// Check the diagnostics for the current document
		for (Diagnostic diagnostic : params.getContext().getDiagnostics()) {
			if ("placeholder.text.issue".equals(diagnostic.getCode().getLeft())) {
				// Create a text edit for replacing the placeholder
				TextEdit edit = new TextEdit();
				edit.setRange(diagnostic.getRange());
				edit.setNewText("replacement_text");

				// Create a workspace edit
				WorkspaceEdit workspaceEdit = new WorkspaceEdit();
				workspaceEdit.setChanges(Collections.singletonMap(params.getTextDocument().getUri(), List.of(edit)));

				// Create the code action
				CodeAction codeAction = new CodeAction("Replace placeholder with 'replacement_text'");
				codeAction.setKind(CodeActionKind.QuickFix);
				codeAction.setEdit(workspaceEdit);

				// Add to the actions list
				actions.add(Either.forRight(codeAction));
			}
		}

		// Return the actions as a CompletableFuture
		return CompletableFuture.completedFuture(actions);
	}

	@Override
	public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
		return CompletableFuture.supplyAsync(() -> {
			// Retrieve the document text from your model
			String uri = params.getTextDocument().getUri();
			AsciidocDocumentModel model = docs.get(uri);
			if (model == null) {
				return Collections.emptyList();
			}

			List<CodeLens> codeLenses = new ArrayList<>();
			List<String> lines = model.getLines();

			// Scan for "TODO" comments
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				int todoIndex = line.indexOf("TODO");
				if (todoIndex != -1) {
					// Define the range for the TODO
					Range range = new Range(new Position(i, todoIndex), new Position(i, todoIndex + "TODO".length()));

					// Create a CodeLens with a command
					Command command = new Command("Resolve TODO", "example.resolveTodo",
							Collections.singletonList("Resolve the TODO at line " + (i + 1)));

					CodeLens codeLens = new CodeLens(range, command, null);
					codeLenses.add(codeLens);
				}
			}

			return codeLenses;
		});
	}

	@Override
	public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
		return null;
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		return null;
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
		return null;
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
		return null;
	}

	@Override
	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		return null;
	}

	@Override
	public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
		return CompletableFuture.supplyAsync(() -> {
			String uri = params.getTextDocument().getUri();

			// Check cache first to avoid repeated file I/O
			List<DocumentLink> cachedLinks = linkCache.get(uri);
			if (cachedLinks != null) {
				return new ArrayList<>(cachedLinks); // Return copy to prevent modification
			}

			List<DocumentLink> links = new ArrayList<>();
			AsciidocDocumentModel model = docs.get(uri);

			if (model == null) {
				return links;
			}

			try {
				// Get the document's parent directory
				Path documentPath = Paths.get(new URI(uri));
				Path parentDir = documentPath.getParent();

				// Defensive check: parent can be null for root paths
				if (parentDir == null) {
					LOGGER.log(Level.WARNING, "Document has no parent directory: " + uri);
					return links;
				}

				List<String> lines = model.getLines();

				for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
					String line = lines.get(lineNum);

					// Detect include links
					Matcher includeMatcher = INCLUDE_PATTERN.matcher(line);
					while (includeMatcher.find()) {
						String targetPath = includeMatcher.group(1).trim();
						DocumentLink link = createFileDocumentLink(parentDir, targetPath, lineNum,
								includeMatcher.start(1), includeMatcher.end(1));
						if (link != null) {
							links.add(link);
						}
					}

					// Detect image links
					Matcher imageMatcher = IMAGE_PATTERN.matcher(line);
					while (imageMatcher.find()) {
						String targetPath = imageMatcher.group(1).trim();
						// Images are typically in img/ subdirectory if it's a simple filename
						String imagePath = targetPath;
						try {
							// Use getNameCount() to reliably detect simple filenames vs paths
							if (Paths.get(targetPath).getNameCount() == 1) {
								imagePath = DEFAULT_IMAGE_DIR + targetPath;
							}
						} catch (InvalidPathException e) {
							// If path is invalid, use as-is and let createFileDocumentLink handle it
						}
						DocumentLink link = createFileDocumentLink(parentDir, imagePath, lineNum,
								imageMatcher.start(1), imageMatcher.end(1));
						if (link != null) {
							links.add(link);
						}
					}

					// Detect link: references
					Matcher linkMatcher = LINK_PATTERN.matcher(line);
					while (linkMatcher.find()) {
						String target = linkMatcher.group(1).trim();

						// Check if it's an external URL
						if (target.startsWith("http://") || target.startsWith("https://")) {
							// Create link for external URL
							DocumentLink link = createLinkWithRange(lineNum, linkMatcher.start(1),
									linkMatcher.end(1), target);
							links.add(link);
						} else {
							// Internal file link (supports all relative paths: ./, ../, or simple filenames)
							DocumentLink link = createFileDocumentLink(parentDir, target, lineNum,
									linkMatcher.start(1), linkMatcher.end(1));
							if (link != null) {
								links.add(link);
							}
						}
					}
				}
			} catch (URISyntaxException e) {
				LOGGER.log(Level.WARNING, "Invalid document URI: " + uri, e);
			} catch (InvalidPathException e) {
				LOGGER.log(Level.WARNING, "Invalid path in document: " + uri, e);
			}

			// Cache the computed links to improve performance on subsequent requests
			linkCache.put(uri, new ArrayList<>(links));

			return links;
		});
	}

	/**
	 * Helper method to create a DocumentLink with a specific range and target URI.
	 * This unifies link creation logic for both external URLs and file paths.
	 */
	private DocumentLink createLinkWithRange(int lineNum, int startChar, int endChar, String targetUri) {
		DocumentLink link = new DocumentLink();
		Range range = new Range(
				new Position(lineNum, startChar),
				new Position(lineNum, endChar)
		);
		link.setRange(range);
		link.setTarget(targetUri);
		return link;
	}

	/**
	 * Helper method to create a DocumentLink for a file path.
	 * Resolves the path relative to the parent directory and validates file existence.
	 */
	private DocumentLink createFileDocumentLink(Path parentDir, String targetPath, int lineNum,
			int startChar, int endChar) {
		try {
			// Normalize path separators and resolve relative path
			String normalizedPath = targetPath.replace("\\", "/");
			Path resolvedPath = parentDir.resolve(normalizedPath).normalize();

			// Check if the file exists
			if (Files.exists(resolvedPath)) {
				return createLinkWithRange(lineNum, startChar, endChar, resolvedPath.toUri().toString());
			}
		} catch (InvalidPathException e) {
			LOGGER.log(Level.FINE, "Invalid path: " + targetPath, e);
		} catch (IOException e) {
			LOGGER.log(Level.FINE, "Error checking file existence: " + targetPath, e);
		}
		return null;
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		AsciidocDocumentModel model = new AsciidocDocumentModel(params.getTextDocument().getText());
		this.docs.put(params.getTextDocument().getUri(), model);
		CompletableFuture.runAsync(() -> languageServer.client
				.publishDiagnostics(new PublishDiagnosticsParams(params.getTextDocument().getUri(), validate(model))));

	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		String uri = params.getTextDocument().getUri();
		AsciidocDocumentModel model = new AsciidocDocumentModel(params.getContentChanges().get(0).getText());
		this.docs.put(uri, model);
		// Invalidate link cache since document content changed
		linkCache.remove(uri);
		CompletableFuture.runAsync(() -> languageServer.client
				.publishDiagnostics(new PublishDiagnosticsParams(uri, validate(model))));

	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		String uri = params.getTextDocument().getUri();
		this.docs.remove(uri);
		// Clean up link cache when document is closed
		linkCache.remove(uri);
	}
	
	@Override
	public void didSave(DidSaveTextDocumentParams params) {
	}

	private List<Diagnostic> validate(AsciidocDocumentModel model) {
		List<Diagnostic> diagnostics = new ArrayList<>();

		// Simulate finding a placeholder issue
		for (int i = 0; i < model.getResolvedLines().size(); i++) {
			String line = model.getResolvedLines().get(i).text;
			int index = line.indexOf("PLACEHOLDER_TEXT");
			if (index != -1) {
				// Create a diagnostic for the placeholder text issue
				Diagnostic diagnostic = new Diagnostic();
				diagnostic.setSeverity(DiagnosticSeverity.Warning);
				diagnostic.setMessage("Found placeholder text that should be replaced.");
				diagnostic.setCode("placeholder.text.issue");
				diagnostic.setRange(
						new Range(new Position(i, index), new Position(i, index + "PLACEHOLDER_TEXT".length())));
				diagnostics.add(diagnostic);
			}
		}

		return diagnostics;
	}

}
