package com.vogella.tasks.model;

import java.util.Date;
import java.util.Objects;

import org.eclipse.core.resources.IResource;

public class Todo {

	public static final String FIELD_RESOURCE = "resource";

	private final long id;
    private String summary = "";
    private String description = "";
    private boolean done = false;
    private Date dueDate = new Date();
	private IResource resource;

    public Todo(long id) {
        this.id = id;
    }

    public Todo(long id, String summary, String description, boolean done, Date dueDate) {
        this.id = id;
        this.summary = summary;
        this.description = description;
        this.done = done;
        setDueDate(dueDate);

    }

    public long getId() {
        return id;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public Date getDueDate() {
        return new Date(dueDate.getTime());
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = new Date(dueDate.getTime());
    }

	public IResource getResource() {
		return resource;
	}

	public void setResource(IResource resource) {
		this.resource = resource;
	}
	@Override
	public int hashCode() {
		return Objects.hash(description, done, dueDate, id, summary);
	}
	public Todo copy() {
	    return new Todo(this.id, this.summary,
	            this.description, this.done,
	            getDueDate());
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Todo other = (Todo) obj;
		return Objects.equals(description, other.description) && done == other.done
				&& Objects.equals(dueDate, other.dueDate) && id == other.id && Objects.equals(summary, other.summary);
	}

	@Override
	public String toString() {
		return "Todo [id=" + id + ", summary=" + summary + ", description=" + description + ", done=" + done
				+ ", dueDate=" + dueDate + "]";
	}
}