package net.jotorren.microservices.content.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import net.jotorren.microservices.content.domain.SourceCodeItem;
import net.jotorren.microservices.content.service.ContentService;
import net.jotorren.microservices.rs.ExceptionRestHandler.ErrorDetails;
import net.jotorren.microservices.tx.CompositeTransactionParticipantController;
import net.jotorren.microservices.tx.CompositeTransactionParticipantService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@Path("/")
@Api(value = "Content services")
public class ContentController extends CompositeTransactionParticipantController {

	private static final Logger LOG = LoggerFactory.getLogger(ContentController.class);

	@Autowired
	private ContentService service;

	@Override
	public CompositeTransactionParticipantService getCompositeTransactionParticipantService() {
		return service;
	}
	
	@GET
	@Path("{id}")
	@Produces("application/json")
	public SourceCodeItem get(@PathParam("id") String id) {
		LOG.info("Trying to get content item [{}] outside any transaction", id);
		
		return service.getContent(id);
	}

	@POST
    @ApiOperation(
    		code = 201,
            value = "Save new content",
            notes = "The newly created resource can be referenced by the URI returned in the the Location header field",
            response = String.class,
            responseHeaders = {
    			 @ResponseHeader(name = "Location", description = "The URI of the saved content", response = String.class)
    		}
        )
	@ApiResponses(value = {
			@ApiResponse(code=503, message="Internal error", response = ErrorDetails.class)
	})
	public Response save(@Context UriInfo uriInfo, 
			@ApiParam(value = "Data of the source code item", required = true) SourceCodeItem content
			) {
		LOG.info("Trying to save content outside any transaction");
		
		String id = service.addNewContent(content);
		LOG.info("New content item id set to [{}]", id);
		
		URI location = uriInfo.getAbsolutePathBuilder().path("{id}")
				.resolveTemplate("id", id).build();
		LOG.info("New content item uri [{}]", location);
		
		return Response.created(location).build();
	}
	
	// Composite Transaction methods
	
	@GET
	@Path("{txid}/{id}")
	@Produces("application/json")
	public SourceCodeItem getTxAware(@PathParam("txid") String txid, @PathParam("id") String id) {
		LOG.info("Trying to get content item [{}] inside transaction [{}]", id, txid);
		
		return service.getContent(txid, id);
	}

	@POST
	@Path("{txid}")
    @ApiOperation(
    		code = 201,
            value = "Save new content enlisting the operation in a composite transaction",
            notes = "The newly created resource can be referenced by the URI returned in the the Location header field",
            response = String.class,
            responseHeaders = {
    			 @ResponseHeader(name = "Location", description = "The URI of the saved content", response = String.class)
    		}
        )
	@ApiResponses(value = {
			@ApiResponse(code=503, message="Internal error", response = ErrorDetails.class)
	})
	public Response saveTxAware(@Context UriInfo uriInfo, 
			@ApiParam(value = "Id of the composite transaction where the operation must be enlisted", required = true) @PathParam("txid") String txid, 
			@ApiParam(value = "Data of the source code item", required = true) SourceCodeItem content
			) {
		LOG.info("Trying to save content inside transaction [{}]", txid);
		
		String id = service.addNewContent(txid, content);
		LOG.info("New content item id set to [{}]", id);
		
		URI location = uriInfo.getAbsolutePathBuilder().path("{id}")
				.resolveTemplate("id", id).build();
		LOG.info("New content item uri [{}]", location);
		
		return Response.created(location).build();
	}
}
