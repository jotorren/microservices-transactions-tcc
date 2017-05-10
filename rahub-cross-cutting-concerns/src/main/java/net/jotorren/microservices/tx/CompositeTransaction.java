package net.jotorren.microservices.tx;

import com.atomikos.tcc.rest.Transaction;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class CompositeTransaction extends Transaction {

	@JsonIgnore
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
