package net.jotorren.microservices.content.controller;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import net.jotorren.microservices.content.domain.SourceCodeItem;
import net.jotorren.microservices.content.service.ContentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@Path("/content")
public class ContentController {

	private static final Logger LOG = LoggerFactory.getLogger(ContentController.class);

	@Autowired
	private ContentService service;
	
	@GET
	@Path("/{id}")
	@Produces("application/json")
	public SourceCodeItem get(@PathParam("id") String id) {
		LOG.info("Trying to get content item [{}] outside any transaction", id);
		
		return service.get(id);
	}

	@POST
	public Response save(@Context UriInfo uriInfo, SourceCodeItem content) {
		LOG.info("Trying to save content outside any transaction");
		
		String id = service.save(content);
		LOG.info("New content item id set to [{}]", id);
		
		URI location = uriInfo.getAbsolutePathBuilder().path("{id}")
				.resolveTemplate("id", id).build();
		LOG.info("New content item uri [{}]", location);
		
		return Response.created(location).build();
	}
	
	
	@GET
	@Path("/{txid}/{id}")
	@Produces("application/json")
	public SourceCodeItem getTxAware(@PathParam("txid") String txid, @PathParam("id") String id) {
		LOG.info("Trying to get content item [{}] inside transaction [{}]", id, txid);
		
		return service.get(txid, id);
	}

	@POST
	@Path("/{txid}")
	public Response saveTxAware(@Context UriInfo uriInfo, @PathParam("txid") String txid, SourceCodeItem content) {
		LOG.info("Trying to save content inside transaction [{}]", txid);
		
		String id = service.save(txid, content);
		LOG.info("New content item id set to [{}]", id);
		
		URI location = uriInfo.getAbsolutePathBuilder().path("{id}")
				.resolveTemplate("id", id).build();
		LOG.info("New content item uri [{}]", location);
		
		return Response.created(location).build();
	}
	
	@DELETE
	@Path("/tcc/{txid}")
	@Consumes("application/tcc")
	public void cancel(@PathParam("txid") String txid){
		LOG.info("Trying to rollback transaction [{}]", txid);
		
		service.rollback(txid);
		
		LOG.info("Transaction [{}] rolled back", txid);
	}

	@PUT
	@Path("/tcc/{txid}")
	@Consumes("application/tcc")
	public void confirm(@PathParam("txid") String txid){
		LOG.info("Trying to commit transaction [{}]", txid);
		
		service.commit(txid);
		
		LOG.info("Transaction [{}] committed", txid);
	}
}
