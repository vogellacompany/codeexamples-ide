package com.vogella.lsp.asciidoc.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
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
	private static final Pattern EXTERNAL_LINK_PATTERN = Pattern.compile("(https?://[^\\s\\[]+)");

	// Default directory for images (can be configured if needed)
	private static final String DEFAULT_IMAGE_DIR = "img/";

	// AsciiDoc directive prefixes for completion
	private static final String IMAGE_DIRECTIVE = "image::";
	private static final String INCLUDE_DIRECTIVE = "include::";
	private static final String IMG_DIRECTORY = "img";

	// Supported image file extensions
	private static final String[] IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".svg"};
	private static final String ADOC_EXTENSION = ".adoc";

	// Constants for hover functionality
	private static final int INCLUDE_PREVIEW_MAX_LINES = 10;
	private static final int STRING_BUILDER_INITIAL_CAPACITY = 256;
	private static final long BYTES_PER_KILOBYTE = 1024;
	private static final long BYTES_PER_MEGABYTE = BYTES_PER_KILOBYTE * 1024;

	private final Map<String, AsciidocDocumentModel> docs = Collections.synchronizedMap(new HashMap<>());

	// Cache for document links to avoid repeated file I/O operations
	private final Map<String, List<DocumentLink>> linkCache = Collections.synchronizedMap(new HashMap<>());

	private final AsciidocLanguageServer languageServer;

	public AsciidocTextDocumentService(AsciidocLanguageServer languageServer) {
		this.languageServer = languageServer;
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
		return CompletableFuture.supplyAsync(() -> {
			List<CompletionItem> completionItems = new ArrayList<>();

			// Get document model
			String uri = params.getTextDocument().getUri();
			AsciidocDocumentModel model = docs.get(uri);

			if (model == null) {
				return Either.forLeft(getStandardCompletions());
			}

			// Get the typed text at the current position
			String typedText = getTypedTextAtPosition(model, params.getPosition());

			// If IMAGE_DIRECTIVE is typed, suggest image files
			if (typedText.startsWith(IMAGE_DIRECTIVE)) {
				String pathAfterImage = typedText.substring(IMAGE_DIRECTIVE.length());
				List<CompletionItem> imageCompletions = getImageFileCompletions(uri, pathAfterImage);
				if (!imageCompletions.isEmpty()) {
					return Either.forLeft(imageCompletions);
				}
			}

			// If INCLUDE_DIRECTIVE is typed, suggest .adoc files
			if (typedText.startsWith(INCLUDE_DIRECTIVE)) {
				String pathAfterInclude = typedText.substring(INCLUDE_DIRECTIVE.length());
				List<CompletionItem> includeCompletions = getIncludeFileCompletions(uri, pathAfterInclude);
				if (!includeCompletions.isEmpty()) {
					return Either.forLeft(includeCompletions);
				}
			}

			// Otherwise, provide standard AsciiDoc completions with filtering
			completionItems = getMatchingStandardCompletions(typedText);

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

			List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();
			List<String> lines = model.getLines();

			// Track header hierarchy for proper nesting
			List<DocumentSymbol> headerStack = new ArrayList<>();

			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i).trim();
				int lineNumber = i;

				// Parse headers (= through =====)
				if (line.startsWith("=")) {
					int level = getHeaderLevel(line);
					DocumentSymbol headerSymbol = parseHeader(line, lineNumber, level);
					if (headerSymbol != null) {
						// Add to appropriate parent based on hierarchy
						if (level == 1 || headerStack.isEmpty()) {
							symbols.add(Either.forRight(headerSymbol));
							headerStack.clear();
							headerStack.add(headerSymbol);
						} else {
							// Find the appropriate parent
							while (headerStack.size() >= level) {
								headerStack.remove(headerStack.size() - 1);
							}

							if (!headerStack.isEmpty()) {
								DocumentSymbol parent = headerStack.get(headerStack.size() - 1);
								List<DocumentSymbol> children = parent.getChildren();
								children.add(headerSymbol);
							} else {
								symbols.add(Either.forRight(headerSymbol));
							}

							headerStack.add(headerSymbol);
						}
						continue;
					}
				}

				// Parse includes
				DocumentSymbol includeSymbol = parseInclude(line, lineNumber);
				if (includeSymbol != null) {
					addToCurrentSection(symbols, headerStack, includeSymbol);
					continue;
				}

				// Parse images
				DocumentSymbol imageSymbol = parseImage(line, lineNumber);
				if (imageSymbol != null) {
					addToCurrentSection(symbols, headerStack, imageSymbol);
					continue;
				}

				// Parse source blocks
				if (line.startsWith("[source")) {
					DocumentSymbol sourceSymbol = parseSourceBlock(lines, i);
					if (sourceSymbol != null) {
						addToCurrentSection(symbols, headerStack, sourceSymbol);
					}
					continue;
				}

				// Parse tables
				if (line.equals("|===")) {
					DocumentSymbol tableSymbol = parseTable(lines, i);
					if (tableSymbol != null) {
						addToCurrentSection(symbols, headerStack, tableSymbol);
					}
					continue;
				}
			}

			return symbols;
		});
	}

	private DocumentSymbol parseHeader(String line, int lineNumber, int level) {
		// Skip lines that are only delimiters (e.g., ==== for example blocks)
		if (level >= 4 && line.trim().matches("=+")) {
			return null;
		}

		String title = line.substring(level).trim();
		if (title.isEmpty()) {
			return null;
		}

		DocumentSymbol symbol = new DocumentSymbol();
		symbol.setName(title);

		// Map header levels to symbol kinds
		switch (level) {
			case 1:
				symbol.setKind(SymbolKind.Module);
				break;
			case 2:
				symbol.setKind(SymbolKind.Class);
				break;
			case 3:
				symbol.setKind(SymbolKind.Method);
				break;
			case 4:
				symbol.setKind(SymbolKind.Property);
				break;
			default:
				symbol.setKind(SymbolKind.String);
		}

		Position start = new Position(lineNumber, 0);
		Position end = new Position(lineNumber, line.length());
		symbol.setRange(new Range(start, end));
		symbol.setSelectionRange(new Range(start, end));
		symbol.setChildren(new ArrayList<>());

		return symbol;
	}

	private int getHeaderLevel(String line) {
		int level = 0;
		while (level < line.length() && line.charAt(level) == '=') {
			level++;
		}
		return level;
	}

	private DocumentSymbol parseInclude(String line, int lineNumber) {
		// Check for include macro at start of line (block-level macro)
		if (!line.startsWith("include::")) {
			return null;
		}

		int endIndex = line.indexOf("[", "include::".length());
		if (endIndex == -1) {
			endIndex = line.length();
		}

		String includePath = line.substring("include::".length(), endIndex);

		DocumentSymbol symbol = new DocumentSymbol();
		symbol.setName("📄 " + includePath);
		symbol.setKind(SymbolKind.File);
		symbol.setDetail("include");

		Position start = new Position(lineNumber, 0);
		Position end = new Position(lineNumber, line.length());
		symbol.setRange(new Range(start, end));
		symbol.setSelectionRange(new Range(start, end));

		return symbol;
	}

	private DocumentSymbol parseImage(String line, int lineNumber) {
		// Check for image macro at start of line (block-level macro)
		// image:: is block image, image: is inline image
		String imagePrefix = null;
		if (line.startsWith("image::")) {
			imagePrefix = "image::";
		} else if (line.startsWith("image:")) {
			imagePrefix = "image:";
		} else {
			return null;
		}

		int endIndex = line.indexOf("[", imagePrefix.length());
		if (endIndex == -1) {
			endIndex = line.length();
		}

		String imagePath = line.substring(imagePrefix.length(), endIndex);

		DocumentSymbol symbol = new DocumentSymbol();
		symbol.setName("🖼️ " + imagePath);
		symbol.setKind(SymbolKind.File);
		symbol.setDetail("image");

		Position start = new Position(lineNumber, 0);
		Position end = new Position(lineNumber, line.length());
		symbol.setRange(new Range(start, end));
		symbol.setSelectionRange(new Range(start, end));

		return symbol;
	}

	private DocumentSymbol parseSourceBlock(List<String> lines, int startLine) {
		String sourceLine = lines.get(startLine).trim();
		String language = "code";

		// Extract language from [source,language] or [source,language,attr1,attr2]
		// Only take the first attribute after 'source'
		if (sourceLine.contains(",")) {
			int commaIndex = sourceLine.indexOf(",");
			int endIndex = sourceLine.indexOf(",", commaIndex + 1); // Next comma
			if (endIndex == -1) {
				endIndex = sourceLine.indexOf("]", commaIndex); // Or closing bracket
			}
			if (endIndex != -1) {
				language = sourceLine.substring(commaIndex + 1, endIndex).trim();
			}
		}

		// Check if opening delimiter is on the same line (e.g., [source,java]----)
		boolean inBlock = sourceLine.endsWith("----") || sourceLine.contains("]----");

		// Find the end of the source block (delimited by ----)
		int endLine = inBlock ? startLine + 1 : startLine;

		// Skip to next line if delimiter wasn't on source line
		if (!inBlock) {
			endLine++;
		}

		while (endLine < lines.size()) {
			String line = lines.get(endLine).trim();
			if (line.equals("----") || line.endsWith("----")) {
				if (inBlock) {
					break;
				}
				inBlock = true;
			}
			endLine++;
		}

		DocumentSymbol symbol = new DocumentSymbol();
		symbol.setName("💻 " + language + " code block");
		symbol.setKind(SymbolKind.Function);
		symbol.setDetail("source");

		Position start = new Position(startLine, 0);
		Position end = new Position(Math.min(endLine, lines.size() - 1), 0);
		symbol.setRange(new Range(start, end));
		symbol.setSelectionRange(new Range(start, start));

		return symbol;
	}

	private DocumentSymbol parseTable(List<String> lines, int startLine) {
		// Find the end of the table
		int endLine = startLine + 1;
		while (endLine < lines.size()) {
			if (lines.get(endLine).trim().equals("|===")) {
				break;
			}
			endLine++;
		}

		DocumentSymbol symbol = new DocumentSymbol();
		symbol.setName("📊 Table");
		symbol.setKind(SymbolKind.Array);
		symbol.setDetail("table");

		Position start = new Position(startLine, 0);
		Position end = new Position(Math.min(endLine, lines.size() - 1), 0);
		symbol.setRange(new Range(start, end));
		symbol.setSelectionRange(new Range(start, start));

		return symbol;
	}

	private void addToCurrentSection(List<Either<SymbolInformation, DocumentSymbol>> symbols,
			List<DocumentSymbol> headerStack, DocumentSymbol symbolToAdd) {
		if (headerStack.isEmpty()) {
			symbols.add(Either.forRight(symbolToAdd));
		} else {
			DocumentSymbol parent = headerStack.get(headerStack.size() - 1);
			List<DocumentSymbol> children = parent.getChildren();
			// Children is always initialized in parseHeader, so no null check needed
			children.add(symbolToAdd);
		}
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		return CompletableFuture.supplyAsync(() -> {
			// Get the position and document
			Position position = params.getPosition();
			String uri = params.getTextDocument().getUri();

			// Get document model
			AsciidocDocumentModel model = docs.get(uri);
			if (model == null) {
				return null;
			}

			// Get the line content at hover position
			String lineContent = model.getLineContent(position.getLine());
			if (lineContent == null || lineContent.trim().isEmpty()) {
				return null;
			}

			// Get document directory for resolving relative paths
			Path documentPath = getDocumentPath(uri);
			if (documentPath == null) {
				return null;
			}
			Path documentDir = documentPath.getParent();
			if (documentDir == null) {
				return null;
			}

			int cursorCharacter = position.getCharacter();

			// Check for image references
			Hover imageHover = handleImageHover(lineContent, documentDir, cursorCharacter);
			if (imageHover != null) {
				return imageHover;
			}

			// Check for include references
			Hover includeHover = handleIncludeHover(lineContent, documentDir, cursorCharacter);
			if (includeHover != null) {
				return includeHover;
			}

			// Check for link references
			Hover linkHover = handleLinkHover(lineContent, documentDir, cursorCharacter);
			if (linkHover != null) {
				return linkHover;
			}

			// Provide default syntax help
			return getDefaultSyntaxHelp(lineContent);
		});
	}

	/**
	 * Handles hover for image references (image::filename[])
	 */
	private Hover handleImageHover(String lineContent, Path documentDir, int cursorPosition) {
		Matcher matcher = IMAGE_PATTERN.matcher(lineContent);

		while (matcher.find()) {
			// Check if cursor is within the match range
			if (cursorPosition >= matcher.start() && cursorPosition <= matcher.end()) {
				String imageName = matcher.group(1).trim();

				// Look for image in img subdirectory
				Path imgDir = documentDir.resolve(DEFAULT_IMAGE_DIR.replace("/", ""));
				Path imagePath = imgDir.resolve(imageName);

				StringBuilder content = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
				content.append("### Image Reference\n\n");

				if (Files.exists(imagePath)) {
					try {
						long fileSize = Files.size(imagePath);
						String formattedSize = formatFileSize(fileSize);

						content.append("**File:** `").append(imageName).append("`\n\n");
						content.append("**Path:** `").append(imagePath.toString()).append("`\n\n");
						content.append("**Size:** ").append(formattedSize).append("\n\n");
						content.append("✓ Image file found and ready to display");
					} catch (IOException e) {
						content.append("⚠ Error reading image file: ").append(imageName);
						logError("Error reading image file: " + imageName, e);
					}
				} else {
					content.append("⚠ **Image not found:** `").append(imageName).append("`\n\n");
					content.append("Expected location: `").append(imagePath.toString()).append("`\n\n");
					content.append("Make sure the image exists in the `img/` subdirectory.");
				}

				Hover hover = new Hover();
				hover.setContents(new MarkupContent(MarkupKind.MARKDOWN, content.toString()));
				return hover;
			}
		}

		return null;
	}

	/**
	 * Handles hover for include references (include::file[])
	 */
	private Hover handleIncludeHover(String lineContent, Path documentDir, int cursorPosition) {
		Matcher matcher = INCLUDE_PATTERN.matcher(lineContent);

		while (matcher.find()) {
			// Check if cursor is within the match range
			if (cursorPosition >= matcher.start() && cursorPosition <= matcher.end()) {
				String includeFile = matcher.group(1).trim();
				Path includePath = documentDir.resolve(includeFile);

				StringBuilder content = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
				content.append("### Include Reference\n\n");

				if (Files.exists(includePath)) {
					try {
						long fileSize = Files.size(includePath);
						String formattedSize = formatFileSize(fileSize);

						content.append("**File:** `").append(includeFile).append("`\n\n");
						content.append("**Path:** `").append(includePath.toString()).append("`\n\n");
						content.append("**Size:** ").append(formattedSize).append("\n\n");

						// Read first N lines of the included file
						List<String> preview = readFilePreview(includePath, INCLUDE_PREVIEW_MAX_LINES);
						if (!preview.isEmpty()) {
							content.append("**Preview (first 10 lines):**\n\n```asciidoc\n");
							for (String line : preview) {
								content.append(line).append("\n");
							}
							content.append("```");
						}
					} catch (IOException e) {
						content.append("⚠ Error reading include file: ").append(includeFile);
						logError("Error reading include file: " + includeFile, e);
					}
				} else {
					content.append("⚠ **Include file not found:** `").append(includeFile).append("`\n\n");
					content.append("Expected location: `").append(includePath.toString()).append("`");
				}

				Hover hover = new Hover();
				hover.setContents(new MarkupContent(MarkupKind.MARKDOWN, content.toString()));
				return hover;
			}
		}

		return null;
	}

	/**
	 * Handles hover for link references (http://, https://, or internal files)
	 */
	private Hover handleLinkHover(String lineContent, Path documentDir, int cursorPosition) {
		// Check for external links (bare URLs)
		Matcher externalMatcher = EXTERNAL_LINK_PATTERN.matcher(lineContent);

		while (externalMatcher.find()) {
			// Check if cursor is within the match range
			if (cursorPosition >= externalMatcher.start() && cursorPosition <= externalMatcher.end()) {
				String url = externalMatcher.group(1);

				StringBuilder content = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
				content.append("### External Link\n\n");
				content.append("**URL:** ").append(url).append("\n\n");
				content.append("🔗 This is an external web link");

				Hover hover = new Hover();
				hover.setContents(new MarkupContent(MarkupKind.MARKDOWN, content.toString()));
				return hover;
			}
		}

		// Check for link macro (link:target[text])
		Matcher internalMatcher = LINK_PATTERN.matcher(lineContent);

		while (internalMatcher.find()) {
			// Check if cursor is within the match range
			if (cursorPosition >= internalMatcher.start() && cursorPosition <= internalMatcher.end()) {
				String linkTarget = internalMatcher.group(1).trim();

				// Handle external URLs in link macro
				if (linkTarget.startsWith("http://") || linkTarget.startsWith("https://")) {
					StringBuilder content = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
					content.append("### External Link\n\n");
					content.append("**URL:** ").append(linkTarget).append("\n\n");
					content.append("🔗 This is an external web link");

					Hover hover = new Hover();
					hover.setContents(new MarkupContent(MarkupKind.MARKDOWN, content.toString()));
					return hover;
				}

				// Handle internal file links
				Path linkPath = documentDir.resolve(linkTarget);

				StringBuilder content = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
				content.append("### Internal Link\n\n");

				if (Files.exists(linkPath)) {
					try {
						long fileSize = Files.size(linkPath);
						String formattedSize = formatFileSize(fileSize);

						content.append("**File:** `").append(linkTarget).append("`\n\n");
						content.append("**Path:** `").append(linkPath.toString()).append("`\n\n");
						content.append("**Size:** ").append(formattedSize).append("\n\n");
						content.append("✓ Link target found");
					} catch (IOException e) {
						content.append("⚠ Error accessing file: ").append(linkTarget);
						logError("Error accessing link file: " + linkTarget, e);
					}
				} else {
					content.append("⚠ **Link target not found:** `").append(linkTarget).append("`\n\n");
					content.append("Expected location: `").append(linkPath.toString()).append("`");
				}

				Hover hover = new Hover();
				hover.setContents(new MarkupContent(MarkupKind.MARKDOWN, content.toString()));
				return hover;
			}
		}

		return null;
	}

	/**
	 * Provides default syntax help based on line content
	 */
	private Hover getDefaultSyntaxHelp(String lineContent) {
		String trimmed = lineContent.trim();
		StringBuilder content = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
		content.append("### AsciiDoc Syntax Help\n\n");

		// Check for headers
		if (trimmed.startsWith("=")) {
			int level = 0;
			while (level < trimmed.length() && trimmed.charAt(level) == '=') {
				level++;
			}
			content.append("**Header Level ").append(level).append("**\n\n");
			content.append("Syntax: `").append("=".repeat(level)).append(" Header Title`\n\n");
			content.append("Creates a level ").append(level).append(" section header.");

		// Check for code blocks
		} else if (trimmed.startsWith("----") || trimmed.startsWith("```")) {
			content.append("**Code Block Delimiter**\n\n");
			content.append("Syntax:\n```asciidoc\n----\ncode here\n----\n```\n\n");
			content.append("Use `----` or ` ``` ` to define code blocks.");

		// Check for lists
		} else if (trimmed.startsWith("*") || trimmed.startsWith("-")) {
			content.append("**Unordered List Item**\n\n");
			content.append("Syntax: `* Item` or `- Item`\n\n");
			content.append("Use `*` or `-` at the start of a line for bullet points.");

		} else if (trimmed.matches("^\\.\\s+.*")) {
			content.append("**Ordered List Item**\n\n");
			content.append("Syntax: `. Item`\n\n");
			content.append("Use `.` at the start of a line for numbered lists.");

		// Default help
		} else {
			content.append("**Common AsciiDoc Elements:**\n\n");
			content.append("* `= Title` - Document title\n");
			content.append("* `== Section` - Section header\n");
			content.append("* `image::path/to/image.png[]` - Embed image\n");
			content.append("* `include::file.adoc[]` - Include another file\n");
			content.append("* `link:url[text]` - Create hyperlink\n");
			content.append("* `*bold*` - Bold text\n");
			content.append("* `_italic_` - Italic text\n");
			content.append("* `` `code` `` - Inline code\n");
		}

		Hover hover = new Hover();
		hover.setContents(new MarkupContent(MarkupKind.MARKDOWN, content.toString()));
		return hover;
	}

	/**
	 * Converts URI string to file system Path
	 */
	private Path getDocumentPath(String uriString) {
		try {
			URI uri = new URI(uriString);
			return Paths.get(uri);
		} catch (URISyntaxException | IllegalArgumentException | java.nio.file.FileSystemNotFoundException e) {
			logError("Error converting URI to path: " + uriString, e);
			return null;
		}
	}

	/**
	 * Formats file size in human-readable format
	 */
	private String formatFileSize(long bytes) {
		if (bytes < BYTES_PER_KILOBYTE) {
			return bytes + " B";
		} else if (bytes < BYTES_PER_MEGABYTE) {
			return String.format("%.2f KB", bytes / (double) BYTES_PER_KILOBYTE);
		} else {
			return String.format("%.2f MB", bytes / (double) BYTES_PER_MEGABYTE);
		}
	}

	/**
	 * Reads the first N lines from a file
	 */
	private List<String> readFilePreview(Path filePath, int maxLines) {
		List<String> lines = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(filePath)) {
			String line;
			int count = 0;
			while ((line = reader.readLine()) != null && count < maxLines) {
				lines.add(line);
				count++;
			}
		} catch (IOException e) {
			logError("Error reading file preview: " + filePath, e);
		}
		return lines;
	}

	/**
	 * Logs an error message with exception details using LSP logging
	 */
	private void logError(String message, Exception e) {
		String fullMessage = "[AsciidocTextDocumentService] " + message;
		if (e != null) {
			fullMessage += ": " + e.getClass().getSimpleName() + " - " + e.getMessage();
		}

		// Use LSP client logging for better IDE integration
		if (languageServer != null && languageServer.client != null) {
			languageServer.client.logMessage(new MessageParams(MessageType.Error, fullMessage));
		} else {
			// Fallback to logger if client not available
			LOGGER.log(Level.SEVERE, fullMessage, e);
		}
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

	/**
	 * Get the typed text from the start of the line to the current position
	 */
	private String getTypedTextAtPosition(AsciidocDocumentModel model, Position position) {
		String lineContent = model.getLineContent(position.getLine());
		if (lineContent == null) {
			return "";
		}

		int offset = position.getCharacter();
		if (offset > lineContent.length()) {
			offset = lineContent.length();
		}

		// Return text from start of line to cursor position
		return lineContent.substring(0, offset);
	}

	/**
	 * Get completion items for image files in img/ directory
	 */
	private List<CompletionItem> getImageFileCompletions(String documentUri, String pathAfterImage) {
		String directoryPath = getDirectoryPath(documentUri);
		if (directoryPath == null) {
			return Collections.emptyList();
		}

		// Images are expected in an IMG_DIRECTORY subdirectory
		File imgFolder = new File(directoryPath, IMG_DIRECTORY);
		if (!imgFolder.exists() || !imgFolder.isDirectory()) {
			return Collections.emptyList();
		}

		String lowerPrefix = pathAfterImage.toLowerCase();
		File[] files = imgFolder.listFiles((dir, name) -> {
			String lowerName = name.toLowerCase();
			return hasImageExtension(lowerName)
				&& lowerName.startsWith(lowerPrefix);
		});

		if (files == null) {
			return Collections.emptyList();
		}

		List<CompletionItem> items = new ArrayList<>();
		for (File file : files) {
			items.add(createFileCompletionItem(file.getName(), IMAGE_DIRECTIVE + file.getName() + "[]", "Image file"));
		}

		return items;
	}

	/**
	 * Get completion items for include files (.adoc files)
	 */
	private List<CompletionItem> getIncludeFileCompletions(String documentUri, String pathAfterInclude) {
		String directoryPath = getDirectoryPath(documentUri);
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

		// Filter files by extension and prefix (case-insensitive)
		final String lowerPrefix = filePrefix.toLowerCase();
		File[] files = folder.listFiles((dir, name) -> {
			String lowerName = name.toLowerCase();
			return lowerName.endsWith(ADOC_EXTENSION) && lowerName.startsWith(lowerPrefix);
		});

		if (files == null) {
			return Collections.emptyList();
		}

		// Build the full path for each file (including the directory part)
		String pathPrefix = pathAfterInclude.substring(0, pathAfterInclude.length() - filePrefix.length());

		List<CompletionItem> items = new ArrayList<>();
		for (File file : files) {
			String filePath = pathPrefix + file.getName();
			items.add(createFileCompletionItem(file.getName(), INCLUDE_DIRECTIVE + filePath + "[]", "AsciiDoc include file"));
		}

		return items;
	}

	/**
	 * Check if a filename has a supported image extension
	 */
	private static boolean hasImageExtension(String lowerCaseFileName) {
		return Arrays.stream(IMAGE_EXTENSIONS)
			.anyMatch(ext -> lowerCaseFileName.endsWith(ext));
	}

	/**
	 * Extract directory path from document URI
	 */
	private String getDirectoryPath(String documentUri) {
		if (documentUri == null || documentUri.isEmpty()) {
			return null;
		}

		try {
			URI uri = new URI(documentUri);
			File file = new File(uri);
			File parent = file.getParentFile();
			if (parent != null && parent.exists() && parent.isDirectory()) {
				return parent.getAbsolutePath();
			}
			return null;
		} catch (URISyntaxException e) {
			// Log error for invalid URI syntax
			LOGGER.log(Level.WARNING, "Invalid document URI: " + documentUri, e);
			return null;
		} catch (IllegalArgumentException e) {
			// Log error for malformed URI
			LOGGER.log(Level.WARNING, "Malformed document URI: " + documentUri, e);
			return null;
		}
	}

	/**
	 * Helper method to create a file completion item
	 */
	private CompletionItem createFileCompletionItem(String label, String insertText, String detail) {
		CompletionItem item = new CompletionItem();
		item.setLabel(label);
		item.setKind(CompletionItemKind.File);
		item.setInsertText(insertText);
		item.setDetail(detail);
		return item;
	}

	/**
	 * Get standard AsciiDoc completion items with snippet support
	 */
	private List<CompletionItem> getStandardCompletions() {
		List<CompletionItem> items = new ArrayList<>();

		// Header level 1
		CompletionItem header1 = new CompletionItem("= Title");
		header1.setKind(CompletionItemKind.Snippet);
		header1.setInsertText("= ${1:Title}");
		header1.setInsertTextFormat(InsertTextFormat.Snippet);
		header1.setDetail("Header level 1");
		items.add(header1);

		// Header level 2
		CompletionItem header2 = new CompletionItem("== Subtitle");
		header2.setKind(CompletionItemKind.Snippet);
		header2.setInsertText("== ${1:Subtitle}");
		header2.setInsertTextFormat(InsertTextFormat.Snippet);
		header2.setDetail("Header level 2");
		items.add(header2);

		// Header level 3
		CompletionItem header3 = new CompletionItem("=== Subsubtitle");
		header3.setKind(CompletionItemKind.Snippet);
		header3.setInsertText("=== ${1:Subsubtitle}");
		header3.setInsertTextFormat(InsertTextFormat.Snippet);
		header3.setDetail("Header level 3");
		items.add(header3);

		// Unordered list
		CompletionItem list = new CompletionItem("* List item");
		list.setKind(CompletionItemKind.Snippet);
		list.setInsertText("* ${1:List item}");
		list.setInsertTextFormat(InsertTextFormat.Snippet);
		list.setDetail("Unordered list item");
		items.add(list);

		// Ordered list
		CompletionItem orderedList = new CompletionItem("1. Ordered list item");
		orderedList.setKind(CompletionItemKind.Snippet);
		orderedList.setInsertText("1. ${1:Ordered list item}");
		orderedList.setInsertTextFormat(InsertTextFormat.Snippet);
		orderedList.setDetail("Ordered list item");
		items.add(orderedList);

		// Code block
		CompletionItem codeBlock = new CompletionItem("[source]");
		codeBlock.setKind(CompletionItemKind.Snippet);
		codeBlock.setInsertText("[source, ${1:java}]\n----\n${2:code}\n----");
		codeBlock.setInsertTextFormat(InsertTextFormat.Snippet);
		codeBlock.setDetail("Source code block");
		items.add(codeBlock);

		// Attribute
		CompletionItem attribute = new CompletionItem(":attribute:");
		attribute.setKind(CompletionItemKind.Snippet);
		attribute.setInsertText(":${1:attribute}:");
		attribute.setInsertTextFormat(InsertTextFormat.Snippet);
		attribute.setDetail("Document attribute");
		items.add(attribute);

		// Image
		CompletionItem image = new CompletionItem(IMAGE_DIRECTIVE);
		image.setKind(CompletionItemKind.Snippet);
		image.setInsertText("image::${1:path}[]");
		image.setInsertTextFormat(InsertTextFormat.Snippet);
		image.setDetail("Image element");
		items.add(image);

		// Include
		CompletionItem include = new CompletionItem(INCLUDE_DIRECTIVE);
		include.setKind(CompletionItemKind.Snippet);
		include.setInsertText("include::${1:file.adoc}[]");
		include.setInsertTextFormat(InsertTextFormat.Snippet);
		include.setDetail("Include file");
		items.add(include);

		return items;
	}

	/**
	 * Get standard completions filtered by typed text
	 */
	private List<CompletionItem> getMatchingStandardCompletions(String typedText) {
		List<CompletionItem> allItems = getStandardCompletions();

		if (typedText == null || typedText.trim().isEmpty()) {
			return allItems;
		}

		String lowerTyped = typedText.toLowerCase().trim();
		return allItems.stream()
			.filter(item -> item.getLabel().toLowerCase().startsWith(lowerTyped))
			.collect(Collectors.toList());
	}

}
