package org.nishen.resourcepartners.dao;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;

public class ConfigImpl implements Config
{
	private static final Logger log = LoggerFactory.getLogger(ConfigImpl.class);

	private String configId;

	private ConcurrentMap<String, String> config;

	private WebTarget elasticTarget;

	private ObjectMapper om = new ObjectMapper();

	@Inject
	public ConfigImpl(@Named("ws.elastic") Provider<WebTarget> elasticTargetProvider, @Assisted String configId)
	{
		this.elasticTarget = elasticTargetProvider.get();
		this.configId = configId;
		this.config = new ConcurrentHashMap<String, String>();
		try
		{
			Optional<Map<String, String>> fetched = fetchConfig();
			if (fetched.isPresent())
			{
				this.config.putAll(fetched.get());
			}
			else
			{
				saveConfig();
				log.info("created config: {}", configId);
			}
		}
		catch (Exception e)
		{
			log.error("unable to load config [{}]: {}", configId, e.getMessage(), e);
		}
	}

	@Override
	public Map<String, String> getAll()
	{
		return Collections.unmodifiableMap(config);
	}

	@Override
	public Optional<String> get(String key)
	{
		return Optional.ofNullable(config.get(key));
	}

	@Override
	public void set(String key, String value)
	{
		config.put(key, value);
		try
		{
			saveConfig();
		}
		catch (Exception e)
		{
			log.error("unable to save config[{}]: {}", configId + "." + key, e.getMessage(), e);
		}
	}

	@Override
	public void setAll(Map<String, String> config)
	{
		this.config.putAll(config);
		try
		{
			saveConfig();
		}
		catch (Exception e)
		{
			log.error("unable to save config[{}]: {}", configId, e.getMessage(), e);
		}
	}

	private void saveConfig() throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		String esConfig = mapper.writeValueAsString(config);

		WebTarget t = elasticTarget.path("config").path("scope").path(configId);
		Builder req = t.request(MediaType.APPLICATION_JSON);
		String result = req.put(Entity.entity(esConfig, MediaType.APPLICATION_JSON), String.class);

		log.debug("saveConfig: {}", result);
	}

	private Optional<Map<String, String>> fetchConfig()
	{
		Map<String, String> config = null;
		try
		{
			WebTarget t = elasticTarget.path("config").path("scope").path(configId).path("_source");
			String esConfig = t.request().accept(MediaType.APPLICATION_JSON).get(String.class);

			TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {};
			config = om.readValue(esConfig, typeRef);
		}
		catch (NotFoundException nfe)
		{
			log.info("config does not yet exist: {}", configId);
		}
		catch (Exception e)
		{
			log.error("unable to retrieve config[{}]: {}", configId, e.getMessage(), e);
		}

		return Optional.ofNullable(config);
	}
}
