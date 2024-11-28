package com.vogella.lsp.asciidoc.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public class AsciidocDocumentModel {

	public static abstract class DocumentLine {
		final int line;
		final String text;
		final int charOffset;

		protected DocumentLine(int line, int charOffset, String text) {
			this.line = line;
			this.charOffset = charOffset;
			this.text = text;
		}
	}
	public class Header extends DocumentLine{
	    private int startLine;
	    private int startColumn;
	    private int endLine;
	    private int endColumn;
	    private String name;

	    // Constructor
	    public Header(int startLine, int startColumn, int endLine, int endColumn, String name) {
	        super(endLine, endColumn, name);
	    	this.startLine = startLine;
	        this.startColumn = startColumn;
	        this.endLine = endLine;
	        this.endColumn = endColumn;
	        this.name = name;
	    }

	    // Getters and setters
	    public int getStartLine() {
	        return startLine;
	    }

	    public void setStartLine(int startLine) {
	        this.startLine = startLine;
	    }

	    public int getStartColumn() {
	        return startColumn;
	    }

	    public void setStartColumn(int startColumn) {
	        this.startColumn = startColumn;
	    }

	    public int getEndLine() {
	        return endLine;
	    }

	    public void setEndLine(int endLine) {
	        this.endLine = endLine;
	    }

	    public int getEndColumn() {
	        return endColumn;
	    }

	    public void setEndColumn(int endColumn) {
	        this.endColumn = endColumn;
	    }

	    public String getName() {
	        return name;
	    }

	    public void setName(String name) {
	        this.name = name;
	    }
	}

	public static class Route extends DocumentLine {
		final String name;

		public Route(int line, int charOffset, String text, String name) {
			super(line, charOffset, text);
			this.name = name;
		}
	}

	public static class VariableDefinition extends DocumentLine {
		final String variableName;
		final String variableValue;

		public VariableDefinition(int lineNumber, int charOffset, String text, String variableName, String value) {
			super(lineNumber, charOffset, text);
			this.variableName = variableName;
			variableValue = value;
		}

	}

	private final List<DocumentLine> lines = new ArrayList<>();
	private final List<Route> routes = new ArrayList<>();
	private final List<Header> headers = new ArrayList<>();
	private final Map<String, VariableDefinition> variables = new HashMap<>();
	public AsciidocDocumentModel(String text) {
	    try (Reader r = new StringReader(text);
	         BufferedReader reader = new BufferedReader(r)) {

	        String lineText;
	        int lineNumber = 0;
	        while ((lineText = reader.readLine()) != null) {
	            DocumentLine line = null;

	            // Detect header (starting with '=' sign)
	            if (lineText.startsWith("=")) {
	                // Find the start and end column positions
	                int startColumn = 0;
	                while (startColumn < lineText.length() && Character.isWhitespace(lineText.charAt(startColumn))) {
	                    startColumn++;
	                }

	                int endColumn = lineText.length();  // Assuming the header goes to the end of the line
	                Header header = new Header(lineNumber, startColumn, lineNumber, endColumn, lineText.trim());
	                headers.add(header);
	                line = header;
	            } else if (!lineText.trim().isEmpty()) {
	                // If not header, it's a route or other content
	                Route route = new Route(lineNumber, 0, lineText, resolve(lineText));
	                routes.add(route);
	                line = route;
	            }

	            if (line != null) {
	                lines.add(line);
	            }
	            lineNumber++;
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}


	// Helper method to calculate the level of a header
	private int calculateHeaderLevel(String lineText) {
	    int level = 0;
	    while (level < lineText.length() && lineText.charAt(level) == '=') {
	        level++;
	    }
	    return level; // level indicates how deep the header is
	}
	private String resolve(String line) {
		for (Entry<String, VariableDefinition> variable : variables.entrySet()) {
			line = line.replace("${" + variable.getKey() + "}", variable.getValue().variableValue);
		}
		return line;
	}

	private VariableDefinition variableDefinition(int lineNumber, String line) {
		String[] segments = line.split("=");
		if (segments.length == 2) {
			VariableDefinition def = new VariableDefinition(lineNumber, 0, line, segments[0], segments[1]);
			variables.put(def.variableName, def);
			return def;
		}
		return null;
	}

	public List<Route> getResolvedRoutes() {
		return Collections.unmodifiableList(this.routes);
	}
	public List<Header> getHeaders() {
		return Collections.unmodifiableList(this.headers);
	}
	public String getVariable(int lineNumber, int character) {
		Optional<DocumentLine> docLine = this.lines.stream().filter(line -> line.line == lineNumber).findFirst();
		if (!docLine.isPresent()) {
			return null;
		}
		String text = docLine.get().text;
		if (text.contains("=") && character < text.indexOf("=")) {
			return text.split("=")[0];
		}
		int prefix = text.substring(0, character).lastIndexOf("${");
		int suffix = text.indexOf("}", character);
		if (prefix >= 0 && suffix >= 0) {
			return text.substring(prefix + "${".length(), suffix);
		}
		return null;
	}

	public Route getRoute(int line) {
		for (Route route : getResolvedRoutes()) {
			if (route.line == line) {
				return route;
			}
		}
		return null;
	}

	public int getDefintionLine(String variable) {
		if (this.variables.containsKey(variable)) {
			return this.variables.get(variable).line;
		}
		return -1;
	}

	public List<DocumentLine> getResolvedLines() {
		return Collections.unmodifiableList(this.lines);
	}

}
