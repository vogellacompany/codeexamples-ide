package z.ex.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;

public class BuildRCPScriptHandler extends AbstractHandler {

	// Hard-coded paths (intentional) - point to user's home directory
	private static final String SCRIPT_PATH = Paths.get(System.getProperty("user.home"),
			"git", "content", "_scripts", "buildRCPScript.sh").toString();
	private static final String PDF_OUTPUT_PATH = Paths.get(System.getProperty("user.home"),
			"git", "content", "output.pdf").toString();

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

		// Execute the script using Eclipse Jobs API
		Job job = new Job("Building RCP") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Executing RCP build script", IProgressMonitor.UNKNOWN);

				try {
					ProcessBuilder pb = new ProcessBuilder(SCRIPT_PATH);
					pb.directory(scriptFile.getParentFile());
					pb.redirectErrorStream(true);

					Process process = pb.start();

					// Read and collect output
					StringBuilder output = new StringBuilder();
					try (BufferedReader reader = new BufferedReader(
							new InputStreamReader(process.getInputStream()))) {
						String line;
						while ((line = reader.readLine()) != null) {
							output.append(line).append("\n");
							if (monitor.isCanceled()) {
								process.destroy();
								return Status.CANCEL_STATUS;
							}
						}
					}

					int exitCode = process.waitFor();
					final boolean success = exitCode == 0;

					Display.getDefault().asyncExec(() -> {
						if (success) {
							boolean openPdf = MessageDialog.openQuestion(
								Display.getDefault().getActiveShell(),
								"Build RCP Script",
								"RCP build script executed successfully.\n\nDo you want to open the PDF file?"
							);

							if (openPdf) {
								openPdfFile();
							}
						} else {
							String message = "RCP build script failed with exit code: " + exitCode +
									"\n\nOutput:\n" + output.toString();
							MessageDialog.openError(
								Display.getDefault().getActiveShell(),
								"Build RCP Script Error",
								message
							);
						}
					});

					return Status.OK_STATUS;

				} catch (IOException e) {
					showError("Error executing script: " + e.getMessage());
					return new Status(IStatus.ERROR, "z.ex.search",
							"Error executing script", e);
				} catch (InterruptedException e) {
					showError("Script execution interrupted: " + e.getMessage());
					Thread.currentThread().interrupt();
					return Status.CANCEL_STATUS;
				} finally {
					monitor.done();
				}
			}
		};

		job.setUser(true); // Shows the job in the UI
		job.schedule();

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

	private void openPdfFile() {
		File pdfFile = new File(PDF_OUTPUT_PATH);

		if (!pdfFile.exists()) {
			showError("PDF file not found at: " + PDF_OUTPUT_PATH);
			return;
		}

		// Use SWT Program to launch the PDF with system default application
		boolean launched = Program.launch(pdfFile.getAbsolutePath());

		if (!launched) {
			showError("Failed to open PDF file. No application is associated with PDF files.");
		}
	}
}
