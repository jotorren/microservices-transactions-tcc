package net.jotorren.microservices.forum.controller;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import net.jotorren.microservices.forum.domain.Forum;
import net.jotorren.microservices.forum.service.ForumService;
import net.jotorren.microservices.tx.CompositeTransactionParticipantController;
import net.jotorren.microservices.tx.CompositeTransactionParticipantService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@Path("/forum")
public class ForumController extends CompositeTransactionParticipantController {

	private static final Logger LOG = LoggerFactory.getLogger(ForumController.class);

	@Autowired
	private ForumService service;

	@Override
	public CompositeTransactionParticipantService getCompositeTransactionParticipantService() {
		return service;
	}
	
	@GET
	@Path("/{id}")
	@Produces("application/json")
	public Forum get(@PathParam("id") String id) {
		LOG.info("Trying to get content item [{}] outside any transaction", id);
		
		return service.getForum(id);
	}

	@POST
	public Response save(@Context UriInfo uriInfo, Forum content) {
		LOG.info("Trying to save content outside any transaction");
		
		String id = service.addNewForum(content);
		LOG.info("New content item id set to [{}]", id);
		
		URI location = uriInfo.getAbsolutePathBuilder().path("{id}")
				.resolveTemplate("id", id).build();
		LOG.info("New content item uri [{}]", location);
		
		return Response.created(location).build();
	}
	
	// Composite Transaction methods
	
	@GET
	@Path("/{txid}/{id}")
	@Produces("application/json")
	public Forum getTxAware(@PathParam("txid") String txid, @PathParam("id") String id) {
		LOG.info("Trying to get content item [{}] inside transaction [{}]", id, txid);
		
		return service.getForum(txid, id);
	}

	@POST
	@Path("/{txid}")
	public Response saveTxAware(@Context UriInfo uriInfo, @PathParam("txid") String txid, Forum content) {
		LOG.info("Trying to save content inside transaction [{}]", txid);
		
		String id = service.addNewForum(txid, content);
		LOG.info("New content item id set to [{}]", id);
		
		URI location = uriInfo.getAbsolutePathBuilder().path("{id}")
				.resolveTemplate("id", id).build();
		LOG.info("New content item uri [{}]", location);
		
		return Response.created(location).build();
	}
}
