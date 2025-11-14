package z.ex.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

public class BuildRCPScriptHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Read paths from preferences
		IPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, "z.ex.search");
		String scriptPath = store.getString(PreferenceConstants.SCRIPT_PATH);
		String pdfOutputPath = store.getString(PreferenceConstants.PDF_OUTPUT_PATH);

		File scriptFile = new File(scriptPath);

		if (!scriptFile.exists()) {
			showError("Script not found at: " + scriptPath);
			return null;
		}

		if (!scriptFile.canExecute()) {
			showError("Script is not executable: " + scriptPath);
			return null;
		}

		// Execute the script using Eclipse Jobs API
		Job job = new Job("Building RCP") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Executing RCP build script", IProgressMonitor.UNKNOWN);

				try {
					ProcessBuilder pb = new ProcessBuilder(scriptPath);
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
								process.destroyForcibly();
								return Status.CANCEL_STATUS;
							}
						}
					}

					int exitCode = process.waitFor();
					final boolean success = exitCode == 0;

					if (success) {
						showSuccessDialog();
					} else {
						String message = "Build failed with exit code: " + exitCode +
								"\n\n" + output.toString();
						showErrorDialog(message);
					}

					return Status.OK_STATUS;

				} catch (IOException e) {
					return new Status(IStatus.ERROR, "z.ex.search",
							"Error executing script: " + e.getMessage(), e);
				} catch (InterruptedException e) {
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

	private void showSuccessDialog() {
		Display.getDefault().asyncExec(() -> {
			boolean openPdf = MessageDialog.openQuestion(
				Display.getDefault().getActiveShell(),
				"Build RCP Script",
				"RCP build script executed successfully.\n\nDo you want to open the PDF file?"
			);

			if (openPdf) {
				openPdfFile();
			}
		});
	}

	private void showErrorDialog(String message) {
		Display.getDefault().asyncExec(() -> {
			MessageDialog.openError(
				Display.getDefault().getActiveShell(),
				"Build RCP Script Error",
				message
			);
		});
	}

	private void openPdfFile() {
		// Read PDF path from preferences
		IPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, "z.ex.search");
		String pdfOutputPath = store.getString(PreferenceConstants.PDF_OUTPUT_PATH);

		File pdfFile = new File(pdfOutputPath);

		if (!pdfFile.exists()) {
			showError("PDF file not found at: " + pdfOutputPath);
			return;
		}

		// Use SWT Program to launch the PDF with system default application
		boolean launched = Program.launch(pdfFile.getAbsolutePath());

		if (!launched) {
			showError("Failed to open PDF file. No application is associated with PDF files.");
		}
	}
}
