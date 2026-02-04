package com.vogella.lsp.asciidoc.server.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vogella.lsp.asciidoc.server.AsciidocLanguageServer;
import com.vogella.lsp.asciidoc.server.AsciidocTextDocumentService;

class AsciidocHoverTest {

	@TempDir
	Path tempDir;

	private AsciidocTextDocumentService service;
	private String docUri;

	@BeforeEach
	void setUp() throws IOException {
		AsciidocLanguageServer server = new AsciidocLanguageServer();
		service = (AsciidocTextDocumentService) server.getTextDocumentService();

		// Create dummy image
		Path imgDir = tempDir.resolve("img");
		Files.createDirectories(imgDir);
		Files.createFile(imgDir.resolve("Sample.png"));

		docUri = tempDir.resolve("test.adoc").toUri().toString();
	}

	private Hover getHover(String content, int line, int character) throws InterruptedException, ExecutionException {
		service.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(docUri, "asciidoc", 1, content)));
		HoverParams params = new HoverParams(new TextDocumentIdentifier(docUri), new Position(line, character));
		return service.hover(params).get();
	}

	@Test
	void testImageHover() throws Exception {
		String content = "\nimage::Sample.png[]";
		// Hover on line 1 over the image macro
		Hover hover = getHover(content, 1, 10);

		assertNotNull(hover);
		MarkupContent markup = hover.getContents().getRight();
		assertNotNull(markup);
		
		String markdown = markup.getValue();
		// Should contain the image reference in Markdown format: ![Sample.png](file:/...)
		assertTrue(markdown.contains("![Sample.png]"), "Markdown should contain alt text");
		assertTrue(markdown.contains("file:"), "Markdown should contain file URI");
		assertTrue(markdown.contains("Sample.png"), "Markdown should contain filename");
	}

	@Test
	void testGenericHoverFallback() throws Exception {
		String content = "\nsome random text";
		// Hover on line 1 where no image is present
		Hover hover = getHover(content, 1, 5);

		assertNotNull(hover);
		MarkupContent markup = hover.getContents().getRight();
		String markdown = markup.getValue();
		
		// Should contain the generic documentation
		assertTrue(markdown.contains("Important AsciiDoc Elements"), "Should show fallback documentation");
		assertTrue(markdown.contains("image::"), "Should mention image macro");
	}

	@Test
	void testNoHoverOnFirstLine() throws Exception {
		String content = "= Document Title";
		// Hover on first line (line 0)
		Hover hover = getHover(content, 0, 5);

		// Current implementation returns null for the first line unless it's a macro
		assertNull(hover, "Hover should be null on the first line by default");
	}
}
