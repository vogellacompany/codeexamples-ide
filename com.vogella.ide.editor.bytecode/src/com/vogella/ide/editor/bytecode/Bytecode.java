package com.vogella.ide.editor.bytecode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.util.ClassFileBytesDisassembler;
import org.eclipse.jdt.core.util.ClassFormatException;

/** Disassembles the class files of a Java source or class file into a .bytecode file. */
public final class Bytecode {

	private static final String OUTPUT_FOLDER = "bytecode";

	private Bytecode() {
	}

	/** Writes the disassembly of the given .java or .class file into the bytecode folder of its project. */
	public static IFile writeBytecodeFile(IFile file, IProgressMonitor monitor) throws CoreException, IOException {
		List<IFile> classFiles = "class".equals(file.getFileExtension()) ? List.of(file) : classFilesOf(file, monitor);
		if (classFiles.isEmpty()) {
			throw new IOException("No class file found for " + file.getName() + ", is the project built without errors?");
		}
		var disassembler = ToolFactory.createDefaultClassFileBytesDisassembler();
		var text = new StringBuilder();
		for (IFile classFile : classFiles) {
			try (InputStream in = classFile.getContents()) {
				text.append(disassembler.disassemble(in.readAllBytes(), "\n", ClassFileBytesDisassembler.DETAILED));
			} catch (ClassFormatException e) {
				throw new IOException(classFile.getName() + " is not a valid class file", e);
			}
			text.append("\n");
		}
		// outside of the source folders, so that the builder does not copy it into the output folder
		return writeProjectFile(file.getProject(), OUTPUT_FOLDER, baseName(file) + ".bytecode", text.toString(),
				monitor);
	}

	/** Creates or overwrites a file in a top level folder of the project. */
	static IFile writeProjectFile(IProject project, String folderName, String fileName, String content,
			IProgressMonitor monitor) throws CoreException {
		IFolder folder = project.getFolder(folderName);
		if (!folder.exists()) {
			folder.create(true, true, monitor);
		}
		IFile target = folder.getFile(IPath.fromOSString(fileName));
		target.write(content.getBytes(StandardCharsets.UTF_8), true, false, true, monitor);
		return target;
	}

	private static List<IFile> classFilesOf(IFile javaFile, IProgressMonitor monitor) throws CoreException {
		ICompilationUnit unit = JavaCore.createCompilationUnitFrom(javaFile);
		IJavaProject project = unit.getJavaProject();
		if (!project.exists()) {
			return List.of();
		}
		var root = (IPackageFragmentRoot) unit.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		IPath output = root.getRawClasspathEntry().getOutputLocation();
		if (output == null) {
			output = project.getOutputLocation();
		}
		String packagePath = unit.getParent().getElementName().replace('.', '/');
		IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(output.append(packagePath));
		folder.refreshLocal(IResource.DEPTH_ONE, monitor);
		if (!folder.exists()) {
			return List.of();
		}
		String typeName = baseName(javaFile);
		List<IFile> result = new ArrayList<>();
		for (IResource member : folder.members()) {
			String name = member.getName();
			if (member instanceof IFile classFile
					&& (name.equals(typeName + ".class") || name.startsWith(typeName + "$") && name.endsWith(".class"))) {
				result.add(classFile);
			}
		}
		result.sort(Comparator.comparing(IResource::getName));
		return result;
	}

	static String baseName(IFile file) {
		String name = file.getName();
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : name;
	}
}
