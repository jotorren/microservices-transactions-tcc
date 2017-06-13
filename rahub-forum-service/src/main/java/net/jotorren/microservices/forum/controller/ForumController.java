package net.jotorren.microservices.forum.controller;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import net.jotorren.microservices.forum.domain.Forum;
import net.jotorren.microservices.forum.service.ForumService;
import net.jotorren.microservices.rs.ExceptionRestHandler;
import net.jotorren.microservices.tx.CompositeTransactionParticipantController;
import net.jotorren.microservices.tx.CompositeTransactionParticipantService;

@RestController
@Path("/")
@Api(value = "Discussion board services")
public class ForumController extends CompositeTransactionParticipantController {

	private static final Logger LOG = LoggerFactory.getLogger(ForumController.class);

	@Autowired
	private ForumService service;

	@Override
	public CompositeTransactionParticipantService getCompositeTransactionParticipantService() {
		return service;
	}
	
	@GET
	@Path("{id}")
	@Produces("application/json")
    @ApiOperation(
    		code = 200,
            value = "Find a discussion board by its Id",
            notes = "Queries data previously persisted in the database",
            response = Forum.class,
            produces = "application/json"
        )
	public Forum get(
			@ApiParam(value = "Id of the discussion board to retrieve", required = true) @PathParam("id") String id
			) {
		LOG.info("Trying to get discussion board [{}] outside any transaction", id);
		
		return service.getForum(id);
	}

	@POST
    @ApiOperation(
    		code = 201,
            value = "Save a new discussion board in the database",
            notes = "The newly created resource can be referenced by the URI returned in the the Location header field",
            response = String.class,
            responseHeaders = {
    			 @ResponseHeader(name = "Location", description = "The URI of the saved discussion board", response = String.class)
    		}
        )
	@ApiResponses(value = {
			@ApiResponse(code=500, message="Error saving the given discussion board", response = String.class)
	})
	public Response save(@Context UriInfo uriInfo, 
			@ApiParam(value = "Data of the discussion board, including the Id of the referred source code item", required = true) Forum content
			) {
		try {
			LOG.info("Trying to save discussion board outside any transaction");
			
			String id = service.addNewForum(content);
			LOG.info("New discussion board id set to [{}]", id);
			
			URI location = uriInfo.getAbsolutePathBuilder().path("{id}")
					.resolveTemplate("id", id).build();
			LOG.info("New discussion board uri [{}]", location);
			
			return Response.created(location).build();
		} catch (Exception e){
			Response response =	Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(new ExceptionRestHandler().toString(e)).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(response);				
		}
	}
	
	// Composite Transaction methods
	
	@GET
	@Path("{txid}/{id}")
	@Produces("application/json")
    @ApiOperation(
    		code = 200,
            value = "Find a discussion board by its Id",
            notes = "Queries the transaction uncommitted data in addition to the one previously persisted in the database",
            response = Forum.class,
            produces = "application/json"
        )
	public Forum getTxAware(
			@ApiParam(value = "Id of a composite transaction", required = true) @PathParam("txid") String txid, 
			@ApiParam(value = "Id of the discussion board to retrieve", required = true) @PathParam("id") String id
			) {
		LOG.info("Trying to get discussion board [{}] inside transaction [{}]", id, txid);
		
		return service.getForum(txid, id);
	}

	@POST
	@Path("{txid}")
    @ApiOperation(
    		code = 201,
            value = "Save a new discussion board enlisting the operation in a composite transaction",
            notes = "No data will be persisted in the database until the transaction is explicitly committed. "
            		+ "The newly created resource can be referenced by the URI returned in the the Location header field",
            response = String.class,
            responseHeaders = {
    			 @ResponseHeader(name = "Location", description = "The URI of the saved discussion board", response = String.class)
    		}
        )
	@ApiResponses(value = {
			@ApiResponse(code=500, message="Error saving the given discussion board", response = String.class)
	})
	public Response saveTxAware(@Context UriInfo uriInfo, 
			@ApiParam(value = "Id of the composite transaction where the operation must be enlisted", required = true) @PathParam("txid") String txid, 
			@ApiParam(value = "Data of the discussion board, including the Id of the referred source code item", required = true) Forum content) {
		try{ 
			LOG.info("Trying to save discussion board inside transaction [{}]", txid);
			
			String id = service.addNewForum(txid, content);
			LOG.info("New discussion board id set to [{}]", id);
			
			URI location = uriInfo.getAbsolutePathBuilder().path("{id}")
					.resolveTemplate("id", id).build();
			LOG.info("New discussion board uri [{}]", location);
			
			return Response.created(location).build();
		} catch (Exception e){
			Response response =	Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(new ExceptionRestHandler().toString(e)).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(response);				
		}		
	}
}
