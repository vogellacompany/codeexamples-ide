package com.vogella.lsp.asciidoc.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AsciidocDocumentModel {

	// Inner class representing a line of the document
	public static class DocumentLine {
		final int line;
		final String text;

		protected DocumentLine(int line, String text) {
			this.line = line;
			this.text = text;
		}
	}

	// List to store all lines from the document
	private final List<DocumentLine> lines = new ArrayList<>();

	// Constructor to read the text and store lines
	public AsciidocDocumentModel(String text) {
		try (Reader r = new StringReader(text); BufferedReader reader = new BufferedReader(r)) {

			String lineText;
			int lineNumber = 0;
			while ((lineText = reader.readLine()) != null) {
				DocumentLine line = new DocumentLine(lineNumber, lineText);
				lines.add(line);
				lineNumber++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Method to get the content of each line by line number
	public String getLineContent(int lineNumber) {
		if (lineNumber < 0 || lineNumber >= lines.size()) {
			return null; // Return null if the line number is out of range
		}
		return lines.get(lineNumber).text;
	}

	// Method to get all lines as an unmodifiable list
	public List<DocumentLine> getResolvedLines() {
		return Collections.unmodifiableList(this.lines);
	}

	// Method to get a list of all lines as strings
	public List<String> getLines() {
		List<String> result = new ArrayList<>();
		for (DocumentLine line : lines) {
			result.add(line.text);
		}
		return Collections.unmodifiableList(result);
	}
}
