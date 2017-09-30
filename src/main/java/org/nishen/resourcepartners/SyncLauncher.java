package org.nishen.resourcepartners;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.servlet.GuiceFilter;

public class SyncLauncher
{
	private static final Logger log = LoggerFactory.getLogger(SyncLauncher.class);

	private static final String SERVICE_NAME = "Resource Sharing Partners Sync Service";

	private HttpServer server;

	public static void main(String[] args) throws Exception
	{
		// hide grizzly warnings
		java.util.logging.Logger.getLogger("org.glassfish.grizzly").setLevel(Level.WARNING);

		List<String> configErrors = new ArrayList<String>();

		String esUrl = System.getenv("ELASTIC_URL");
		String esUsr = System.getenv("ELASTIC_USR");
		String esPwd = System.getenv("ELASTIC_PWD");

		String almaUrl = System.getenv("ALMA_URL");

		String syncHost = System.getenv("SYNC_HOST");
		String syncPort = System.getenv("SYNC_PORT");
		String syncPath = System.getenv("SYNC_PATH");

		if (esUrl == null)
			configErrors.add("required environment setting missing: ELASTIC_URL");

		if (esUsr == null)
			configErrors.add("required environment setting missing: ELASTIC_USR");

		if (esPwd == null)
			configErrors.add("required environment setting missing: ELASTIC_PWD");

		if (almaUrl == null)
			configErrors.add("required environment setting missing: ALMA_URL");

		if (syncHost == null)
			configErrors.add("required environment setting missing: SYNC_HOST");

		if (syncPort == null)
			configErrors.add("required environment setting missing: SYNC_PORT");

		if (syncPath == null)
			configErrors.add("required environment setting missing: SYNC_PATH");

		if (configErrors.size() > 0)
		{
			for (String error : configErrors)
				System.out.println(error);

			return;
		}

		SyncLauncher server = new SyncLauncher();

		server.start(syncHost, syncPort, syncPath);

		log.info("Server started - hit enter to stop it.");

		System.in.read();

		server.stop();

		log.info("Server stopped.");
	}

	public SyncLauncher()
	{
		log.debug("instantiated class: {}", this.getClass().getName());
	}

	public HttpServer start(String host, String port, String path) throws IOException
	{
		log.info("starting service: {}", SERVICE_NAME);

		URI BASE_URI = UriBuilder.fromUri(String.format("http://%s:%s/%s", host, port, path)).build();

		// Create HttpServer
		final HttpServer serverLocal = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, false);

		final WebappContext context = new WebappContext(SERVICE_NAME, "/" + path);
		context.addListener(SyncServletContextListener.class);

		// Initialize and register Jersey ServletContainer
		ServletRegistration servletRegistration = context.addServlet("ServletContainer", ServletContainer.class);
		servletRegistration.addMapping("/*");
		servletRegistration.setInitParameter("javax.ws.rs.Application", "org.nishen.resourcepartners.SyncApp");

		// Initialize and register GuiceFilter
		final FilterRegistration registration = context.addFilter("GuiceFilter", GuiceFilter.class);
		registration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

		context.deploy(serverLocal);

		serverLocal.start();

		server = serverLocal;

		log.info("serving at: {}", BASE_URI.toString());

		return server;
	}

	public void stop() throws Exception
	{
		server.shutdown();
	}
}
