package com.vogella.lsp.asciidoc.server;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

public class AsciidocLanguageServer implements LanguageServer {

	private TextDocumentService textService;
	private WorkspaceService workspaceService;
	LanguageClient client;

	public AsciidocLanguageServer() {
		textService = new AsciidocTextDocumentService(this);
		workspaceService = new AsciidocWorkspaceService();
	}

	/**
	 * Here we tell the framework which functionality our server supports
	 */
	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		final InitializeResult res = new InitializeResult(new ServerCapabilities());
		res.getCapabilities().setCompletionProvider(new CompletionOptions());
//		res.getCapabilities().setCodeActionProvider(Boolean.TRUE);
//		res.getCapabilities().setHoverProvider(Boolean.TRUE);
//		res.getCapabilities().setReferencesProvider(Boolean.TRUE);
		res.getCapabilities().setTextDocumentSync(TextDocumentSyncKind.Full);
//		res.getCapabilities().setDefinitionProvider(Boolean.TRUE);
		res.getCapabilities().setDocumentSymbolProvider(Boolean.TRUE);

		return CompletableFuture.supplyAsync(() -> res);
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		return CompletableFuture.supplyAsync(() -> {
			return Boolean.FALSE;
		});
	}

	@Override
	public void exit() {
		System.out.println("Shutdown");
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return this.textService;
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return this.workspaceService;
	}

	public void setRemoteProxy(LanguageClient remoteProxy) {
		this.client = remoteProxy;
	}

}
