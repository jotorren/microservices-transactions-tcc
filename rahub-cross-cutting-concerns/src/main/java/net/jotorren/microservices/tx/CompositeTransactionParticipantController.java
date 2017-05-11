package net.jotorren.microservices.tx;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CompositeTransactionParticipantController {

	private static final Logger LOG = LoggerFactory.getLogger(CompositeTransactionParticipantController.class);
	
	@DELETE
	@Path("/tcc/{txid}")
	@Consumes("application/tcc")
	public void cancel(@PathParam("txid") String txid){
		LOG.info("Trying to rollback transaction [{}]", txid);
		
		getCompositeTransactionParticipantService().cancel(txid);
		
		LOG.info("Transaction [{}] rolled back", txid);
	}

	@PUT
	@Path("/tcc/{txid}")
	@Consumes("application/tcc")
	public void confirm(@PathParam("txid") String txid){
		LOG.info("Trying to commit transaction [{}]", txid);
		
		getCompositeTransactionParticipantService().confirm(txid);
		
		LOG.info("Transaction [{}] committed", txid);
	}
	
	public abstract CompositeTransactionParticipantService getCompositeTransactionParticipantService();
}
