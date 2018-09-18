package com.vogella.tasks.services.internal;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.osgi.service.component.annotations.Component;

import com.vogella.tasks.model.ITodoService;

@Component(service = IContextFunction.class, property = "service.context.key=com.vogella.tasks.model.ITodoService")
public class TodoServiceContextFunction extends ContextFunction {
    @Override
    public Object compute(IEclipseContext context, String contextKey) {
		ITodoService todoService = ContextInjectionFactory.make(MyTodoServiceImpl.class, context);

		MApplication app = context.get(MApplication.class);
		IEclipseContext appCtx = app.getContext();
		appCtx.set(ITodoService.class, todoService);

		return todoService;
    }
}