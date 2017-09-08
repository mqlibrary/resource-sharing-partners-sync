package org.nishen.resourcepartners.resources;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nishen.resourcepartners.SyncException;
import org.nishen.resourcepartners.SyncProcessor;
import org.nishen.resourcepartners.SyncProcessorFactory;
import org.nishen.resourcepartners.dao.Cache;
import org.nishen.resourcepartners.entity.ElasticSearchChangeRecord;
import org.nishen.resourcepartners.entity.SyncPayload;
import org.nishen.resourcepartners.model.ObjectFactory;
import org.nishen.resourcepartners.model.Partner;
import org.nishen.resourcepartners.model.Partners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("{nuc}")
public class Synchroniser
{
	private static final Logger log = LoggerFactory.getLogger(Synchroniser.class);

	private ObjectFactory of = new ObjectFactory();

	private SyncProcessorFactory syncProcessorFactory;

	private Cache<SyncPayload> cache;

	@Inject
	public Synchroniser(Provider<Cache<SyncPayload>> cacheProvider, SyncProcessorFactory syncProcessorFactory)
	{
		cache = cacheProvider.get();
		this.syncProcessorFactory = syncProcessorFactory;
	}

	@Path("sync")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes("application/x-www-form-urlencoded")
	public Response sync(@PathParam("nuc") String nuc, @FormParam("apikey") String apikey)
	{
		log.debug("[sync] nuc: {}, key: {}", nuc, apikey);

		Partners partners = of.createPartners();

		try
		{
			SyncProcessor sync = syncProcessorFactory.create(nuc, apikey);
			SyncPayload payload = sync.sync(false).get();
			for (Partner p : payload.getChanged().values())
				partners.getPartner().add(p);
		}
		catch (SyncException se)
		{
			log.warn("{}", se.getMessage(), se);
			return Response.status(Status.NOT_FOUND)
			               .entity("{\"error\": \"configuration for organisation not found: " + nuc + "\"}").build();
		}
		catch (Exception e)
		{
			log.warn("{}", e.getMessage(), e);
			return Response.serverError().entity(String.format("{ \"error\": \"%s\" }", e.getMessage())).build();
		}

		return Response.ok(partners).build();
	}

	@Path("preview")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes("application/x-www-form-urlencoded")
	public Response preview(@PathParam("nuc") String nuc, @FormParam("apikey") String apikey)
	{
		log.debug("[preview] nuc: {}, key: {}", nuc, apikey);

		Partners partners = of.createPartners();

		try
		{
			SyncProcessor sync = syncProcessorFactory.create(nuc, apikey);
			SyncPayload payload = sync.sync(true).get();
			for (Partner p : payload.getChanged().values())
				partners.getPartner().add(p);
		}
		catch (SyncException se)
		{
			log.warn("{}", se.getMessage(), se);
			return Response.status(Status.NOT_FOUND)
			               .entity("{\"error\": \"configuration for organisation not found: " + nuc + "\"}").build();
		}
		catch (Exception e)
		{
			log.warn("{}", e.getMessage(), e);
			return Response.serverError().entity(String.format("{ \"error\": \"%s\" }", e.getMessage())).build();
		}

		return Response.ok(partners).build();
	}

	@Path("changes")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes("application/x-www-form-urlencoded")
	public Response changes(@PathParam("nuc") String nuc, @FormParam("apikey") String apikey)
	{
		log.debug("[changes] nuc: {}, key: {}", nuc, apikey);

		List<ElasticSearchChangeRecord> changes = new ArrayList<ElasticSearchChangeRecord>();

		try
		{
			SyncProcessor sync = syncProcessorFactory.create(nuc, apikey);
			SyncPayload payload = sync.sync(true).get();
			for (List<ElasticSearchChangeRecord> changeList : payload.getChanges().values())
				changes.addAll(changeList);

			GenericEntity<List<ElasticSearchChangeRecord>> entity =
			        new GenericEntity<List<ElasticSearchChangeRecord>>(changes) {};

			return Response.ok(entity).build();
		}
		catch (SyncException se)
		{
			log.warn("{}", se.getMessage(), se);
			return Response.status(Status.NOT_FOUND)
			               .entity("{\"error\": \"configuration for organisation not found: " + nuc + "\"}").build();
		}
		catch (Exception e)
		{
			log.warn("{}", e.getMessage(), e);
			return Response.serverError().entity(String.format("{ \"error\": \"%s\" }", e.getMessage())).build();
		}
	}

	@Path("orphaned")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes("application/x-www-form-urlencoded")
	public Response orphaned(@PathParam("nuc") String nuc, @FormParam("apikey") String apikey)
	{
		log.debug("[orphaned] nuc: {}, key: {}", nuc, apikey);

		Partners partners = of.createPartners();

		try
		{
			SyncProcessor sync = syncProcessorFactory.create(nuc, apikey);
			SyncPayload payload = sync.sync(true).get();
			for (Partner p : payload.getDeleted().values())
				partners.getPartner().add(p);
		}
		catch (SyncException se)
		{
			log.warn("{}", se.getMessage(), se);
			return Response.status(Status.NOT_FOUND)
			               .entity("{\"error\": \"configuration for organisation not found: " + nuc + "\"}").build();
		}
		catch (Exception e)
		{
			log.warn("{}", e.getMessage(), e);
			return Response.serverError().entity(String.format("{ \"error\": \"%s\" }", e.getMessage())).build();
		}

		return Response.ok(partners).build();
	}

	@Path("expirecache")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes("application/x-www-form-urlencoded")
	public Response expireCache(@PathParam("nuc") String nuc, @FormParam("apikey") String apikey)
	{
		log.debug("[expireCache] nuc: {}, key: {}", nuc, apikey);

		cache.expire(apikey);

		return Response.ok().build();
	}
}
