package com.vogella.tasks.ui.parts;

import org.osgi.service.component.annotations.Component;

@Component
public class ServerConnection implements IServerConnection {
	static int counter = 1;

	public ServerConnection() {

		counter++;
	}

	@Override
	public String toString() {
		return String.valueOf(counter);
	}

}
