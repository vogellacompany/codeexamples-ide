package com.vogella.langserverdemo;

import java.util.Arrays;

import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;

public class DartStreamConnectionProvider extends ProcessStreamConnectionProvider {

	public DartStreamConnectionProvider() {
		setWorkingDirectory(System.getProperty("user.dir"));
		setCommands(Arrays.asList("/usr/bin/env", ".pub-cache/bin/dart_language_server"));
	}
}
