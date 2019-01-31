package com.vogella.langserverdemo;

import java.util.Arrays;
import java.util.List;

import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;

public class DartStreamConnectionProvider extends ProcessStreamConnectionProvider {

	public DartStreamConnectionProvider() {
		String userDir = System.getProperty("user.dir");
		String dartLocation = userDir + "/Library/dart-nightly/bin";
		setWorkingDirectory(userDir);
		List<String> commands = Arrays.asList(
				dartLocation + "/dart", // Use the dart executable
				dartLocation + "/snapshots/analysis_server.dart.snapshot", // The language server
				"--lsp" // Needed parameter to tell the server that it should use the LSP
		);
		setCommands(commands);
	}
}
