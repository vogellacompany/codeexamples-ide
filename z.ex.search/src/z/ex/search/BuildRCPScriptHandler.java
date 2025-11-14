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
import org.eclipse.jface.notifications.NotificationPopup;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
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

					Display.getDefault().asyncExec(() -> {
						if (success) {
							showSuccessNotification();
						} else {
							String message = "Build failed with exit code: " + exitCode +
									"\n\n" + output.toString();
							showErrorNotification(message);
						}
					});

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
			showErrorNotification(message);
		});
	}

	private void showSuccessNotification() {
		NotificationPopup.forDisplay(Display.getDefault())
			.content(composite -> {
				composite.setLayout(new org.eclipse.swt.layout.GridLayout(1, false));

				WidgetFactory.label(SWT.WRAP)
					.text("RCP build script executed successfully.")
					.create(composite);

				WidgetFactory.button(SWT.PUSH)
					.text("Open PDF")
					.onSelect(e -> openPdfFile())
					.layoutData(new org.eclipse.swt.layout.GridData(SWT.LEFT, SWT.CENTER, false, false))
					.create(composite);

				return composite;
			})
			.title(composite -> WidgetFactory.label(SWT.NONE)
				.text("Build Successful")
				.create(composite), true)
			.open();
	}

	private void showErrorNotification(String message) {
		NotificationPopup.forDisplay(Display.getDefault())
			.content(composite -> {
				composite.setLayout(new org.eclipse.swt.layout.GridLayout(1, false));

				WidgetFactory.label(SWT.WRAP)
					.text(message.length() > 300 ? message.substring(0, 300) + "..." : message)
					.create(composite);

				return composite;
			})
			.title(composite -> WidgetFactory.label(SWT.NONE)
				.text("Build Script Error")
				.create(composite), true)
			.open();
	}

	private void openPdfFile() {
		// Read PDF path from preferences
		IPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, "z.ex.search");
		String pdfOutputPath = store.getString(PreferenceConstants.PDF_OUTPUT_PATH);

		File pdfFile = new File(pdfOutputPath);

		if (!pdfFile.exists()) {
			showErrorNotification("PDF file not found at: " + pdfOutputPath);
			return;
		}

		// Use SWT Program to launch the PDF with system default application
		boolean launched = Program.launch(pdfFile.getAbsolutePath());

		if (!launched) {
			showErrorNotification("Failed to open PDF file. No application is associated with PDF files.");
		}
	}
}
