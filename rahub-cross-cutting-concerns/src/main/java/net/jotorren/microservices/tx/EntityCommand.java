package net.jotorren.microservices.tx;

import java.io.Serializable;

public class EntityCommand<T> implements Serializable{
	private static final long serialVersionUID = -3927305961526601453L;

	public enum Action {
		INSERT, UPDATE, DELETE, QUERY
	}

	private T entity;
	private Action action;
	private String transactionId;
	private long timestamp;
	
	public T getEntity() {
		return entity;
	}

	public void setEntity(T entity) {
		this.entity = entity;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
