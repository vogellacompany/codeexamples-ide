package com.vogella.jdt.quickfix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.java.AbstractJavaCompletionProposal;
import org.eclipse.jdt.ui.actions.AddGetterSetterAction;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.StyledString;

@SuppressWarnings("restriction")
public class QuickAssistsProcessorGetterSetter implements IQuickAssistProcessor {
	private boolean hasFields = false;

	@Override
	public boolean hasAssists(IInvocationContext context) throws CoreException {
		// currently hasAssists is not called by JDT
		return true;
	}

	@Override
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations)
			throws CoreException {
		hasFields = false;
		ASTNode coveringNode = context.getCoveringNode();
		if (coveringNode == null) {
			return null;
		}
		// we should have selected a class name
		if (!(coveringNode instanceof SimpleName)) {
			return null;
		}
		// ensure that simple names parent is a type declaration
		if (!(coveringNode.getParent() instanceof TypeDeclaration)) {
			return null;
		}
		coveringNode.getRoot().accept(new ASTVisitor() {
			@Override
			public boolean visit(FieldDeclaration node) {
				hasFields = true;
				return super.visit(node);
			}
		});

		if (!hasFields) {
			return null;
		}

		List<IJavaCompletionProposal> proposals = new ArrayList<>();

		addGetterAndSetterProposal(context, proposals);

		return proposals.toArray(new IJavaCompletionProposal[proposals.size()]);
	}

	private void addGetterAndSetterProposal(IInvocationContext context, List<IJavaCompletionProposal> proposals) {
		proposals.add(new AbstractJavaCompletionProposal() {
			@Override
			public StyledString getStyledDisplayString() {
				ICompilationUnit compilationUnit = context.getCompilationUnit();
				return new StyledString(
						"Generate Getter and setter for " + compilationUnit.findPrimaryType().getElementName());
			}

			@Override
			protected int getPatternMatchRule(String pattern, String string) {
				// override the match rule since we do not work with a pattern, but just want to
				// open the "Generate Getters and Setters..." dialog
				return -1;
			}

			@Override
			public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
				if (context instanceof AssistContext) {
					AssistContext assistContext = (AssistContext) context;
					AddGetterSetterAction addGetterSetterAction = new AddGetterSetterAction(
							(CompilationUnitEditor) assistContext.getEditor());

					addGetterSetterAction.run();
				}
			}
		});
	}
}
