package net.jotorren.microservices.tx.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.jotorren.microservices.tx.CompositeTransactionManager;
import net.jotorren.microservices.tx.EntityCommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CompositeTransactionMemoryManagerImpl implements CompositeTransactionManager {

	private static final Logger LOG = LoggerFactory.getLogger(CompositeTransactionManager.class);

	private Map<String, List<EntityCommand>> transactions = new ConcurrentHashMap<String, List<EntityCommand>>();

	@Override
	public void open(String txId) {
		if (transactions.containsKey(txId)) {
			throw new IllegalArgumentException("The composite transaction ["+ txId + "] already exists");
		}
		transactions.put(txId, new ArrayList<EntityCommand>());
	}

	@Override
	public void enlist(String txId, EntityCommand ec) {
		if (!transactions.containsKey(txId)) {
			throw new IllegalArgumentException("The composite transaction ["+ txId + "] does not exist");
		}

		List<EntityCommand> transactionOperations = transactions.get(txId);
		transactionOperations.add(ec);
	}

	@Override
	public List<EntityCommand> fetch(String txId) {
		List<EntityCommand> transactionOperations = transactions.get(txId);
		if (null == transactionOperations) {
			throw new IllegalArgumentException("The composite transaction ["+ txId + "] does not exist");
		}
		
		return transactionOperations;
	}
	
	@Override
	public void close(String txId) {
		List<EntityCommand> transactionOperations = transactions.remove(txId);
		if (null == transactionOperations) {
			LOG.warn("The composite transaction [{}] does not exist", txId);
		}
	}
}
