package com.vogella.lsp.asciidoc.client;

import java.io.IOException;


import com.vogella.lsp.asciidoc.server.AsciidocLanguageServer;

public class ConnectionProviderSolution extends AbstractConnectionProvider {
	private static final AsciidocLanguageServer LANGUAGE_SERVER = new AsciidocLanguageServer();
	public ConnectionProviderSolution() {
		super(LANGUAGE_SERVER);
	}
	
	@Override
	public void start() throws IOException {
		super.start();
		LANGUAGE_SERVER.setRemoteProxy(launcher.getRemoteProxy());
	}
}
