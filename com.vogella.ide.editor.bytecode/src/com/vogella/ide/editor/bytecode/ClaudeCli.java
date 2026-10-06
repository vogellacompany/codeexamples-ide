package com.vogella.ide.editor.bytecode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/** Runs the Claude Code CLI in print mode and returns its answer. */
public final class ClaudeCli {

	private static final String SYSTEM_PROMPT = """
			You translate JVM bytecode, as printed by the Eclipse JDT class file disassembler, \
			into idiomatic source code of the requested programming language. \
			Reconstruct the algorithm behind the instructions, choose meaningful names \
			and use the idioms and standard library of the target language. \
			Answer with the source code only, no markdown fences and no explanation outside of code comments.""";

	private ClaudeCli() {
	}

	public static String translate(String bytecode, String language, IProgressMonitor monitor)
			throws IOException, InterruptedException {
		Path executable = findExecutable();
		List<String> command = List.of(executable.toString(), "-p",
				// a pure translation needs no file or shell access
				"--tools", "",
				"--no-session-persistence",
				"--system-prompt", SYSTEM_PROMPT,
				"Translate this bytecode to " + language + ".");

		var builder = new ProcessBuilder(command);
		// keeps the CLAUDE.md of the workspace project out of the context
		builder.directory(new File(System.getProperty("java.io.tmpdir")));
		// an IDE started from the desktop lacks the shell PATH, which an npm installed claude needs to find node
		builder.environment().merge("PATH", searchPath(executable), (path, extra) -> extra + File.pathSeparator + path);

		Process process = builder.start();
		CompletableFuture<String> output = readAsync(process.getInputStream());
		CompletableFuture<String> errors = readAsync(process.getErrorStream());
		try (OutputStream stdin = process.getOutputStream()) {
			stdin.write(bytecode.getBytes(StandardCharsets.UTF_8));
		}
		while (!process.waitFor(200, TimeUnit.MILLISECONDS)) {
			if (monitor.isCanceled()) {
				process.descendants().forEach(ProcessHandle::destroy);
				process.destroy();
				throw new OperationCanceledException();
			}
		}
		if (process.exitValue() != 0) {
			throw new IOException("claude exited with " + process.exitValue() + ": " + errors.join() + output.join());
		}
		return stripMarkdownFence(output.join());
	}

	static Path findExecutable() throws IOException {
		String configured = System.getProperty("claude.executable", System.getenv("CLAUDE_EXECUTABLE"));
		if (configured != null && !configured.isBlank()) {
			return Path.of(configured);
		}
		boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
		List<String> names = windows ? List.of("claude.exe", "claude.cmd") : List.of("claude");
		List<Path> directories = new ArrayList<>();
		String path = System.getenv("PATH");
		if (path != null) {
			for (String entry : path.split(File.pathSeparator)) {
				directories.add(Path.of(entry));
			}
		}
		Path home = Path.of(System.getProperty("user.home"));
		directories.add(home.resolve(".local/bin"));
		directories.add(home.resolve(".claude/local"));
		directories.add(Path.of("/opt/homebrew/bin"));
		directories.add(Path.of("/usr/local/bin"));
		String appData = System.getenv("APPDATA");
		if (appData != null) {
			directories.add(Path.of(appData, "npm"));
		}
		for (Path directory : directories) {
			for (String name : names) {
				Path candidate = directory.resolve(name);
				if (Files.isExecutable(candidate)) {
					return candidate;
				}
			}
		}
		throw new IOException("Claude Code CLI not found, install it or start Eclipse with -Dclaude.executable=<path>");
	}

	private static String searchPath(Path executable) {
		String home = System.getProperty("user.home");
		return String.join(File.pathSeparator, executable.getParent().toString(), home + "/.local/bin",
				"/opt/homebrew/bin", "/usr/local/bin");
	}

	private static CompletableFuture<String> readAsync(InputStream stream) {
		return CompletableFuture.supplyAsync(() -> {
			try (stream) {
				return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				return e.getMessage();
			}
		});
	}

	static String stripMarkdownFence(String answer) {
		String trimmed = answer.strip();
		if (trimmed.startsWith("```") && trimmed.endsWith("```") && trimmed.length() > 6) {
			int firstLineEnd = trimmed.indexOf('\n');
			if (firstLineEnd > 0) {
				trimmed = trimmed.substring(firstLineEnd + 1, trimmed.length() - 3).strip();
			}
		}
		return trimmed + System.lineSeparator();
	}
}
