package com.vogella.ide.iconreplacer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.PlatformUI;

public class RestartHelper {

	public static void restartWithClean() {
		List<String> commands = new ArrayList<>();
		String existingCmds = System.getProperty("eclipse.commands", "");
		for (String line : existingCmds.split("[\\r\\n]+")) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty()
					&& !trimmed.equals("-clean")
					&& !trimmed.equals("-clearPersistedState")) {
				commands.add(trimmed);
			}
		}
		commands.add("-clean");
		commands.add("-clearPersistedState");

		System.setProperty("eclipse.exitcode", "24"); // 24 = RESTART
		System.setProperty("eclipse.exitdata", String.join("\n", commands));

		PlatformUI.getWorkbench().restart();
	}
}
