package net.jotorren.microservices.tx;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.jotorren.microservices.rs.ExceptionRestHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CompositeTransactionParticipantController {

	private static final Logger LOG = LoggerFactory.getLogger(CompositeTransactionParticipantController.class);
	
	@DELETE
	@Path("/tcc/{txid}")
	@Consumes("application/tcc")
    @ApiOperation(
    		code = 204,
    		response = String.class,
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
    		code = 204,
    		response = String.class,
            value = "Commit a given composite transaction",
            notes = "See https://www.atomikos.com/Blog/TransactionManagementAPIForRESTTCC",
            consumes = "application/tcc"
        )
	@ApiResponses(value = {
			@ApiResponse(code=404, message="Error committing transaction", response = String.class)
	})
	public void confirm(
			@ApiParam(value = "Id of the composite transaction to commit", required = true) @PathParam("txid") String txid
		){
		LOG.info("Trying to commit transaction [{}]", txid);
		
		try {
			getCompositeTransactionParticipantService().confirm(txid);
		
			LOG.info("Transaction [{}] committed", txid);
		} catch(Exception e){
			// See com.atomikos.icatch.tcc.rest.ParticipantAdapterImp.callConfirmOnJaxrsClient()
			Response response =	Response.status(Response.Status.NOT_FOUND)
					.entity(new ExceptionRestHandler().toString(e)).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(response);			
		}
	}
	
	public abstract CompositeTransactionParticipantService getCompositeTransactionParticipantService();
}
