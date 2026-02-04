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

import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vogella.lsp.asciidoc.server.AsciidocLanguageServer;
import com.vogella.lsp.asciidoc.server.AsciidocTextDocumentService;

class AsciidocDefinitionTest {

	@TempDir
	Path tempDir;

	private AsciidocTextDocumentService service;
	private String docUri;
	private Path includedFile;

	@BeforeEach
	void setUp() throws IOException {
		AsciidocLanguageServer server = new AsciidocLanguageServer();
		service = (AsciidocTextDocumentService) server.getTextDocumentService();

		// Create dummy file to be included
		includedFile = tempDir.resolve("target.adoc");
		Files.createFile(includedFile);

		docUri = tempDir.resolve("test.adoc").toUri().toString();
	}

	private List<? extends Location> getDefinition(String content, int line, int character) throws InterruptedException, ExecutionException {
		service.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(docUri, "asciidoc", 1, content)));
		DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(docUri), new Position(line, character));
		Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> result = service.definition(params).get();
		return result.getLeft();
	}

	@Test
	void testIncludeDefinition() throws Exception {
		String content = "include::target.adoc[]";
		// Cursor on "target.adoc"
		List<? extends Location> locations = getDefinition(content, 0, 10);

		assertNotNull(locations);
		assertFalse(locations.isEmpty());
		assertEquals(1, locations.size());
		
		Location loc = locations.get(0);
		assertEquals(includedFile.toUri(), new java.net.URI(loc.getUri()));
	}

	@Test
	void testIncludeInSubfolderDefinition() throws Exception {
		Path subDir = tempDir.resolve("sub");
		Files.createDirectories(subDir);
		Path subFile = subDir.resolve("subfile.adoc");
		Files.createFile(subFile);

		String content = "include::sub/subfile.adoc[]";
		// Cursor on "subfile.adoc"
		List<? extends Location> locations = getDefinition(content, 0, 15);

		assertNotNull(locations);
		assertEquals(1, locations.size());
		assertEquals(subFile.toUri(), new java.net.URI(locations.get(0).getUri()));
	}

	@Test
	void testNoDefinition() throws Exception {
		String content = "include::nonexistent.adoc[]";
		List<? extends Location> locations = getDefinition(content, 0, 10);

		assertNotNull(locations);
		assertTrue(locations.isEmpty());
	}

	@Test
	void testImageDefinition() throws Exception {
		Path imgDir = tempDir.resolve("img");
		Files.createDirectories(imgDir);
		Path imgFile = imgDir.resolve("Sample.png");
		Files.createFile(imgFile);

		String content = "image::Sample.png[]";
		// Cursor on "Sample.png"
		List<? extends Location> locations = getDefinition(content, 0, 10);

		assertNotNull(locations);
		assertEquals(1, locations.size());
		assertEquals(imgFile.toUri(), new java.net.URI(locations.get(0).getUri()));
	}
}
