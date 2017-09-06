package org.nishen.resourcepartners.resources;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nishen.resourcepartners.SyncProcessor;
import org.nishen.resourcepartners.SyncProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("sync/{nuc}")
public class Synchroniser
{
	private static final Logger log = LoggerFactory.getLogger(Synchroniser.class);

	@Inject
	private SyncProcessorFactory syncProcessorFactory;

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes("application/x-www-form-urlencoded")
	public Response sync(@PathParam("nuc") String nuc, @FormParam("apikey") String apikey)
	{
		log.debug("nuc: {}, key: {}", nuc, apikey);

		SyncProcessor sync = syncProcessorFactory.create(nuc, apikey);

		return Response.ok("{ \"name\" : \"Sweet!\" }").build();
	}
}
