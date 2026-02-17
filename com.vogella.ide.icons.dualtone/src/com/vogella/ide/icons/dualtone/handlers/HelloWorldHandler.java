package com.vogella.ide.icons.dualtone.handlers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jakarta.inject.Named;

public class HelloWorldHandler {

	private static final String UI_BEST_PRACTICES_PROJECT_NAME = "ui-best-practices";
	private static final String ICON_MAPPING_PATH = "iconpacks/eclipse-dual-tone/icon-mapping.json";
	private static final String DUAL_TONE_ICONS_PATH = "iconpacks/eclipse-dual-tone/dual-tone-icons";

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		IProject uiBestPractices = findUiBestPracticesProject(root);
		if (uiBestPractices == null) {
			MessageDialog.openError(shell, "Project Not Found",
					"The '" + UI_BEST_PRACTICES_PROJECT_NAME + "' project is not available in the workspace.\n"
							+ "Please clone and import it from:\n"
							+ "https://github.com/eclipse-platform/ui-best-practices");
			return;
		}

		IFile mappingFile = uiBestPractices.getFile(ICON_MAPPING_PATH);
		if (!mappingFile.exists()) {
			MessageDialog.openError(shell, "Mapping File Not Found",
					"Could not find '" + ICON_MAPPING_PATH + "' in project '" + uiBestPractices.getName() + "'.");
			return;
		}

		Map<String, List<String>> iconMapping;
		try (InputStream is = mappingFile.getContents();
				InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
			Type type = new TypeToken<Map<String, List<String>>>() {
			}.getType();
			iconMapping = new Gson().fromJson(reader, type);
		} catch (Exception e) {
			MessageDialog.openError(shell, "Parse Error",
					"Could not parse '" + ICON_MAPPING_PATH + "': " + e.getMessage());
			return;
		}

		Map<String, IProject> workspacePlugins = buildPluginIdMap(root);

		Job job = Job.create("Applying dual-tone icons to workspace plugins", monitor -> {
			return copyIcons(uiBestPractices, iconMapping, workspacePlugins, monitor, shell);
		});
		job.setUser(true);
		job.schedule();
	}

	private IProject findUiBestPracticesProject(IWorkspaceRoot root) {
		for (IProject project : root.getProjects()) {
			if (project.isOpen() && project.getName().contains(UI_BEST_PRACTICES_PROJECT_NAME)) {
				return project;
			}
		}
		return null;
	}

	private Map<String, IProject> buildPluginIdMap(IWorkspaceRoot root) {
		Map<String, IProject> map = new HashMap<>();
		for (IProject project : root.getProjects()) {
			if (!project.isOpen()) {
				continue;
			}
			map.put(project.getName(), project);
			readBundleSymbolicName(project).ifPresent(bsn -> map.put(bsn, project));
		}
		return map;
	}

	private Optional<String> readBundleSymbolicName(IProject project) {
		IFile manifest = project.getFile("META-INF/MANIFEST.MF");
		if (!manifest.exists()) {
			return Optional.empty();
		}
		try (InputStream is = manifest.getContents()) {
			String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			for (String line : content.split("\\r?\\n")) {
				if (line.startsWith("Bundle-SymbolicName:")) {
					String bsn = line.substring("Bundle-SymbolicName:".length()).trim();
					int semicolonIdx = bsn.indexOf(';');
					if (semicolonIdx != -1) {
						bsn = bsn.substring(0, semicolonIdx).trim();
					}
					return Optional.of(bsn);
				}
			}
		} catch (Exception e) {
			// ignore unreadable manifests
		}
		return Optional.empty();
	}

	private IStatus copyIcons(IProject uiBestPractices, Map<String, List<String>> iconMapping,
			Map<String, IProject> workspacePlugins, IProgressMonitor monitor, Shell shell) {
		IFolder iconsFolder = uiBestPractices.getFolder(DUAL_TONE_ICONS_PATH);
		if (!iconsFolder.exists()) {
			Display.getDefault().asyncExec(() -> {
				if (!shell.isDisposed()) {
					MessageDialog.openError(shell, "Icons Folder Not Found",
							"Could not find '" + DUAL_TONE_ICONS_PATH + "' in project '" + uiBestPractices.getName()
									+ "'.");
				}
			});
			return Status.error("Dual-tone icons folder not found");
		}

		int copied = 0;
		int skipped = 0;

		monitor.beginTask("Copying dual-tone icons", iconMapping.size());

		for (Map.Entry<String, List<String>> entry : iconMapping.entrySet()) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			String sourceIconName = entry.getKey();
			monitor.subTask(sourceIconName);

			IFile sourceFile = iconsFolder.getFile(sourceIconName);
			if (!sourceFile.exists()) {
				skipped++;
				monitor.worked(1);
				continue;
			}

			for (String targetPath : entry.getValue()) {
				int slashIdx = targetPath.indexOf('/');
				if (slashIdx < 0) {
					continue;
				}
				String pluginId = targetPath.substring(0, slashIdx);
				String relativePath = targetPath.substring(slashIdx + 1);

				IProject targetProject = workspacePlugins.get(pluginId);
				if (targetProject == null) {
					continue;
				}

				IFile targetFile = targetProject.getFile(relativePath);
				try {
					ensureParentFolders(targetFile, monitor);
					if (targetFile.exists()) {
						try (InputStream is = sourceFile.getContents()) {
							targetFile.setContents(is, true, true, monitor);
						}
					} else {
						try (InputStream is = sourceFile.getContents()) {
							targetFile.create(is, true, monitor);
						}
					}
					copied++;
				} catch (CoreException | java.io.IOException e) {
					// log and continue with remaining icons
				}
			}
			monitor.worked(1);
		}

		int finalCopied = copied;
		int finalSkipped = skipped;
		Display.getDefault().asyncExec(() -> {
			if (!shell.isDisposed()) {
				MessageDialog.openInformation(shell, "Dual-Tone Icons Applied",
						"Completed icon replacement.\n" + "Icons copied: " + finalCopied + "\n"
								+ "Source icons not in pack (skipped): " + finalSkipped);
			}
		});

		return Status.OK_STATUS;
	}

	private void ensureParentFolders(IFile file, IProgressMonitor monitor) throws CoreException {
		IContainer parent = file.getParent();
		if (parent instanceof IFolder folder && !folder.exists()) {
			ensureFolder(folder, monitor);
		}
	}

	private void ensureFolder(IFolder folder, IProgressMonitor monitor) throws CoreException {
		IContainer parent = folder.getParent();
		if (parent instanceof IFolder parentFolder && !parentFolder.exists()) {
			ensureFolder(parentFolder, monitor);
		}
		if (!folder.exists()) {
			folder.create(true, true, monitor);
		}
	}
}
