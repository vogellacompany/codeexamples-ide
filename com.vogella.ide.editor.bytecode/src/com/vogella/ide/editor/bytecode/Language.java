package com.vogella.ide.editor.bytecode;

import java.util.List;
import java.util.Locale;

/** A target language for the translation and the file extension of its source files. */
public record Language(String name, String extension) {

	public static final List<Language> DEFAULTS = List.of(
			new Language("Java", "java"),
			new Language("Kotlin", "kt"),
			new Language("Python", "py"),
			new Language("JavaScript", "js"),
			new Language("TypeScript", "ts"),
			new Language("Dart", "dart"),
			new Language("Rust", "rs"),
			new Language("Go", "go"),
			new Language("C#", "cs"),
			new Language("C++", "cpp"),
			new Language("Swift", "swift"),
			new Language("Haskell", "hs"),
			new Language("COBOL", "cbl"));

	public static Language of(String name) {
		return DEFAULTS.stream()
				.filter(l -> l.name().equalsIgnoreCase(name))
				.findFirst()
				.orElseGet(() -> new Language(name, fallbackExtension(name)));
	}

	private static String fallbackExtension(String name) {
		String extension = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
		return extension.isEmpty() ? "txt" : extension;
	}
}
