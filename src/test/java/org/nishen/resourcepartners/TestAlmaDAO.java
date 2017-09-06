package org.nishen.resourcepartners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.nishen.resourcepartners.dao.AlmaDAO;
import org.nishen.resourcepartners.dao.AlmaDAOFactory;
import org.nishen.resourcepartners.model.Partner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

import static org.junit.Assert.fail;

public class TestAlmaDAO
{
	private static final Logger log = LoggerFactory.getLogger(TestAlmaDAO.class);

	private static Injector injector = null;

	private static AlmaDAO alma = null;

	@BeforeClass
	public static void setup()
	{
		// list for injector modules
		List<Module> modules = new ArrayList<Module>();

		// module (main configuration)
		modules.add(new SyncModule());

		// create the injector
		log.debug("creating injector");
		injector = Guice.createInjector(modules);

		AlmaDAOFactory factory = injector.getInstance(AlmaDAOFactory.class);
		String apikey = injector.getInstance(Key.get(String.class, Names.named("ws.alma.key")));
		alma = factory.create(apikey);
	}

	@Test
	public void testGetPartner()
	{
		log.debug("running test: {}", Arrays.asList(new Throwable().getStackTrace()).get(0).getMethodName());

		try
		{
			Map<String, Partner> partners = alma.getPartners();
			for (Map.Entry<String, Partner> entry : partners.entrySet())
			{
				log.debug("{}: {}", entry.getKey(), entry.getValue().getPartnerDetails().getCode());
			}
		}
		catch (SyncException e)
		{
			fail(e.getMessage());
		}
	}
}
