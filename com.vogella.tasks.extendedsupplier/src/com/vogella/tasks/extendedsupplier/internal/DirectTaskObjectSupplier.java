package com.vogella.tasks.extendedsupplier.internal;

import jakarta.inject.Inject;

import org.eclipse.e4.core.di.suppliers.ExtendedObjectSupplier;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.osgi.service.component.annotations.Component;

import com.vogella.tasks.extendedsupplier.DirectTask;
import com.vogella.tasks.model.TaskService;

@Component(service=ExtendedObjectSupplier.class, 
		property = "dependency.injection.annotation=com.vogella.tasks.extendedsupplier.DirectTask")
public class DirectTaskObjectSupplier extends ExtendedObjectSupplier {
	
	@Inject
	private TaskService taskService;
	
	@Override
	public Object get(IObjectDescriptor descriptor, IRequestor requestor, boolean track, boolean group) {
		// get the DirectTodo from the IObjectDescriptor 
		DirectTask uniqueTodo = descriptor.getQualifier(DirectTask.class);
		
		// get the id from the DirectTodo (default is 0)
		long id = uniqueTodo.id();
		
		// get the task, which has the given id or null
		return taskService.get(id);
		
	}
}