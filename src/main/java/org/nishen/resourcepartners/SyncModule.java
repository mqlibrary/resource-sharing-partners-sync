package org.nishen.resourcepartners;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.xml.bind.Marshaller;

import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.nishen.resourcepartners.dao.AlmaDAO;
import org.nishen.resourcepartners.dao.AlmaDAOFactory;
import org.nishen.resourcepartners.dao.AlmaDAOImpl;
import org.nishen.resourcepartners.dao.Config;
import org.nishen.resourcepartners.dao.ConfigFactory;
import org.nishen.resourcepartners.dao.ConfigImpl;
import org.nishen.resourcepartners.dao.ElasticSearchDAO;
import org.nishen.resourcepartners.dao.ElasticSearchDAOImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class SyncModule extends AbstractModule
{
	private static final Logger log = LoggerFactory.getLogger(SyncModule.class);

	private static final String CONFIG_FILE = "app.properties";

	private static final Properties config = new Properties();

	private WebTarget elasticTarget = null;

	private WebTarget almaTarget = null;

	@Override
	protected void configure()
	{
		String configFilename = CONFIG_FILE;
		if (System.getProperty("config") != null)
			configFilename = System.getProperty("config");

		File configFile = new File(configFilename);
		try
		{
			if (!configFile.exists() || !configFile.canRead())
				throw new IOException("cannot read config file: " + configFile.getAbsolutePath());

			config.load(new FileReader(configFile));

			if (log.isDebugEnabled())
				for (String k : config.stringPropertyNames())
					log.debug("{}: {}={}", new Object[] { CONFIG_FILE, k, config.getProperty(k) });
		}
		catch (IOException e)
		{
			log.error("unable to load configuration: {}", configFile.getAbsoluteFile(), e);
			return;
		}

		// bind instances
		bind(ElasticSearchDAO.class).to(ElasticSearchDAOImpl.class).in(Scopes.SINGLETON);
		bind(String.class).annotatedWith(Names.named("ws.alma.key")).toInstance(config.getProperty("ws.alma.key"));

		FactoryModuleBuilder factoryModuleBuilder = new FactoryModuleBuilder();
		install(factoryModuleBuilder.implement(Config.class, ConfigImpl.class).build(ConfigFactory.class));
		install(factoryModuleBuilder.implement(AlmaDAO.class, AlmaDAOImpl.class).build(AlmaDAOFactory.class));
		install(factoryModuleBuilder.implement(SyncProcessor.class, SyncProcessorImpl.class)
		                            .build(SyncProcessorFactory.class));
	}

	@Provides
	@Named("ws.elastic")
	protected WebTarget provideWebTargetElastic()
	{
		if (elasticTarget == null)
		{
			String usr = config.getProperty("ws.elastic.usr");
			String pwd = config.getProperty("ws.elastic.pwd");
			HttpAuthenticationFeature auth = HttpAuthenticationFeature.basic(usr, pwd);

			Client client =
			        ClientBuilder.newClient().register(auth).property(UnmarshallerProperties.JSON_ATTRIBUTE_PREFIX, "@")
			                     .property(UnmarshallerProperties.JSON_INCLUDE_ROOT, false)
			                     .property(MarshallerProperties.JSON_ATTRIBUTE_PREFIX, "@")
			                     .property(Marshaller.JAXB_ENCODING, "UTF-8");

			elasticTarget = client.target(config.getProperty("ws.elastic.url"));
		}

		return elasticTarget;
	}

	@Provides
	@Named("ws.alma")
	protected WebTarget provideWebTargetAlma()
	{
		String url = config.getProperty("ws.alma.url");

		Client client = ClientBuilder.newClient();
		if (almaTarget == null)
		{
			log.info("using alma api: {}", url);
			almaTarget = client.target(url);
		}

		return almaTarget;
	}
}
