package org.nishen.resourcepartners;

import javax.inject.Inject;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.jaxb.internal.JaxbMessagingBinder;
import org.glassfish.jersey.jaxb.internal.JaxbParamConverterBinder;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.jaxrs.config.BeanConfig;

public class SyncApp extends ResourceConfig
{
	private static final Logger log = LoggerFactory.getLogger(ResourceConfig.class);

	@Inject
	public SyncApp(ServiceLocator locator)
	{
		packages("org.nishen.resourcepartners");

		register(io.swagger.jaxrs.listing.ApiListingResource.class);
		register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
		register(new JaxbMessagingBinder());
		register(new JaxbParamConverterBinder());
		register(new MoxyJsonFeature());

		// link guice with hk2
		GuiceBridge.getGuiceBridge().initializeGuiceBridge(locator);
		GuiceIntoHK2Bridge guiceBridge = locator.getService(GuiceIntoHK2Bridge.class);
		guiceBridge.bridgeGuiceInjector(SyncServletContextListener.injector);

		BeanConfig beanConfig = new BeanConfig();
		beanConfig.setVersion("0.0.8");
		beanConfig.setSchemes(new String[] { "http", "https" });
		beanConfig.setBasePath("/partner-sync");
		beanConfig.setResourcePackage("org.nishen.resourcepartners.resources");
		beanConfig.setPrettyPrint(true);
		beanConfig.setScan(true);

		log.debug("instantiated class: {}", this.getClass().getName());
	}
}
