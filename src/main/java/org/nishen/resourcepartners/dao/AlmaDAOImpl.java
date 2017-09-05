package org.nishen.resourcepartners.dao;

import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class AlmaDAOImpl implements AlmaDAO
{
	private static final Logger log = LoggerFactory.getLogger(AlmaDAOImpl.class);

	private static final int LIMIT = 100;

	private TaskFactory taskFactory;

	private Provider<WebTarget> webTargetProvider;

	private WebTarget target;

	private String apikey;

	@Inject
	public AlmaDAOImpl(TaskFactory taskFactory, @Named("ws.alma") Provider<WebTarget> webTargetProvider,
	                   @Named("ws.alma.key") String apikey)
	{
		this.taskFactory = taskFactory;
		this.webTargetProvider = webTargetProvider;
		this.apikey = apikey;

		target = webTargetProvider.get();
		target = target.path("users").queryParam("apikey", apikey).queryParam("limit", LIMIT);

		log.debug("instantiated class: {}", this.getClass().getName());
	}
}
