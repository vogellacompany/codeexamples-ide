package com.vogella.lsp.asciidoc.server.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
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

		// Create subdirectories and files for path completion testing
		Path docsDir = tempDir.resolve("docs");
		Files.createDirectories(docsDir);
		Files.createFile(docsDir.resolve("chapter1.adoc"));
		Files.createFile(docsDir.resolve("chapter2.adoc"));

		Path subDir = docsDir.resolve("sub");
		Files.createDirectories(subDir);
		Files.createFile(subDir.resolve("nested.adoc"));

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

	@Test
	void testIncludePathCompletionWithSubdirectory() throws Exception {
		// content: include::docs/
		// cursor at index 14: include::docs/|
		String content = "include::docs/";
		List<CompletionItem> completions = getCompletions(content, 0, 14);

		// Should find both files in docs/
		CompletionItem chapter1 = findItem(completions, "chapter1.adoc");
		CompletionItem chapter2 = findItem(completions, "chapter2.adoc");
		assertNotNull(chapter1, "chapter1.adoc completion not found");
		assertNotNull(chapter2, "chapter2.adoc completion not found");

		// Should also find the sub/ directory
		CompletionItem subDir = findItem(completions, "sub/");
		assertNotNull(subDir, "sub/ directory completion not found");
		assertEquals(CompletionItemKind.Folder, subDir.getKind(), "sub/ should be a folder");

		// Verify the text edit includes the full path
		TextEdit edit = chapter1.getTextEdit().getLeft();
		assertEquals("docs/chapter1.adoc[]", edit.getNewText());
	}

	@Test
	void testIncludePathCompletionWithPrefix() throws Exception {
		// content: include::docs/ch
		// cursor at index 16: include::docs/ch|
		String content = "include::docs/ch";
		List<CompletionItem> completions = getCompletions(content, 0, 16);

		// Should find both chapter files
		CompletionItem chapter1 = findItem(completions, "chapter1.adoc");
		CompletionItem chapter2 = findItem(completions, "chapter2.adoc");
		assertNotNull(chapter1, "chapter1.adoc completion not found");
		assertNotNull(chapter2, "chapter2.adoc completion not found");

		// Should NOT find sub/ since it doesn't start with 'ch'
		CompletionItem subDir = findItem(completions, "sub/");
		assertEquals(null, subDir, "sub/ should not be in completions");
	}

	@Test
	void testIncludePathCompletionNestedDirectory() throws Exception {
		// content: include::docs/sub/
		// cursor at index 18: include::docs/sub/|
		String content = "include::docs/sub/";
		List<CompletionItem> completions = getCompletions(content, 0, 18);

		// Should find nested.adoc in docs/sub/
		CompletionItem nested = findItem(completions, "nested.adoc");
		assertNotNull(nested, "nested.adoc completion not found");

		// Verify the text edit includes the full nested path
		TextEdit edit = nested.getTextEdit().getLeft();
		assertEquals("docs/sub/nested.adoc[]", edit.getNewText());
	}

	@Test
	void testIncludeDirectoryCompletion() throws Exception {
		// content: include::d
		// cursor at index 10: include::d|
		String content = "include::d";
		List<CompletionItem> completions = getCompletions(content, 0, 10);

		// Should find docs/ directory
		CompletionItem docsDir = findItem(completions, "docs/");
		assertNotNull(docsDir, "docs/ directory completion not found");
		assertEquals(CompletionItemKind.Folder, docsDir.getKind(), "docs/ should be a folder");

		// Verify the text edit
		TextEdit edit = docsDir.getTextEdit().getLeft();
		assertEquals("docs/", edit.getNewText(), "Directory completion should not add []");
	}

	@Test
	void testIncludeCompletionsSorted() throws Exception {
		// content: include::docs/
		// cursor at index 14: include::docs/|
		String content = "include::docs/";
		List<CompletionItem> completions = getCompletions(content, 0, 14);

		// Verify we have the expected items
		assertNotNull(findItem(completions, "chapter1.adoc"), "chapter1.adoc not found");
		assertNotNull(findItem(completions, "chapter2.adoc"), "chapter2.adoc not found");
		assertNotNull(findItem(completions, "sub/"), "sub/ not found");

		// Verify sorting: directories first (with "0_" prefix), then files (with "1_" prefix)
		CompletionItem subDir = findItem(completions, "sub/");
		CompletionItem chapter1 = findItem(completions, "chapter1.adoc");
		CompletionItem chapter2 = findItem(completions, "chapter2.adoc");

		// Check sortText values
		assertEquals("0_sub", subDir.getSortText(), "Directory should have 0_ prefix");
		assertEquals("1_chapter1.adoc", chapter1.getSortText(), "File should have 1_ prefix");
		assertEquals("1_chapter2.adoc", chapter2.getSortText(), "File should have 1_ prefix");

		// Verify directory comes before files when sorted
		assertTrue(subDir.getSortText().compareTo(chapter1.getSortText()) < 0,
				"Directory should sort before files");
	}

	private CompletionItem findItem(List<CompletionItem> items, String label) {
		return items.stream().filter(i -> i.getLabel().equals(label)).findFirst().orElse(null);
	}
}
