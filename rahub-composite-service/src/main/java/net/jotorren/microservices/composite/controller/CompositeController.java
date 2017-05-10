package net.jotorren.microservices.composite.controller;

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
import net.jotorren.microservices.tx.CompositeTransactionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@Path("/api")
public class CompositeController {

	@Autowired
	private CompositeService service;
	
	@POST
	@Path("/files")
	@Produces ( "application/json" )
	public Response save(@Context UriInfo uriInfo, CompositeData data) throws CompositeTransactionException {
		
		Entry<String, List<String>> txEntities = service.saveAllEntities(data);
		
		URI location = uriInfo.getAbsolutePathBuilder().path("{id}")
				.resolveTemplate("id", txEntities.getKey()).build();
		
		return Response.created(location).entity(txEntities.getValue()).build();
	}
}
