package com.vogella.languageserver.asciidoc.client;

import java.io.IOException;


import com.vogella.languageserver.asciidoc.AsciidocLanguageServer;

public class ConnectionProviderSolution extends AbstractConnectionProvider {
	private static final AsciidocLanguageServer TRAVEL_LANGUAGE_SERVER = new AsciidocLanguageServer();
	public ConnectionProviderSolution() {
		super(TRAVEL_LANGUAGE_SERVER);
	}
	
	@Override
	public void start() throws IOException {
		super.start();
		TRAVEL_LANGUAGE_SERVER.setRemoteProxy(launcher.getRemoteProxy());
	}
}
