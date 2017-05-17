package net.jotorren.microservices.composite.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;

import java.net.URI;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import net.jotorren.microservices.composite.domain.CompositeData;
import net.jotorren.microservices.composite.service.CompositeService;
import net.jotorren.microservices.rs.ExceptionRestHandler.ErrorDetails;
import net.jotorren.microservices.tx.CompositeTransactionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@Path("/")
@Api(value = "API Services")
public class CompositeController {

	@Autowired
	private CompositeService service;
	
	@POST
	@Path("files")
	@Produces ( "application/json" )
    @ApiOperation(
    		code = 201,
            value = "Save new content while creating its discussion board",
            notes = "Both operations (new content and forum) must succeed within a given time interval, otherwise both will be canceled or rolled back. "
            		+ "The newly created resource(s) can be referenced by the URI(s) returned in the entity of the response, with the URI for the "
            		+ "distributed transaction given by the Location header field",
            response = String.class,
            responseContainer = "List",
            responseHeaders = {
    			 @ResponseHeader(name = "Location", description = "The distributed transaction URI", response = String.class)
    		}
        )
	@ApiResponses(value = {
			@ApiResponse(code=500, message="Error processing request", response = ErrorDetails.class)
	})
	public Response save(@Context UriInfo uriInfo, 
			@ApiParam(value = "Data to pass to server", required = true) CompositeData data
			) throws CompositeTransactionException {
		
		Entry<String, List<String>> txEntities = service.saveAllEntities(data);
		
		URI location = uriInfo.getAbsolutePathBuilder().path("{id}")
				.resolveTemplate("id", txEntities.getKey()).build();
		
		return Response.created(location).entity(txEntities.getValue()).build();
	}
}
