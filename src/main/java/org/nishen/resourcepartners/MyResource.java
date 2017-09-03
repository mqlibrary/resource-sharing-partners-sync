package org.nishen.resourcepartners;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.nishen.resourcepartners.dao.Config;
import org.nishen.resourcepartners.dao.ConfigFactory;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("myresource")
public class MyResource
{
	private Config config;

	@Inject
	private ConfigFactory configFactory;

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getIt()
	{
		config = configFactory.create("ILRS");

		return config.get("last_run").orElse("no result!");
	}
}
