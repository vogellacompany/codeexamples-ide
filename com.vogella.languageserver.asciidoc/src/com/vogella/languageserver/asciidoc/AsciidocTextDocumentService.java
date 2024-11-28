package com.vogella.languageserver.asciidoc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

public class AsciidocTextDocumentService implements TextDocumentService {
	private final Map<String, AsciidocDocumentModel> docs = Collections.synchronizedMap(new HashMap<>());

	private final AsciidocLanguageServer languageServer;

	public AsciidocTextDocumentService(AsciidocLanguageServer languageServer) {
		this.languageServer = languageServer;
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		return CompletableFuture.supplyAsync(() -> {
			// Example: Provide completions for AsciiDoc elements

			CompletionItem item1 = new CompletionItem();
			item1.setLabel("image::");

			CompletionItem item2 = new CompletionItem();
			item2.setLabel("include::");
			List<CompletionItem> completionItems = List.of(item1, item2);


//			// Map the entries from suggestions to CompletionItem objects
//			List<CompletionItem> completionItems = AsciidocElements.INSTANCE.suggestions.entrySet().stream()
//					.map(entry -> {
//						CompletionItem item = new CompletionItem();
//						item.setLabel(entry.getKey()); // Use the key as the label
//						item.setInsertText(entry.getValue() + "\n$0"); // Add the placeholder cursor at the appropriate
//																		// position
//						item.setInsertTextFormat(InsertTextFormat.Snippet); // Use snippet format to support cursor
//																			// placeholders
//
//						item.setInsertText(entry.getValue()); // Use the value (template) as the insert text
//						return item;
//					}).collect(Collectors.toList());

			// Return the result wrapped in Either.forLeft
			return Either.forLeft(completionItems);
		});
	}


	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		return null;
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
			DefinitionParams params) {
		return null;
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

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		AsciidocDocumentModel model = new AsciidocDocumentModel(params.getTextDocument().getText());
		this.docs.put(params.getTextDocument().getUri(), model);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		AsciidocDocumentModel model = new AsciidocDocumentModel(params.getContentChanges().get(0).getText());
		this.docs.put(params.getTextDocument().getUri(), model);
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		this.docs.remove(params.getTextDocument().getUri());
	}
	
	@Override
	public void didSave(DidSaveTextDocumentParams params) {
	}
}
