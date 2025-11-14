package z.ex.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

public class BuildRCPScriptHandler extends AbstractHandler {

	// Hard-coded path (intentional) - points to user's home directory script
	private static final String SCRIPT_PATH = System.getProperty("user.home") +
			"/git/content/_scripts/buildRCPScript.sh";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		File scriptFile = new File(SCRIPT_PATH);

		if (!scriptFile.exists()) {
			showError("Script not found at: " + SCRIPT_PATH);
			return null;
		}

		if (!scriptFile.canExecute()) {
			showError("Script is not executable: " + SCRIPT_PATH);
			return null;
		}

		// Execute the script in a separate thread to avoid blocking the UI
		Thread scriptThread = new Thread(() -> {
			try {
				ProcessBuilder pb = new ProcessBuilder(SCRIPT_PATH);
				pb.directory(scriptFile.getParentFile());
				pb.redirectErrorStream(true);

				Process process = pb.start();

				// Read and display output
				StringBuilder output = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(process.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						output.append(line).append("\n");
						System.out.println(line);
					}
				}

				int exitCode = process.waitFor();

				final String message;
				if (exitCode == 0) {
					message = "RCP build script executed successfully.";
				} else {
					message = "RCP build script failed with exit code: " + exitCode +
							"\n\nOutput:\n" + output.toString();
				}

				Display.getDefault().asyncExec(() -> {
					MessageDialog.openInformation(
						Display.getDefault().getActiveShell(),
						"Build RCP Script",
						message
					);
				});

			} catch (IOException e) {
				showError("Error executing script: " + e.getMessage());
			} catch (InterruptedException e) {
				showError("Script execution interrupted: " + e.getMessage());
				Thread.currentThread().interrupt();
			}
		});

		scriptThread.setDaemon(true);
		scriptThread.start();

		return null;
	}

	private void showError(String message) {
		Display.getDefault().asyncExec(() -> {
			MessageDialog.openError(
				Display.getDefault().getActiveShell(),
				"Build RCP Script Error",
				message
			);
		});
	}
}
