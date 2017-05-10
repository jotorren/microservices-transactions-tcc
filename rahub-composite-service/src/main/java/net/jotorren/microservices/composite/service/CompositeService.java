package net.jotorren.microservices.composite.service;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;

import net.jotorren.microservices.composite.domain.CompositeData;
import net.jotorren.microservices.tcc.TccRestCoordinator;
import net.jotorren.microservices.tx.CompositeTransactionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompositeService {

	@Autowired
	private TccRestCoordinator tccRestCoordinator;

	public Entry<String, List<String>> saveAllEntities(CompositeData data) throws CompositeTransactionException {
		String txId = UUID.randomUUID().toString();
		
		return new AbstractMap.SimpleEntry<String, List<String>>(txId, 
				Arrays.asList("uri-source-code", "uri-forum"));
	}
}
