package net.jotorren.microservices.tx;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

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
    @ApiOperation(
            value = "Rollback a given composite transaction",
            notes = "See https://www.atomikos.com/Blog/TransactionManagementAPIForRESTTCC",
            consumes = "application/tcc"
        )
	public void cancel(
			@ApiParam(value = "Id of the composite transaction to rollback", required = true) @PathParam("txid") String txid
			){
		LOG.info("Trying to rollback transaction [{}]", txid);
		
		getCompositeTransactionParticipantService().cancel(txid);
		
		LOG.info("Transaction [{}] rolled back", txid);
	}

	@PUT
	@Path("/tcc/{txid}")
	@Consumes("application/tcc")
    @ApiOperation(
            value = "Commit a given composite transaction",
            notes = "See https://www.atomikos.com/Blog/TransactionManagementAPIForRESTTCC",
            consumes = "application/tcc"
        )
	public void confirm(
			@ApiParam(value = "Id of the composite transaction to commit", required = true) @PathParam("txid") String txid
		){
		LOG.info("Trying to commit transaction [{}]", txid);
		
		getCompositeTransactionParticipantService().confirm(txid);
		
		LOG.info("Transaction [{}] committed", txid);
	}
	
	public abstract CompositeTransactionParticipantService getCompositeTransactionParticipantService();
}
