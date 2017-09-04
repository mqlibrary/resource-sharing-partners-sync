package org.nishen.resourcepartners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class SyncServletContextListener extends GuiceServletContextListener
{
	private static final Logger log = LoggerFactory.getLogger(SyncServletContextListener.class);

	public static Injector injector = Guice.createInjector(new ServletModule(), new SyncModule());

	public SyncServletContextListener()
	{
		log.debug("instantiated class: {}", this.getClass().getName());
	}

	@Override
	protected Injector getInjector()
	{
		return injector;
	}
}
