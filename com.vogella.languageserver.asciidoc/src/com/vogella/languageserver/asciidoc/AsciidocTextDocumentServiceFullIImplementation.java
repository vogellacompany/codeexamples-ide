package com.vogella.languageserver.asciidoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import com.vogella.languageserver.asciidoc.AsciidocDocumentModel.Route;

public class AsciidocTextDocumentServiceFullIImplementation implements TextDocumentService {

	private final Map<String, AsciidocDocumentModel> docs = Collections.synchronizedMap(new HashMap<>());
	private final AsciidocLanguageServer languageServer;

	public AsciidocTextDocumentServiceFullIImplementation(AsciidocLanguageServer languageServer) {
		this.languageServer = languageServer;
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		return CompletableFuture.supplyAsync(() -> {
			// Map the entries from suggestions to CompletionItem objects
			List<CompletionItem> completionItems = AsciidocElements.INSTANCE.suggestions.entrySet().stream()
					.map(entry -> {
						CompletionItem item = new CompletionItem();
						item.setLabel(entry.getKey()); // Use the key as the label
						item.setInsertText(entry.getValue() + "\n$0"); // Add the placeholder cursor at the appropriate
																		// position
						item.setInsertTextFormat(InsertTextFormat.Snippet); // Use snippet format to support cursor
																			// placeholders

						item.setInsertText(entry.getValue()); // Use the value (template) as the insert text
						return item;
					}).collect(Collectors.toList());

			// Return the result wrapped in Either.forLeft
			return Either.forLeft(completionItems);
		});
	}

	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		return null;
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		return CompletableFuture.supplyAsync(() -> {
			AsciidocDocumentModel doc = docs.get(params.getTextDocument().getUri());
			Hover res = new Hover();
			res.setContents(doc.getResolvedRoutes().stream()
					.filter(route -> route.line == params.getPosition().getLine()).map(route -> route.name)
					.map(EclipseConMap.INSTANCE.type::get).map(this::getHoverContent).collect(Collectors.toList()));
			return res;
		});
	}

	private Either<String, MarkedString> getHoverContent(String type) {

		if ("Beginner".equals(type)) {
			return Either.forLeft("<font color='green'>Beginner</font>");
		} else if ("Intermediate".equals(type)) {
			return Either.forLeft("<font color='blue'>Intermediate</font>");
		} else if ("Advanced".equals(type)) {
			return Either.forLeft("<font color='red'>Advanced</font>");
		}
		return Either.forLeft(type);
	}

//	@Override
//	public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams position) {
//		return null;
//	}
//
//	@Override
//	public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams position) {
//		return CompletableFuture.supplyAsync(() -> {
//			EclipseConDocumentModel doc = docs.get(position.getTextDocument().getUri());
//			String variable = doc.getVariable(position.getPosition().getLine(), position.getPosition().getCharacter());
//			if (variable != null) {
//				int variableLine = doc.getDefintionLine(variable);
//				if (variableLine == -1) {
//					return Collections.emptyList();
//				}
//				Location location = new Location(position.getTextDocument().getUri(), new Range(
//					new Position(variableLine, 0),
//					new Position(variableLine, variable.length())
//					));
//				return Collections.singletonList(location);
//			}
//			return null;
//		});
//	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
			DefinitionParams params) {
		return TextDocumentService.super.definition(params);
	}

	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		return CompletableFuture.supplyAsync(() -> {
			AsciidocDocumentModel doc = docs.get(params.getTextDocument().getUri());
			String variable = doc.getVariable(params.getPosition().getLine(), params.getPosition().getCharacter());
			if (variable != null) {
				return doc.getResolvedRoutes().stream().filter(
						route -> route.text.contains("${" + variable + "}") || route.text.startsWith(variable + "="))
						.map(route -> new Location(params.getTextDocument().getUri(),
								new Range(new Position(route.line, route.text.indexOf(variable)),
										new Position(route.line, route.text.indexOf(variable) + variable.length()))))
						.collect(Collectors.toList());
			}
			String routeName = doc.getResolvedRoutes().stream()
					.filter(route -> route.line == params.getPosition().getLine()).collect(Collectors.toList())
					.get(0).name;
			return doc.getResolvedRoutes().stream().filter(route -> route.name.equals(routeName))
					.map(route -> new Location(params.getTextDocument().getUri(),
							new Range(new Position(route.line, 0), new Position(route.line, route.text.length()))))
					.collect(Collectors.toList());
		});
	}

//	@Override
//	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(TextDocumentPositionParams position) {
//		return null;
//	}
	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		AsciidocDocumentModel model = new AsciidocDocumentModel(params.getTextDocument().getText());
		this.docs.put(params.getTextDocument().getUri(), model);
		CompletableFuture.runAsync(() -> languageServer.client
				.publishDiagnostics(new PublishDiagnosticsParams(params.getTextDocument().getUri(), validate(model))));
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		AsciidocDocumentModel model = new AsciidocDocumentModel(params.getContentChanges().get(0).getText());
		this.docs.put(params.getTextDocument().getUri(), model);
		// send notification
		CompletableFuture.runAsync(() -> languageServer.client
				.publishDiagnostics(new PublishDiagnosticsParams(params.getTextDocument().getUri(), validate(model))));
	}

	private List<Diagnostic> validate(AsciidocDocumentModel model) {
		List<Diagnostic> res = new ArrayList<>();
		Route previousRoute = null;
		for (Route route : model.getResolvedRoutes()) {
			if (!EclipseConMap.INSTANCE.all.contains(route.name)) {
				Diagnostic diagnostic = new Diagnostic();
				diagnostic.setSeverity(DiagnosticSeverity.Error);
				diagnostic.setMessage("This is not a Session");
				diagnostic.setRange(new Range(new Position(route.line, route.charOffset),
						new Position(route.line, route.charOffset + route.text.length())));
				res.add(diagnostic);
			} else if (previousRoute != null && !EclipseConMap.INSTANCE.startsFrom(route.name, previousRoute.name)) {
				Diagnostic diagnostic = new Diagnostic();
				diagnostic.setSeverity(DiagnosticSeverity.Warning);
				diagnostic.setMessage("'" + route.name + "' does not follow '" + previousRoute.name + "'");
				diagnostic.setRange(new Range(new Position(route.line, route.charOffset),
						new Position(route.line, route.charOffset + route.text.length())));
				res.add(diagnostic);
			}
			previousRoute = route;
		}
		return res;
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		this.docs.remove(params.getTextDocument().getUri());
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
	}

	@Override
	public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
		return null;
	}

	@Override
	public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
		return null;
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		return null;
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
		return null;
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
		return null;
	}

	@Override
	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		return null;
	}
	
	private List<Diagnostic> validate(AsciidocDocumentModel model) {
	    List<Diagnostic> diagnostics = new ArrayList<>();
	    
	    // Simulate finding a placeholder issue
	    for (int i = 0; i < model.getResolvedLines().size(); i++) {
	        String line = model.getLines().get(i);
	        int index = line.indexOf("PLACEHOLDER_TEXT");
	        if (index != -1) {
	            // Create a diagnostic for the placeholder text issue
	            Diagnostic diagnostic = new Diagnostic();
	            diagnostic.setSeverity(DiagnosticSeverity.Warning);
	            diagnostic.setMessage("Found placeholder text that should be replaced.");
	            diagnostic.setCode("placeholder.text.issue");
	            diagnostic.setRange(new Range(
	                    new Position(i, index), 
	                    new Position(i, index + "PLACEHOLDER_TEXT".length())));
	            diagnostics.add(diagnostic);
	        }
	    }

	    return diagnostics;
	}


}
