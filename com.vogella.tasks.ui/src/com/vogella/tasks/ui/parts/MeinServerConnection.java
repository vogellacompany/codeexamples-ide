package com.vogella.tasks.ui.parts;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
// OSGi immediate component NOT an OSGi service
public class MeinServerConnection {
	static int counter = 1;

	@Reference
	IServerConnection service;
	
	public MeinServerConnection() {
		
		counter++;
	}
	
	@Override
	public String toString() {
		return String.valueOf(counter);
	}
	
}
