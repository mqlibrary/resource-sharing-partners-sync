package org.nishen.resourcepartners;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class SyncLauncher
{
	private static final Logger log = LoggerFactory.getLogger(SyncLauncher.class);

	// Base URI the Grizzly HTTP server will listen on
	public static final String BASE_URI = "http://localhost:8080/myapp/";

	private ServiceLocator locator;

	public static HttpServer startServer()
	{
		final ResourceConfig rc = new ResourceConfig().packages("org.nishen.resourcepartners");
		return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
	}

	public static void main(String[] args) throws Exception
	{
		SyncLauncher app = new SyncLauncher();
		app.run();
	}

	public void run() throws IOException
	{
		// list for injector modules
		List<Module> modules = new ArrayList<Module>();

		// module (main configuration)
		// modules.add(new ServletModule());
		modules.add(new SyncModule());

		// create the injector (guice)
		log.debug("creating injector");
		Injector injector = Guice.createInjector(modules);

		// create the locator (hk2)
		locator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
		log.debug("item: {}", locator.getName());

		// link guice with hk2
		GuiceBridge.getGuiceBridge().initializeGuiceBridge(locator);
		GuiceIntoHK2Bridge guiceBridge = locator.getService(GuiceIntoHK2Bridge.class);
		guiceBridge.bridgeGuiceInjector(injector);

		// start the server
		final HttpServer server = startServer();
		System.out.println(String.format("Jersey app started with WADL available at " +
		                                 "%sapplication.wadl\nHit enter to stop it...", BASE_URI));
		System.in.read();
		server.shutdown();
	}
}
