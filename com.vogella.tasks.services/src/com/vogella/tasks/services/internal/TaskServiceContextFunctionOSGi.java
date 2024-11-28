package com.vogella.tasks.services.internal;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.vogella.tasks.model.TaskService;

public class TaskServiceContextFunctionOSGi extends ContextFunction {
	@Override
	public Object compute(IEclipseContext context, String contextKey) {

		// create instance of TaskService with dependency injection
		TaskService taskService = ContextInjectionFactory.make(TransientTaskServiceImpl.class, context);

		// add instance of TaskService to context so that
//		// test next caller gets the same instance
//		MApplication app = context.get(MApplication.class);
//		IEclipseContext appCtx = app.getContext();
//		appCtx.set(TaskService.class, taskService);

		// in case the TaskService is also needed in the OSGi layer, e.g.
		// by other OSGi services, register the instance also in the OSGi service layer
		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
		BundleContext bundleContext = bundle.getBundleContext();
		bundleContext.registerService(TaskService.class, taskService, null);

		// return model for current invocation
		// next invocation uses object from application context
		return taskService;
	}
}
