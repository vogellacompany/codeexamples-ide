package com.vogella.lsp.asciidoc.server.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vogella.lsp.asciidoc.server.AsciidocLanguageServer;
import com.vogella.lsp.asciidoc.server.AsciidocTextDocumentService;

class AsciidocCompletionTest {

	@TempDir
	Path tempDir;

	private AsciidocTextDocumentService service;
	private String docUri;

	@BeforeEach
	void setUp() throws IOException {
		AsciidocLanguageServer server = new AsciidocLanguageServer();
		service = (AsciidocTextDocumentService) server.getTextDocumentService();

		// Create a dummy image in img/ subdirectory
		Path imgDir = tempDir.resolve("img");
		Files.createDirectories(imgDir);
		Files.createFile(imgDir.resolve("Sample.png"));

		// Create a dummy adoc file for include
		Files.createFile(tempDir.resolve("other.adoc"));

		docUri = tempDir.resolve("test.adoc").toUri().toString();
	}

	private List<CompletionItem> getCompletions(String content, int line, int character)
			throws InterruptedException, ExecutionException {
		service.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(docUri, "asciidoc", 1, content)));
		CompletionParams params = new CompletionParams(new TextDocumentIdentifier(docUri),
				new Position(line, character));
		return service.completion(params).get().getLeft();
	}

	@Test
	void testImageCompletionInsideExistingBrackets() throws Exception {
		// content: image::[]
		// cursor at index 8: image::[|]
		String content = "image::[]";
		List<CompletionItem> completions = getCompletions(content, 0, 8);

		CompletionItem item = findItem(completions, "Sample.png");
		assertNotNull(item, "Sample.png completion not found");

		TextEdit edit = item.getTextEdit().getLeft();
		Range range = edit.getRange();

		// Should replace the existing "[]" which starts at index 7 and ends at 9
		assertEquals(7, range.getStart().getCharacter(), "Start range mismatch");
		assertEquals(9, range.getEnd().getCharacter(), "End range mismatch");
		assertEquals("Sample.png[]", edit.getNewText());
	}

	@Test
	void testImageCompletionWithPrefix() throws Exception {
		// content: image::S
		// cursor at index 8: image::S|
		String content = "image::S";
		List<CompletionItem> completions = getCompletions(content, 0, 8);

		CompletionItem item = findItem(completions, "Sample.png");
		assertNotNull(item, "Sample.png completion not found");

		TextEdit edit = item.getTextEdit().getLeft();
		Range range = edit.getRange();

		// Should replace the prefix "S" which starts at index 7
		assertEquals(7, range.getStart().getCharacter());
		assertEquals(8, range.getEnd().getCharacter());
		assertEquals("Sample.png[]", edit.getNewText());
	}

	@Test
	void testIncludeCompletionInsideBrackets() throws Exception {
		// content: include::[]
		// cursor at index 10: include::[|]
		String content = "include::[]";
		List<CompletionItem> completions = getCompletions(content, 0, 10);

		CompletionItem item = findItem(completions, "other.adoc");
		assertNotNull(item, "other.adoc completion not found");

		TextEdit edit = item.getTextEdit().getLeft();
		Range range = edit.getRange();

		// Should replace "[]" which starts at index 9
		assertEquals(9, range.getStart().getCharacter());
		assertEquals(11, range.getEnd().getCharacter());
		assertEquals("other.adoc[]", edit.getNewText());
	}

	private CompletionItem findItem(List<CompletionItem> items, String label) {
		return items.stream().filter(i -> i.getLabel().equals(label)).findFirst().orElse(null);
	}
}
