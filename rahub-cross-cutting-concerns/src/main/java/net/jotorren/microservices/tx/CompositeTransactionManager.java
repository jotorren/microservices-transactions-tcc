package net.jotorren.microservices.tx;

import java.util.List;

public interface CompositeTransactionManager {

	void open(String txId);
	void enlist(String txId, EntityCommand<?> ec);
	List<EntityCommand<?>> fetch(String txId);
	void close(String txId);
}
