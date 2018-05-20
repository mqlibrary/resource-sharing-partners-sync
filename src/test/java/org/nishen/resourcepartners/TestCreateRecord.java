package org.nishen.resourcepartners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.nishen.resourcepartners.dao.ElasticSearchDAO;
import org.nishen.resourcepartners.entity.ElasticSearchPartner;
import org.nishen.resourcepartners.model.Partner;
import org.nishen.resourcepartners.util.JaxbUtilModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import static org.junit.Assert.fail;

public class TestCreateRecord
{
	private static final Logger log = LoggerFactory.getLogger(TestCreateRecord.class);

	private static Injector injector = null;

	private static ElasticSearchDAO esDAO = null;

	private static SyncProcessor syncProcessor = null;

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

		esDAO = injector.getInstance(ElasticSearchDAO.class);

		SyncProcessorFactory syncProcessorFactory = injector.getInstance(SyncProcessorFactory.class);
		try
		{
			syncProcessor = syncProcessorFactory.create("NMQU", "l7xx564d0f1a4bc8423aa78a6b9619023878");
		}
		catch (SyncException se)
		{
			log.error("failed to setup syncProcessor: {}", se.getMessage(), se);
		}
	}

	@Test
	public void testGetPartner()
	{
		log.debug("running test: {}", Arrays.asList(new Throwable().getStackTrace()).get(0).getMethodName());

		try
		{
			ElasticSearchPartner partner = esDAO.getPartner("SL").orElse(null);
			log.debug("{}", partner);

			Partner p = syncProcessor.makePartner(partner);
			log.debug("{}", p);

			JaxbUtilModel.formatPretty(p, System.out);
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}
}
