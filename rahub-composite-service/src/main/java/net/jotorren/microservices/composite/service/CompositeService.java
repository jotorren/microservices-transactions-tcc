package net.jotorren.microservices.composite.service;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import net.jotorren.microservices.composite.domain.CompositeData;
import net.jotorren.microservices.tcc.TccRestCoordinator;
import net.jotorren.microservices.tx.CompositeTransaction;
import net.jotorren.microservices.tx.CompositeTransactionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CompositeService {

	private static final Logger LOG = LoggerFactory.getLogger(CompositeService.class);
			
	@Autowired
	private TccRestCoordinator tccRestCoordinator;

	@Value("${tcc.rest.transaction.timeout}")
	private long transactionTimeout;

	@Value("${content.service.tcc.url}")
	private String contentServiceTccUrl;
	
	@Value("${forum.service.tcc.url}")
	private String forumServiceTccUrl;
	
	public Entry<String, List<String>> saveAllEntities(CompositeData data) throws CompositeTransactionException {
		Entry<String, List<String>> newObjectsUris = null;
		
		CompositeTransaction transaction = tccRestCoordinator.open(System.currentTimeMillis() + transactionTimeout, 
				contentServiceTccUrl, forumServiceTccUrl);

		Response commitResponse;
		try{
			String contentURI = transaction.getParticipantLinks().get(0).getUri(); // callContentService(contentServiceBaseUrl+"/"+txId, data);
			LOG.info("Step 1: content created [{}]", contentURI);
			
			String forumURI = transaction.getParticipantLinks().get(1).getUri(); // callForumService(forumServiceBaseUrl+"/"+txId, data, contentURI);
			LOG.info("Step 2: forum discussion created [{}]", forumURI);
			
			newObjectsUris = new AbstractMap.SimpleEntry<String, List<String>>(transaction.getId(), Arrays.asList(contentURI, forumURI));
			
			commitResponse = tccRestCoordinator.commit(transaction);

		} catch (Exception e){
			tccRestCoordinator.rollback(transaction);
			throw new CompositeTransactionException(e);
		}
		
		if (HttpServletResponse.SC_NO_CONTENT != commitResponse.getStatus()) {
			throw new CompositeTransactionException(commitResponse.readEntity(String.class));
		}
		
		return newObjectsUris;
	}
}
