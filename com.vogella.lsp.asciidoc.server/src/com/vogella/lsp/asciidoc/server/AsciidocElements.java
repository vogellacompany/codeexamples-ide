package com.vogella.lsp.asciidoc.server;

import java.util.HashMap;
import java.util.Map;

public class AsciidocElements {

	public static final AsciidocElements INSTANCE = new AsciidocElements();

	Map<String, String> suggestions = new HashMap<>();

	public AsciidocElements() {
		String sourceTemplate = """
				[source,java]
				----
				$0
				----
				""";
		suggestions.put("source", sourceTemplate);

	}
}
