package net.jotorren.microservices.composite.service;

import java.net.URI;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.core.Response;

import net.jotorren.microservices.composite.domain.CompositeData;
import net.jotorren.microservices.composite.domain.CompositeForum;
import net.jotorren.microservices.tcc.TccRestCoordinator;
import net.jotorren.microservices.tx.CompositeTransaction;
import net.jotorren.microservices.tx.CompositeTransactionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CompositeService {

	private static final Logger LOG = LoggerFactory.getLogger(CompositeService.class);

	@Value("${tcc.transaction.timeout}")
	private long transactionTimeout;

	@Autowired
	private TccRestCoordinator tccRestCoordinator;

	@Value("${content.service.url}")
	private String contentServiceUrl;
	
	@Value("${content.service.tcc.url}")
	private String contentServiceTccUrl;

	@Value("${forum.service.url}")
	private String forumServiceUrl;
	
	@Value("${forum.service.tcc.url}")
	private String forumServiceTccUrl;
	
	@Autowired
	private RestTemplate restTemplate;

	private String getIdFromURI(URI uri){
		String[] segments = uri.getPath().split("/");
		return segments[segments.length-1];
	}
	
	private String removeTransactionIdFromURI(URI uri){
		String[] segments = uri.getPath().split("/");
		String txId = segments[segments.length-2];
		
		return uri.toString().replace("/"+txId, "");
	}
	
	public Entry<String, List<String>> saveAllEntities(CompositeData data) throws CompositeTransactionException {
		Entry<String, List<String>> newObjectsUris = null;
		
		CompositeTransaction transaction = tccRestCoordinator.open(System.currentTimeMillis() + transactionTimeout, 
				contentServiceTccUrl, forumServiceTccUrl);

		Response commitResponse;
		try{
			// first service call
			String contentServiceUrlWithTransaction = contentServiceUrl + 
					(contentServiceUrl.endsWith("/")? "" : "/") + transaction.getPartialTransactionId(0);
			LOG.info("Step 1: calling [{}]", contentServiceUrlWithTransaction);
			URI contentUriWithTransaction = restTemplate.postForLocation(contentServiceUrlWithTransaction, data);
			LOG.info("Step 1: content created [{}]", contentUriWithTransaction);
			
			// second service call preparation (using data received from the first one)
			CompositeForum forumData = new CompositeForum();
			forumData.setTopicName(data.getTopicName());
			forumData.setTopicCategory(data.getTopicCategory());
			forumData.setSubjectId(getIdFromURI(contentUriWithTransaction));
			
			// second service call
			String forumServiceUrlWithTransaction = forumServiceUrl + 
					(forumServiceUrl.endsWith("/")? "" : "/") + transaction.getPartialTransactionId(1);
			LOG.info("Step 2: calling [{}]", forumServiceUrlWithTransaction);
			URI forumUriWithTransaction = restTemplate.postForLocation(forumServiceUrlWithTransaction, forumData);
			LOG.info("Step 2: forum discussion created [{}]", forumUriWithTransaction);
			
			// commit
			commitResponse = tccRestCoordinator.commit(transaction);

			// everything seems to be fine
			newObjectsUris = new AbstractMap.SimpleEntry<String, List<String>>(
					transaction.getId(), Arrays.asList(
							removeTransactionIdFromURI(contentUriWithTransaction),
							removeTransactionIdFromURI(forumUriWithTransaction)
			));
			
		} catch (Exception e){
			// oops! something went wrong
			tccRestCoordinator.rollback(transaction);
			throw new CompositeTransactionException(e);
		}
		
		// check for potential heuristic exceptions (some commit calls failed)
		if (Response.Status.NO_CONTENT.getStatusCode() != commitResponse.getStatus()) {
			throw new CompositeTransactionException(commitResponse.readEntity(String.class));
		}
		
		return newObjectsUris;
	}
}
