package com.vogella.dartlanguageserver;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import org.eclipse.lsp4e.server.StreamConnectionProvider;

public class DartStreamConnectionProvider extends ProcessStreamConnectionProvider implements StreamConnectionProvider{

    public DartStreamConnectionProvider() {
        String dartSdkLocation = "/home/vogella/git/flutter/bin/cache/dart-sdk"; 

        setWorkingDirectory(System.getProperty("user.dir")); //$NON-NLS-1$
        List<String> commands = new ArrayList<>();
        commands.add(dartSdkLocation + "/bin/"); //$NON-NLS-1$
		commands.add(dartSdkLocation + "/bin/snapshots/analysis_server.dart.snapshot"); //$NON-NLS-1$
		commands.add("--lsp"); //$NON-NLS-1$
		setCommands(commands);
    }
}