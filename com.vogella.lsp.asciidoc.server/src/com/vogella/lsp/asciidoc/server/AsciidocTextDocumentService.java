package com.vogella.lsp.asciidoc.server;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
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
	private final Map<String, AsciidocDocumentModel> docs = Collections.synchronizedMap(new HashMap<>());

	private final AsciidocLanguageServer languageServer;

	public AsciidocTextDocumentService(AsciidocLanguageServer languageServer) {
		this.languageServer = languageServer;
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		return CompletableFuture.supplyAsync(() -> {
			List<CompletionItem> completionItems = new ArrayList<>();
			String uri = position.getTextDocument().getUri();
			AsciidocDocumentModel model = docs.get(uri);

			if (model == null) {
				return Either.forLeft(Collections.emptyList());
			}

			int lineNum = position.getPosition().getLine();
			int charPos = position.getPosition().getCharacter();
			String lineContent = model.getLineContent(lineNum);
			if (lineContent == null)
				lineContent = "";

			String prefixLine = charPos <= lineContent.length() ? lineContent.substring(0, charPos) : lineContent;
			String suffixLine = charPos < lineContent.length() ? lineContent.substring(charPos) : "";

			// 1. Image completion
			// Matches 'image::' or 'image:' followed by an optional path and an optional opening bracket at the end
			Pattern imagePattern = Pattern.compile("image:[:]?([^\\[\\]\\s]*)(\\[)?$");
			Matcher imageMatcher = imagePattern.matcher(prefixLine);
			if (imageMatcher.find()) {
				String pathPrefix = imageMatcher.group(1);
				boolean hasOpeningInPrefix = imageMatcher.group(2) != null;
				int startChar = charPos - pathPrefix.length() - (hasOpeningInPrefix ? 1 : 0);

				// Determine how much of the suffix to replace
				int endChar = charPos;
				if (suffixLine.startsWith("[]")) {
					endChar += 2;
				} else if (suffixLine.startsWith("]")) {
					endChar += 1;
				} else if (hasOpeningInPrefix && suffixLine.startsWith("[")) {
					// This case is unlikely given the regex, but good for completeness
					endChar += 1;
				}

				List<String> images = scanForFiles(uri, "img", new String[] { ".png", ".jpg", ".jpeg", ".gif" });
				for (String img : images) {
					if (img.toLowerCase().startsWith(pathPrefix.toLowerCase())) {
						CompletionItem item = new CompletionItem();
						item.setLabel(img);
						item.setKind(CompletionItemKind.File);
						item.setDetail("Image file");

						TextEdit edit = new TextEdit();
						edit.setRange(new Range(new Position(lineNum, startChar), new Position(lineNum, endChar)));
						edit.setNewText(img + "[]");
						item.setTextEdit(Either.forLeft(edit));

						completionItems.add(item);
					}
				}
				return Either.forLeft(completionItems);
			}

			// 2. Include completion
			Pattern includePattern = Pattern.compile("include::([^\\[\\]\\s]*)(\\[)?$");
			Matcher includeMatcher = includePattern.matcher(prefixLine);
			if (includeMatcher.find()) {
				String pathPrefix = includeMatcher.group(1);
				boolean hasOpeningInPrefix = includeMatcher.group(2) != null;
				int startChar = charPos - pathPrefix.length() - (hasOpeningInPrefix ? 1 : 0);

				int endChar = charPos;
				if (suffixLine.startsWith("[]")) {
					endChar += 2;
				} else if (suffixLine.startsWith("]")) {
					endChar += 1;
				}

				List<String> files = scanForFiles(uri, ".", new String[] { ".adoc" });
				for (String file : files) {
					if (file.toLowerCase().startsWith(pathPrefix.toLowerCase())) {
						CompletionItem item = new CompletionItem();
						item.setLabel(file);
						item.setKind(CompletionItemKind.File);
						item.setDetail("Asciidoc file");

						TextEdit edit = new TextEdit();
						edit.setRange(new Range(new Position(lineNum, startChar), new Position(lineNum, endChar)));
						edit.setNewText(file + "[]");
						item.setTextEdit(Either.forLeft(edit));

						completionItems.add(item);
					}
				}
				return Either.forLeft(completionItems);
			}

			// 3. Default proposals
			completionItems.add(createCompletionItem("= Title", CompletionItemKind.Snippet, "= Title"));
			completionItems.add(createCompletionItem("== Subtitle", CompletionItemKind.Snippet, "== Subtitle"));
			completionItems.add(createCompletionItem("image::", CompletionItemKind.Snippet, "image::"));
			completionItems.add(createCompletionItem("include::", CompletionItemKind.Snippet, "include::"));
			
			CompletionItem sourceBlock = new CompletionItem();
			sourceBlock.setLabel("Source Code Block");
			sourceBlock.setKind(CompletionItemKind.Snippet);
			sourceBlock.setInsertText("[source, java]\n----\n\n----");
			completionItems.add(sourceBlock);

			return Either.forLeft(completionItems);
		});
	}

	private CompletionItem createCompletionItem(String label, CompletionItemKind kind, String insertText) {
		CompletionItem item = new CompletionItem();
		item.setLabel(label);
		item.setKind(kind);
		item.setInsertText(insertText);
		return item;
	}

	private List<String> scanForFiles(String documentUri, String subDir, String[] extensions) {
		List<String> fileNames = new ArrayList<>();
		try {
			URI uri = new URI(documentUri);
			File docFile = new File(uri);
			File parentDir = docFile.getParentFile();
			
			File targetDir = new File(parentDir, subDir);
			if (targetDir.exists() && targetDir.isDirectory()) {
				File[] files = targetDir.listFiles((dir, name) -> {
					for (String ext : extensions) {
						if (name.toLowerCase().endsWith(ext)) return true;
					}
					return false;
				});
				
				if (files != null) {
					for (File f : files) {
						fileNames.add(f.getName());
					}
				}
			}
		} catch (Exception e) {
			// Ignore errors
		}
		return fileNames;
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
			DocumentSymbolParams params) {
		return CompletableFuture.supplyAsync(() -> {
			// Create a list to hold the symbols
			List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();

			// Create a symbol for a class
			DocumentSymbol classSymbol = new DocumentSymbol();
			classSymbol.setName("MyClass");
			classSymbol.setKind(SymbolKind.Class);
			classSymbol.setRange(new Range(new Position(0, 0), new Position(0, 10)));
			classSymbol.setSelectionRange(new Range(new Position(0, 0), new Position(0, 10)));

			// Create a symbol for a method inside the class
			DocumentSymbol methodSymbol = new DocumentSymbol();
			methodSymbol.setName("myMethod");
			methodSymbol.setKind(SymbolKind.Method);
			methodSymbol.setRange(new Range(new Position(1, 0), new Position(1, 10)));
			methodSymbol.setSelectionRange(new Range(new Position(1, 0), new Position(1, 10)));

			// Add the method symbol as a child of the class symbol
			classSymbol.setChildren(List.of(methodSymbol));

			// Add the class symbol to the list of symbols
			symbols.add(Either.forRight(classSymbol));

			// Return the list of symbols
			return symbols;
		});
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		return CompletableFuture.supplyAsync(() -> {
			String uri = params.getTextDocument().getUri();
			AsciidocDocumentModel model = docs.get(uri);
			if (model == null)
				return null;

			int lineNum = params.getPosition().getLine();
			int charPos = params.getPosition().getCharacter();
			String lineContent = model.getLineContent(lineNum);
			if (lineContent == null)
				return null;

			// Check for image macro
			Pattern imagePattern = Pattern.compile("image:[:]?([^\\[\\]\\s]*)(\\[)?");
			Matcher matcher = imagePattern.matcher(lineContent);

			while (matcher.find()) {
				// Check if the cursor is within the match range
				if (charPos >= matcher.start() && charPos <= matcher.end()) {
					String imageName = matcher.group(1);
					if (!imageName.isEmpty()) {
						try {
							URI docUri = new URI(uri);
							File docFile = new File(docUri);
							File parentDir = docFile.getParentFile();

							// Try both 'img/' subdirectory and current directory
							File imgFile = new File(parentDir, "img/" + imageName);
							if (!imgFile.exists()) {
								imgFile = new File(parentDir, imageName);
							}

							if (imgFile.exists()) {
								String imgUri = imgFile.toURI().toString();
								// Render the actual image in Markdown
								// Some clients prefer a clean Markdown image syntax
								String content = String.format("![%s](%s)", imageName, imgUri);
								Hover hover = new Hover();
								hover.setContents(new MarkupContent(MarkupKind.MARKDOWN, content));
								return hover;
							}
						} catch (Exception e) {
							// Fallback to default
						}
					}
				}
			}

			// We hover only after the first line
			if (lineNum > 0) {

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
	public void didOpen(DidOpenTextDocumentParams params) {
		AsciidocDocumentModel model = new AsciidocDocumentModel(params.getTextDocument().getText());
		this.docs.put(params.getTextDocument().getUri(), model);
		CompletableFuture.runAsync(() -> languageServer.client
				.publishDiagnostics(new PublishDiagnosticsParams(params.getTextDocument().getUri(), validate(model))));

	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		AsciidocDocumentModel model = new AsciidocDocumentModel(params.getContentChanges().get(0).getText());
		this.docs.put(params.getTextDocument().getUri(), model);
		CompletableFuture.runAsync(() -> languageServer.client
				.publishDiagnostics(new PublishDiagnosticsParams(params.getTextDocument().getUri(), validate(model))));

	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		this.docs.remove(params.getTextDocument().getUri());
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
