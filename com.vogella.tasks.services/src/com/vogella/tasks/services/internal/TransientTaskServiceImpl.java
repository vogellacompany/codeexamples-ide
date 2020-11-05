package com.vogella.tasks.services.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.e4.core.services.events.IEventBroker;

import com.vogella.tasks.events.TaskEventConstants;
import com.vogella.tasks.model.Task;
import com.vogella.tasks.model.TaskService;

public class TransientTaskServiceImpl implements TaskService {

	@Inject // <.>
	private IEventBroker broker;

	private static AtomicInteger current = new AtomicInteger(1);
	private List<Task> tasks;

	public TransientTaskServiceImpl() {
		tasks = createTestData();
	}

	@Override
	public void consume(Consumer<List<Task>> taskConsumer) {
		// Simulate Server access delay
//		try {
//			TimeUnit.SECONDS.sleep(5);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		// always pass a new copy of the data
		taskConsumer.accept(tasks.stream().map(Task::copy).collect(Collectors.toList()));
	}


	// create or update an existing instance of object
	@Override
	public synchronized boolean update(Task newTask) {
		// hold the Optional object as reference to determine, if the object is
		// newly created or not
		Optional<Task> taskOptional = findById(newTask.getId());

		// get the actual object or create a new one
		Task task = taskOptional.orElse(new Task(current.getAndIncrement()));
		task.setSummary(newTask.getSummary());
		task.setDescription(newTask.getDescription());
		task.setDone(newTask.isDone());
		task.setDueDate(newTask.getDueDate());

		if (!taskOptional.isPresent()) {
			tasks.add(task);
			broker.post(TaskEventConstants.TOPIC_TASKS_NEW,
					Map.of(TaskEventConstants.TOPIC_TASKS_NEW, TaskEventConstants.TOPIC_TASKS_NEW, Task.FIELD_ID,
							task.getId())); // <.>
		} else {
			broker.post(TaskEventConstants.TOPIC_TASKS_UPDATE, Map.of(TaskEventConstants.TOPIC_TASKS,
					TaskEventConstants.TOPIC_TASKS_UPDATE, Task.FIELD_ID, task.getId())); // <.>
		}
		return true;
	}

	@Override
	public Optional<Task> get(long id) {
		return findById(id).map(Task::copy);
	}

	@Override
	public boolean delete(long id) {
		Optional<Task> deletedTask = findById(id);
		deletedTask.ifPresent(t -> {
			tasks.remove(t);
			broker.post(TaskEventConstants.TOPIC_TASKS_DELETE, Map.of(TaskEventConstants.TOPIC_TASKS,
					TaskEventConstants.TOPIC_TASKS_DELETE, Task.FIELD_ID, t.getId())); // <.>
		});


		return deletedTask.isPresent();
	}

	// Example data, change if you like
	private List<Task> createTestData() {
		List<Task> list = List.of(create("Application model", "Flexible and extensible"),
				create("DI", "@Inject as programming mode"), create("OSGi", "Services"),
				create("SWT", "Widgets"), create("JFace", "Especially Viewers!"),
				create("CSS Styling", "Style your application"),
				create("Eclipse services", "Selection, model, Part"),
				create("Renderer", "Different UI toolkit"), create("Compatibility Layer", "Run Eclipse 3.x"));
		return new ArrayList<>(list);
	}

	private Task create(String summary, String description) {
		return new Task(current.getAndIncrement(), summary, description, false, LocalDate.now());
	}

	private Optional<Task> findById(long id) {
		return tasks.stream().filter(t -> t.getId() == id).findAny();
	}

	@Override
	public List<Task> getAll() {
		return tasks.stream().map(Task::copy).collect(Collectors.toList());
	}

}
