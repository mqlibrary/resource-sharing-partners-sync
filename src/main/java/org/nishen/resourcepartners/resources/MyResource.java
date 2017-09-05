package org.nishen.resourcepartners.resources;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.nishen.resourcepartners.dao.ElasticSearchDAO;
import org.nishen.resourcepartners.entity.ElasticSearchPartner;
import org.nishen.resourcepartners.util.JaxbUtil;

@Path("myresource")
public class MyResource
{
	@Inject
	private ElasticSearchDAO elastic;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getIt()
	{
		ElasticSearchPartner p = null;
		try
		{
			p = elastic.getPartner("NMQU").get();
		}
		catch (IOException ioe)
		{

		}

		// return config.get("last_run").orElse("no result!");
		return JaxbUtil.formatPretty(p);
	}
}
