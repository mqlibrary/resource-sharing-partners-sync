package org.nishen.resourcepartners;

import java.io.IOException;
import java.net.URI;
import java.util.EnumSet;
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

	private static final URI BASE_URI = UriBuilder.fromUri("http://localhost:2020/myapp").build();

	private HttpServer server;

	public static void main(String[] args) throws Exception
	{
		java.util.logging.Logger.getLogger("org.glassfish.grizzly").setLevel(Level.WARNING);

		SyncLauncher server = new SyncLauncher();

		server.start();

		log.info("Server started - hit enter to stop it.");

		System.in.read();

		server.stop();
	}

	public SyncLauncher()
	{
		log.debug("instantiated class: {}", this.getClass().getName());
	}

	public HttpServer start() throws IOException
	{
		log.info("starting service: {}", SERVICE_NAME);

		// Create HttpServer
		final HttpServer serverLocal = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, false);

		final WebappContext context = new WebappContext(SERVICE_NAME, "/myapp");
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

		return server;
	}

	public void stop() throws Exception
	{
		server.shutdown();
	}

	public URI getBaseUri()
	{
		return BASE_URI;
	}
}
