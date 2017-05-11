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
	
	public String getPartialTransactionId(int index) {
		if (index < 0 || null == participantLinks || index >= participantLinks.size()){
			return null;
		}
		
		String participantUri = participantLinks.get(index).getUri();
		return participantUri.substring(participantUri.lastIndexOf("/") + 1);
	}
}
