package net.jotorren.microservices.tcc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import net.jotorren.microservices.tx.CompositeTransaction;
import net.jotorren.microservices.tx.CompositeTransactionManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.atomikos.tcc.rest.ParticipantLink;
import com.atomikos.tcc.rest.Transaction;

@Service
public class TccRestCoordinator {

    @Autowired
    @Qualifier("tccCoordinatorClient")
    private WebTarget tccCoordinatorClient;
    
    @Autowired
    private CompositeTransactionManager txManager;
    
	public CompositeTransaction open(long timestamp, String... participantUris){
		String txId = UUID.randomUUID().toString();
		
		int i = 0;
		String participantUri;
		String partialTxId;
		List<ParticipantLink> participants = new ArrayList<ParticipantLink>();
		for (String uri : participantUris) {
			partialTxId = txId + "-" + String.format("%03d", i++);
			txManager.open(partialTxId);
			
			participantUri = uri + (uri.endsWith("/")? "" : "/") + partialTxId;
			ParticipantLink participantLink = new ParticipantLink(participantUri, timestamp);
			participants.add(participantLink);
		}
		
		CompositeTransaction transaction = new CompositeTransaction();
		transaction.setId(txId);
		transaction.getParticipantLinks().addAll(participants);
		return transaction;
	}
	
	public Response commit(Transaction transaction){
		return tccCoordinatorClient.path("/confirm").request().put(Entity.entity(transaction, "application/tcc+json"));
	}

	public Response rollback(Transaction transaction){
		return tccCoordinatorClient.path("/cancel").request().put(Entity.entity(transaction, "application/tcc+json"));
	}
	
	public void close(CompositeTransaction transaction){
		txManager.close(transaction.getId());
	}
}
