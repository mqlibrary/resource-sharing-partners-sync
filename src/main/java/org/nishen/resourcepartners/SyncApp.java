package org.nishen.resourcepartners;

import javax.inject.Inject;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

public class SyncApp extends ResourceConfig
{
	@Inject
	public SyncApp(ServiceLocator locator)
	{
		packages("org.nishen.resourcepartners");

		// link guice with hk2
		GuiceBridge.getGuiceBridge().initializeGuiceBridge(locator);
		GuiceIntoHK2Bridge guiceBridge = locator.getService(GuiceIntoHK2Bridge.class);
		guiceBridge.bridgeGuiceInjector(SyncServletContextListener.injector);
	}
}
