package net.jotorren.microservices.tcc;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

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
    
	public Transaction open(long timestamp, String... participantUris){
		List<ParticipantLink> participants = new ArrayList<ParticipantLink>();
		for (String uri : participantUris) {
			ParticipantLink participantLink = new ParticipantLink(uri, timestamp);
			participants.add(participantLink);
		}

		Transaction transaction = new Transaction();
		transaction.getParticipantLinks().addAll(participants);
		return transaction;
	}
	
	public Response commit(Transaction transaction){
		return tccCoordinatorClient.path("/confirm").request().put(Entity.entity(transaction, "application/tcc+json"));
	}

	public Response rollback(Transaction transaction){
		return tccCoordinatorClient.path("/cancel").request().put(Entity.entity(transaction, "application/tcc+json"));
	}
}
