package com.vogella.ide.iconreplacer;

import java.net.URL;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.Bundle;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class IconReplacerHandler extends AbstractHandler {

	private static final String EXTENSION_POINT_ID = "com.vogella.ide.iconreplacer.iconpack";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event);

		boolean confirmed = MessageDialog.openConfirm(shell,
				"Apply Icon Pack",
				"This will patch bundle JARs and restart Eclipse. Continue?");
		if (!confirmed) {
			return null;
		}

		try {
			IConfigurationElement[] elements =
					Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_POINT_ID);

			if (elements.length == 0) {
				MessageDialog.openInformation(shell, "Apply Icon Pack",
						"No icon pack contributions found.");
				return null;
			}

			for (IConfigurationElement element : elements) {
				Bundle contributor = Platform.getBundle(element.getContributor().getName());
				String mappingFile = element.getAttribute("mappingFile");
				String iconFolder = element.getAttribute("iconFolder");

				URL mappingUrl = contributor.getEntry(mappingFile);
				URL iconFolderUrl = contributor.getEntry(iconFolder);

				if (mappingUrl == null) {
					throw new IllegalArgumentException(
							"Cannot find mappingFile '" + mappingFile + "' in bundle "
									+ contributor.getSymbolicName());
				}
				if (iconFolderUrl == null) {
					throw new IllegalArgumentException(
							"Cannot find iconFolder '" + iconFolder + "' in bundle "
									+ contributor.getSymbolicName());
				}

				BundlePatcher.apply(mappingUrl, iconFolderUrl);
			}

			RestartHelper.restartWithClean();

		} catch (Exception e) {
			MessageDialog.openError(shell, "Icon Replacement Failed", e.getMessage());
		}

		return null;
	}
}
