package org.nishen.resourcepartners.resources;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("{nuc}")
@Produces(MediaType.APPLICATION_JSON)
@Api
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

	@GET
	@Path("test")
	@ApiOperation(value = "test service", notes = "make a test call to ensure service is functioning")
	@ApiImplicitParams({ @ApiImplicitParam(name = "nuc", value = "Organisations NUC (National Union Code) symbol",
	                                       required = true, dataType = "string", paramType = "path"),
	                     @ApiImplicitParam(name = "Authorization",
	                                       value = "ExLibiris API key, e.g. 'apikey 17xxxxxxx...'", dataType = "string",
	                                       paramType = "header") })
	public Response test(@PathParam("nuc") String nuc, @HeaderParam("Authorization") String authorization)
	{
		log.debug("[test] nuc: {}, authorization: {}", nuc, authorization);

		return Response.ok().entity(String.format("{ \"Authorization\" : \"%s...\"}", authorization.substring(0, 10)))
		               .build();
	}

	@GET
	@Path("sync")
	@ApiOperation(value = "synchronise Alma institution resource sharing partners with datasource",
	              response = Partners.class)
	@ApiImplicitParams({ @ApiImplicitParam(name = "nuc", value = "Organisations NUC (National Union Code) symbol",
	                                       required = true, dataType = "string", paramType = "path"),
	                     @ApiImplicitParam(name = "Authorization",
	                                       value = "ExLibiris API key, e.g. 'apikey 17xxxxxxx...'", dataType = "string",
	                                       paramType = "header") })
	public Response sync(@PathParam("nuc") String nuc, @HeaderParam("Authorization") String authorization)
	{
		log.debug("[sync] nuc: {}, key: {}", nuc, authorization);

		Partners partners = of.createPartners();

		try
		{
			SyncProcessor sync = syncProcessorFactory.create(nuc, authorization);
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

	@GET
	@Path("preview")
	@ApiOperation(value = "preview of records that will be changed when sync is called. Does not change any data",
	              response = Partners.class)
	@ApiImplicitParams({ @ApiImplicitParam(name = "nuc", value = "Organisations NUC (National Union Code) symbol",
	                                       required = true, dataType = "string", paramType = "path"),
	                     @ApiImplicitParam(name = "Authorization",
	                                       value = "ExLibiris API key, e.g. 'apikey 17xxxxxxx...'", dataType = "string",
	                                       paramType = "header") })
	public Response preview(@PathParam("nuc") String nuc, @HeaderParam("Authorization") String authorization)
	{
		log.debug("[preview] nuc: {}, key: {}", nuc, authorization);

		Partners partners = of.createPartners();

		try
		{
			SyncProcessor sync = syncProcessorFactory.create(nuc, authorization);
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

	@GET
	@Path("changes")
	@ApiOperation(value = "view all fields that will be changed on sync", response = ElasticSearchChangeRecord.class,
	              responseContainer = "List")
	@ApiImplicitParams({ @ApiImplicitParam(name = "nuc", value = "Organisations NUC (National Union Code) symbol",
	                                       required = true, dataType = "string", paramType = "path"),
	                     @ApiImplicitParam(name = "Authorization",
	                                       value = "ExLibiris API key, e.g. 'apikey 17xxxxxxx...'", dataType = "string",
	                                       paramType = "header") })
	public Response changes(@PathParam("nuc") String nuc, @HeaderParam("Authorization") String authorization)
	{
		log.debug("[changes] nuc: {}, key: {}", nuc, authorization);

		List<ElasticSearchChangeRecord> changes = new ArrayList<ElasticSearchChangeRecord>();

		try
		{
			SyncProcessor sync = syncProcessorFactory.create(nuc, authorization);
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

	@GET
	@Path("orphaned")
	@ApiOperation(value = "view records in Alma with no equivalent in the datasource", response = Partners.class)
	@ApiImplicitParams({ @ApiImplicitParam(name = "nuc", value = "Organisations NUC (National Union Code) symbol",
	                                       required = true, dataType = "string", paramType = "path"),
	                     @ApiImplicitParam(name = "Authorization",
	                                       value = "ExLibiris API key, e.g. 'apikey 17xxxxxxx...'", dataType = "string",
	                                       paramType = "header") })
	public Response orphaned(@PathParam("nuc") String nuc, @HeaderParam("Authorization") String authorization)
	{
		log.debug("[orphaned] nuc: {}, key: {}", nuc, authorization);

		Partners partners = of.createPartners();

		try
		{
			SyncProcessor sync = syncProcessorFactory.create(nuc, authorization);
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

	@GET
	@Path("expirecache")
	@ApiOperation(value = "clear the cache")
	@ApiImplicitParams({ @ApiImplicitParam(name = "nuc", value = "Organisations NUC (National Union Code) symbol",
	                                       required = true, dataType = "string", paramType = "path"),
	                     @ApiImplicitParam(name = "Authorization",
	                                       value = "ExLibiris API key, e.g. 'apikey 17xxxxxxx...'", dataType = "string",
	                                       paramType = "header") })
	public Response expireCache(@PathParam("nuc") String nuc, @HeaderParam("Authorization") String authorization)
	{
		log.debug("[expireCache] nuc: {}, key: {}", nuc, authorization);

		cache.expire(authorization);

		return Response.ok().build();
	}
}
