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
import org.nishen.resourcepartners.model.ObjectFactory;
import org.nishen.resourcepartners.model.Partner;
import org.nishen.resourcepartners.model.Partners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("preview/{nuc}")
public class Previewer
{
	private static final Logger log = LoggerFactory.getLogger(Previewer.class);

	private ObjectFactory of = new ObjectFactory();

	@Inject
	private SyncProcessorFactory syncProcessorFactory;

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes("application/x-www-form-urlencoded")
	public Response preview(@PathParam("nuc") String nuc, @FormParam("apikey") String apikey)
	{
		log.debug("nuc: {}, key: {}", nuc, apikey);

		SyncProcessor sync = syncProcessorFactory.create(nuc, apikey);

		Partners partners = of.createPartners();

		try
		{
			for (Partner p : sync.sync(true).get().values())
				partners.getPartner().add(p);
		}
		catch (Exception e)
		{
			return Response.serverError().build();
		}

		return Response.ok(partners).build();
	}
}
