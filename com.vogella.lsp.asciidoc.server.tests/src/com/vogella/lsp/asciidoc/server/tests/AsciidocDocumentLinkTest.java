package com.vogella.lsp.asciidoc.server.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vogella.lsp.asciidoc.server.AsciidocLanguageServer;
import com.vogella.lsp.asciidoc.server.AsciidocTextDocumentService;

class AsciidocDocumentLinkTest {

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

	private List<DocumentLink> getLinks(String content) throws InterruptedException, ExecutionException {
		service.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(docUri, "asciidoc", 1, content)));
		DocumentLinkParams params = new DocumentLinkParams(new TextDocumentIdentifier(docUri));
		return service.documentLink(params).get();
	}

	@Test
	void testIncludeLinkRange() throws Exception {
		String filename = "target.adoc";
		String content = "include::" + filename + "[]";
		List<DocumentLink> links = getLinks(content);

		assertNotNull(links);
		assertEquals(1, links.size());
		DocumentLink link = links.get(0);
		
		// Range should cover "target.adoc"
		assertEquals(0, link.getRange().getStart().getLine());
		assertEquals(9, link.getRange().getStart().getCharacter()); // "include::".length() = 9
		assertEquals(9 + filename.length(), link.getRange().getEnd().getCharacter());
		assertEquals(includedFile.toUri(), new java.net.URI(link.getTarget()));
	}

	@Test
	void testImageLinkRange() throws Exception {
		Path imgDir = tempDir.resolve("img");
		Files.createDirectories(imgDir);
		Path imgFile = imgDir.resolve("Sample.png");
		Files.createFile(imgFile);

		String filename = "Sample.png";
		String content = "image::" + filename + "[]";
		List<DocumentLink> links = getLinks(content);

		assertNotNull(links);
		assertEquals(1, links.size());
		DocumentLink link = links.get(0);

		assertEquals(0, link.getRange().getStart().getLine());
		assertEquals(7, link.getRange().getStart().getCharacter()); // "image::".length() = 7
		assertEquals(7 + filename.length(), link.getRange().getEnd().getCharacter());
		assertEquals(imgFile.toUri(), new java.net.URI(link.getTarget()));
	}
}
