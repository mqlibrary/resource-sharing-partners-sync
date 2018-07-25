package org.nishen.resourcepartners;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.nishen.resourcepartners.entity.ElasticSearchPartner;
import org.nishen.resourcepartners.util.DataUtils;
import org.nishen.resourcepartners.util.JaxbUtil;
import org.nishen.resourcepartners.util.JaxbUtilModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class TestSyncProcessor
{
	private static final Logger log = LoggerFactory.getLogger(TestSyncProcessor.class);

	private static Injector injector = null;

	private static SyncProcessor sync = null;

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

		SyncProcessorFactory syncFactory = injector.getInstance(SyncProcessorFactory.class);
		try
		{
			sync = syncFactory.create("NMQU", "apikey ");
		}
		catch (SyncException e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testSyncProcessor1()
	{
		log.debug("running test: {}", Arrays.asList(new Throwable().getStackTrace()).get(0).getMethodName());

		try
		{
			byte[] dataBytes = DataUtils.loadFile("src/test/resources/data/partner-aato.json");
			String data = new String(dataBytes);

			ElasticSearchPartner esp = JaxbUtil.get(data, ElasticSearchPartner.class);

			log.debug(JaxbUtil.formatPretty(esp));

			log.debug(JaxbUtilModel.formatPretty(sync.makePartner(esp)));
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testSyncProcessor2()
	{
		log.debug("running test: {}", Arrays.asList(new Throwable().getStackTrace()).get(0).getMethodName());

		try
		{
			byte[] dataBytes = DataUtils.loadFile("src/test/resources/data/partner-ntsm.json");
			String data = new String(dataBytes);

			ElasticSearchPartner esp = JaxbUtil.get(data, ElasticSearchPartner.class);

			log.debug(JaxbUtil.formatPretty(esp));

			log.debug(JaxbUtilModel.formatPretty(sync.makePartner(esp)));
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

}
