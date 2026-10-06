package com.vogella.adapters;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IResource;

import com.vogella.tasks.model.Task;

/**
 * Remembers the resource linked to a task, keyed by task id, as the task model has no resource field.
 */
public final class TaskResources {

	private static final Map<Long, IResource> RESOURCES = new ConcurrentHashMap<>();

	private TaskResources() {
	}

	public static IResource get(Task task) {
		return RESOURCES.get(task.getId());
	}

	public static void set(Task task, IResource resource) {
		if (resource == null) {
			RESOURCES.remove(task.getId());
		} else {
			RESOURCES.put(task.getId(), resource);
		}
	}
}
