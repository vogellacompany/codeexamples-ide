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

			// 2. Include completion with smart path support
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

				// Parse path into directory and filename parts
				String dirPart = "";
				String filePrefix = pathPrefix;
				int lastSlash = pathPrefix.lastIndexOf('/');
				if (lastSlash >= 0) {
					dirPart = pathPrefix.substring(0, lastSlash + 1);
					filePrefix = pathPrefix.substring(lastSlash + 1);
				}

				// Get completions for the current path segment
				List<CompletionItem> pathCompletions = getPathCompletions(uri, dirPart, filePrefix, lineNum, startChar, endChar);
				completionItems.addAll(pathCompletions);

				return Either.forLeft(completionItems);
			}

			// 3. Default proposals
			// Header completions only at the beginning of the line
			if (prefixLine.trim().isEmpty()) {
				completionItems.add(createCompletionItem("= Title", CompletionItemKind.Snippet, "= Title"));
				completionItems.add(createCompletionItem("== Subtitle", CompletionItemKind.Snippet, "== Subtitle"));
			}

			// Other completions available everywhere
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

	/**
	 * Get path-aware completions for include directives.
	 * Supports directory traversal and completes the current path segment.
	 * Results are sorted: directories first, then files, both alphabetically.
	 */
	private List<CompletionItem> getPathCompletions(String documentUri, String dirPart, String filePrefix,
			int lineNum, int startChar, int endChar) {
		List<CompletionItem> completions = new ArrayList<>();
		try {
			URI uri = new URI(documentUri);
			File docFile = new File(uri);
			File parentDir = docFile.getParentFile();

			// Resolve the target directory relative to the current document
			File targetDir = new File(parentDir, dirPart);
			if (!targetDir.exists() || !targetDir.isDirectory()) {
				return completions;
			}

			File[] entries = targetDir.listFiles();
			if (entries == null) {
				return completions;
			}

			for (File entry : entries) {
				String name = entry.getName();

				// Skip hidden files and the current document
				if (name.startsWith(".")) {
					continue;
				}

				// Check if name matches the prefix (case-insensitive)
				if (!name.toLowerCase().startsWith(filePrefix.toLowerCase())) {
					continue;
				}

				if (entry.isDirectory()) {
					// Add directory completion
					CompletionItem item = new CompletionItem();
					item.setLabel(name + "/");
					item.setKind(CompletionItemKind.Folder);
					item.setDetail("Directory");
					item.setSortText("0_" + name.toLowerCase()); // Directories first

					TextEdit edit = new TextEdit();
					edit.setRange(new Range(new Position(lineNum, startChar), new Position(lineNum, endChar)));
					edit.setNewText(dirPart + name + "/");
					item.setTextEdit(Either.forLeft(edit));

					completions.add(item);
				} else if (name.endsWith(".adoc")) {
					// Add file completion
					CompletionItem item = new CompletionItem();
					item.setLabel(name);
					item.setKind(CompletionItemKind.File);
					item.setDetail("Asciidoc file");
					item.setSortText("1_" + name.toLowerCase()); // Files after directories

					TextEdit edit = new TextEdit();
					edit.setRange(new Range(new Position(lineNum, startChar), new Position(lineNum, endChar)));
					edit.setNewText(dirPart + name + "[]");
					item.setTextEdit(Either.forLeft(edit));

					completions.add(item);
				}
			}
		} catch (Exception e) {
			// Ignore errors
		}
		return completions;
	}

	@Override
	public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
		return CompletableFuture.supplyAsync(() -> {
			String uri = params.getTextDocument().getUri();
			AsciidocDocumentModel model = docs.get(uri);
			if (model == null) {
				return Collections.emptyList();
			}

			List<DocumentLink> links = new ArrayList<>();
			List<String> lines = model.getLines();

			for (int i = 0; i < lines.size(); i++) {
				collectLinks(uri, lines.get(i), i, links);
			}

			return links;
		});
	}

	private void collectLinks(String baseUri, String lineContent, int lineIndex, List<DocumentLink> links) {
		// Pattern for include::path[...] and image::path[...]
		Pattern pattern = Pattern.compile("(include|image):[:]?([^\\s\\[\\]]+)\\[[^\\]]*\\]");
		Matcher matcher = pattern.matcher(lineContent);
		while (matcher.find()) {
			String type = matcher.group(1);
			String path = matcher.group(2);
			int startChar = matcher.start(2);
			int endChar = matcher.end(2);

			Location loc = resolveFileLocation(baseUri, path);
			if (loc == null && "image".equals(type)) {
				loc = resolveFileLocation(baseUri, "img/" + path);
			}

			if (loc != null) {
				Range range = new Range(new Position(lineIndex, startChar), new Position(lineIndex, endChar));
				DocumentLink link = new DocumentLink(range, loc.getUri(), "Open " + path);
				links.add(link);
			}
		}
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
			// Matches image::path[...] or image:path[...]
			Pattern imagePattern = Pattern.compile("(image:[:]?([^\\s\\[\\]]+)\\[[^\\]]*\\])");
			Matcher matcher = imagePattern.matcher(lineContent);

			while (matcher.find()) {
				// Check if the cursor is within the match range
				if (charPos >= matcher.start() && charPos <= matcher.end()) {
					String imageName = matcher.group(2);
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

							Hover hover = new Hover();
							if (imgFile.exists()) {
								String imgUri = imgFile.toURI().toString();
								// Render the actual image in Markdown
								String content = String.format("![%s](%s)", imageName, imgUri);
								hover.setContents(new MarkupContent(MarkupKind.MARKDOWN, content));
							} else {
								// Explicitly inform that the image was not found
								String content = String.format("**Image not found:** `%s`\n\nChecked in:\n* `%s`\n* `%s`", 
										imageName, new File(parentDir, "img/").getPath(), parentDir.getPath());
								hover.setContents(new MarkupContent(MarkupKind.MARKDOWN, content));
							}
							return hover;
						} catch (Exception e) {
							// Fallback to default
						}
					}
				}
			}

			// If no specific syntax is matched, return null
			return null;
		});
	}
	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
			DefinitionParams params) {

		String uri = params.getTextDocument().getUri();
		AsciidocDocumentModel model = this.docs.get(uri);
		if (model == null) {
			return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
		}

		int line = params.getPosition().getLine();
		int character = params.getPosition().getCharacter();

		String lineContent = model.getLineContent(line);
		if (lineContent == null) {
			return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
		}

		// Detect include::path[...]
		Pattern includePattern = Pattern.compile("include::([^\\s\\[\\]]+)\\[[^\\]]*\\]");
		Matcher matcher = includePattern.matcher(lineContent);
		while (matcher.find()) {
			int start = matcher.start(1);
			int end = matcher.end(1);
			if (character >= start && character <= end) {
				String path = matcher.group(1);
				Location loc = resolveFileLocation(uri, path);
				if (loc != null) {
					return CompletableFuture.completedFuture(Either.forLeft(Collections.singletonList(loc)));
				}
			}
		}

		// Detect image::path[...]
		Pattern imagePattern = Pattern.compile("image:[:]?([^\\s\\[\\]]+)\\[[^\\]]*\\]");
		matcher = imagePattern.matcher(lineContent);
		while (matcher.find()) {
			int start = matcher.start(1);
			int end = matcher.end(1);
			if (character >= start && character <= end) {
				String path = matcher.group(1);
				// Try direct path
				Location loc = resolveFileLocation(uri, path);
				if (loc == null) {
					// Try in img/ subdirectory
					loc = resolveFileLocation(uri, "img/" + path);
				}
				if (loc != null) {
					return CompletableFuture.completedFuture(Either.forLeft(Collections.singletonList(loc)));
				}
			}
		}

		return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
	}

	private Location resolveFileLocation(String baseUri, String relativePath) {
		try {
			URI uri = new URI(baseUri);
			File baseFile = new File(uri);
			File parentDir = baseFile.getParentFile();
			File targetFile = new File(parentDir, relativePath);
			if (targetFile.exists()) {
				Location location = new Location();
				location.setUri(targetFile.toURI().toString());
				location.setRange(new Range(new Position(0, 0), new Position(0, 0)));
				return location;
			}
		} catch (Exception e) {
			// ignore
		}
		return null;
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
