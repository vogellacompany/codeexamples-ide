package com.vogella.contribute.parts;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import jakarta.annotation.PostConstruct;

public class AdditionalInformationPart {
    @PostConstruct
    public void postConstruct(Composite parent, IEclipseContext context) {
        TextFactory.newText(SWT.BORDER |SWT.MULTI).create(parent);
        // this triggers you access to the active editor
        ContextInjectionFactory.make(ContextAccessExample.class, context);
    }
}