package com.vogella.tasks.ui.parts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.fieldassist.AutoCompleteField;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.vogella.tasks.model.TaskService;
import com.vogella.tasks.ui.parts.contentassists.TaskContentProposal;
import com.vogella.tasks.ui.parts.contentassists.TaskContentProposalProvider;

public class AutoCompleteFieldPart {
	
	private Path lastDir;

	@Inject
	TaskService taskService;

	@PostConstruct
	public void createControls(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);

		Combo combo = new Combo(parent, SWT.NONE);
		AutoCompleteField autoCompleteField = new AutoCompleteField(combo, new ComboContentAdapter());

		combo.addModifyListener(e -> {
			Path dir = getPathWithoutFileName(combo.getText());
			if (dir == null || dir.equals(lastDir) || !isDirectory(dir)) {
				return;
			}
			lastDir = dir;
			try (Stream<Path> paths = Files.list(dir)) {
				List<String> directories = filterPaths(paths);
				autoCompleteField.setProposals(directories.toArray(new String[directories.size()]));
			} catch (IOException ex) {
				// ignoreO
			}
		});
		Text text = WidgetFactory.text(SWT.BORDER)
				.layoutData(GridDataFactory.fillDefaults().grab(true, false).create())
				.create(parent);
		TaskContentProposalProvider taskContentProposalProvider = new TaskContentProposalProvider(new ArrayList<>());
		ContentProposalAdapter contentProposal = new ContentProposalAdapter(text,
				new TextContentAdapter(), taskContentProposalProvider, null, null);

		contentProposal.addContentProposalListener(proposal -> {
			TaskContentProposal p = (TaskContentProposal) proposal;
			System.out.println(p.getTask().getSummary());
		});
		contentProposal.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);

		WidgetFactory.button(SWT.PUSH).onSelect(e -> {
			taskContentProposalProvider.setProposals(taskService.getAll());
			contentProposal.refresh();

			// This will work as of the 2020-12 release
//			contentProposal.openProposalPopup();
		}).text("Press to add the content proposals from service").create(parent);
	}

	private Path getPathWithoutFileName(String inputPath) {
		int lastIndex = inputPath.lastIndexOf(File.separatorChar);
		if (separatorNotFound(lastIndex)) {
			return null;
		} else if (endsWithSeparator(inputPath, lastIndex)) {
			return getPath(inputPath);
		} else {
			return getPath(removeFileName(inputPath, lastIndex));
		}
	}

	private boolean separatorNotFound(int lastIndex) {
		return lastIndex < 0;
	}

	private boolean endsWithSeparator(String inputPath, int lastIndex) {
		return lastIndex == inputPath.length();
	}

	private String removeFileName(String text, int lastIndex) {
		if (lastIndex == 0) {
			return File.separator;
		} else {
			return text.substring(0, lastIndex);
		}
	}

	private Path getPath(String text) {
		try {
			return Paths.get(text);
		} catch (InvalidPathException ex) {
			return null;
		}
	}

	private boolean isDirectory(Path dir) {
		try {
			return Files.isDirectory(dir);
		} catch (SecurityException ex) {
			return false;
		}
	}

	private List<String> filterPaths(Stream<Path> paths) {
		return paths.filter(path -> {
			String[] directoriesInPath = path.toString().split(File.separator);
			String fileName = directoriesInPath[directoriesInPath.length - 1];
			String lastDirectory = directoriesInPath[directoriesInPath.length - 2];
			return !lastDirectory.equals(".") && !fileName.startsWith(".") && Files.isDirectory(path);
		}).map(Path::toString).collect(Collectors.toList());
	}
}
