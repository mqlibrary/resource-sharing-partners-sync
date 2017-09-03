package org.nishen.resourcepartners.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.nishen.resourcepartners.entity.ElasticSearchEntity;
import org.nishen.resourcepartners.entity.ElasticSearchPartner;
import org.nishen.resourcepartners.util.DataUtils;
import org.nishen.resourcepartners.util.JaxbUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class ElasticSearchDAOImpl implements ElasticSearchDAO
{
	private static final Logger log = LoggerFactory.getLogger(ElasticSearchDAOImpl.class);

	private static final int SEARCH_SIZE = 10000;

	private Set<String> indices;

	private WebTarget elasticTarget;

	private ObjectMapper om;

	@Inject
	public ElasticSearchDAOImpl(@Named("ws.elastic") Provider<WebTarget> elasticTargetProvider)
	{
		this.elasticTarget = elasticTargetProvider.get();

		try
		{
			indices = getElasticSearchIndices();
			createElasticSearchIndex("config");
			createElasticSearchIndex("partners");
		}
		catch (IOException ioe)
		{
			log.error("unable to create index: {}", "partners");
		}

		this.om = new ObjectMapper();

		log.debug("instantiated class: {}", this.getClass().getName());
	}

	@Override
	public Optional<ElasticSearchPartner> getPartner(String id) throws IOException
	{
		if (!indices.contains("partners"))
			createElasticSearchIndex("partners");

		WebTarget t = elasticTarget.path("partners").path("partner").path(id).path("_source");

		ElasticSearchPartner partner = null;
		try
		{
			partner = t.request().accept(MediaType.APPLICATION_JSON).get(ElasticSearchPartner.class);
		}
		catch (NotFoundException nfe)
		{
			log.warn("partner does not exist: {}", id);
		}

		return Optional.ofNullable(partner);
	}

	@Override
	public Map<String, ElasticSearchPartner> getPartners() throws IOException
	{
		Map<String, ElasticSearchPartner> partners = new HashMap<String, ElasticSearchPartner>();

		if (!indices.contains("partners"))
			createElasticSearchIndex("partners");

		WebTarget t = elasticTarget.path("partners").path("partner").path("_search").queryParam("sort", "nuc")
		                           .queryParam("size", SEARCH_SIZE);

		String result = t.request().accept(MediaType.APPLICATION_JSON).get(String.class);

		try
		{
			JsonNode root = om.readTree(result);
			JsonNode partnerList = root.get("hits").get("hits");
			for (JsonNode p : partnerList)
			{
				String source = p.get("_source").toString();
				log.trace("source: {}", source);

				ElasticSearchPartner e = JaxbUtil.get(source, ElasticSearchPartner.class);
				log.trace("unmarshalled source: {}", e);

				partners.put(e.getNuc(), e);
			}
		}
		catch (IOException ioe)
		{
			log.error("failed to parse json search results: {}", ioe.getMessage(), ioe);
		}

		return partners;
	}

	@Override
	public void addEntity(ElasticSearchEntity esEntity) throws Exception
	{
		List<ElasticSearchEntity> esEntities = new ArrayList<>();

		esEntities.add(esEntity);

		addEntities(esEntities);
	}

	@Override
	public void addEntities(Collection<? extends ElasticSearchEntity> esEntities) throws Exception
	{
		if (esEntities == null || esEntities.size() == 0)
		{
			log.info("ESDAO saveData: no data to save");

			return;
		}

		indices = getElasticSearchIndices();

		int count = 0;
		StringWriter out = new StringWriter();
		for (ElasticSearchEntity e : esEntities)
		{
			if (!indices.contains(e.getElasticSearchIndex()))
				createElasticSearchIndex(e.getElasticSearchIndex());

			String pattern = "{\"update\": { \"_index\": \"%s\", \"_type\": \"%s\", \"_id\": \"%s\"}}\n";
			Object[] args = new String[3];
			args[0] = e.getElasticSearchIndex();
			args[1] = e.getElasticSearchType();
			args[2] = e.getElasticSearchId();

			out.append(String.format(pattern, args));
			out.append("{ \"doc\": ");
			out.append(JaxbUtil.format(e));
			out.append(", \"doc_as_upsert\": true }");
			out.append("\n");

			count++;
		}

		if (!out.toString().isEmpty())
		{
			log.info("posting data: {}", count);
			postBulkData(out.toString());
		}
	}

	@Override
	public void delEntity(ElasticSearchEntity esEntity)
	{
		WebTarget t = elasticTarget.path(esEntity.getElasticSearchIndex()).path(esEntity.getElasticSearchType())
		                           .path(esEntity.getElasticSearchId());

		try
		{
			t.request().accept(MediaType.APPLICATION_JSON).delete();
		}
		catch (NotFoundException nfe)
		{
			log.warn("attempting to delete entity that does not exist: {}", esEntity.getElasticSearchId());
		}
	}

	@Override
	public void delEntities(Collection<? extends ElasticSearchEntity> esEntities) throws Exception
	{
		if (esEntities == null || esEntities.size() == 0)
		{
			log.debug("elasticsearch delete entities: no entities to delete");

			return;
		}

		int count = 0;
		StringWriter out = new StringWriter();
		for (ElasticSearchEntity e : esEntities)
		{
			String pattern = "{\"delete\": { \"_index\": \"%s\", \"_type\": \"%s\", \"_id\": \"%s\"}}\n";
			Object[] args = new String[3];
			args[0] = e.getElasticSearchIndex();
			args[1] = e.getElasticSearchType();
			args[2] = e.getElasticSearchId();

			out.append(String.format(pattern, args));
			out.append("\n");

			count++;
		}

		if (!out.toString().isEmpty())
		{
			log.info("posting data: {}", count);
			postBulkData(out.toString());
		}
	}

	private Set<String> getElasticSearchIndices()
	{
		if (indices != null && !indices.isEmpty())
			return indices;

		indices = new HashSet<String>();

		WebTarget t = elasticTarget.path("_cat/indices").queryParam("h", "index");
		Builder req = t.request(MediaType.TEXT_PLAIN);
		String result = req.get(String.class);

		try (Scanner scanner = new Scanner(result))
		{
			while (scanner.hasNextLine())
			{
				String index = scanner.nextLine().trim();
				indices.add(index);
			}
		}
		catch (Exception e)
		{
			log.error("error obtaining index list: {}", e.getMessage(), e);
		}

		return indices;
	}

	private synchronized void createElasticSearchIndex(String index) throws IOException
	{
		if (indices.contains(index))
			return;

		WebTarget t = elasticTarget.path(index);
		Builder req = t.request(MediaType.APPLICATION_JSON);

		String mapping = "/mapping-" + index + ".json";
		InputStream in = ClassLoader.class.getResourceAsStream(mapping);

		if (in == null)
		{
			log.error("unable to load input stream: {}", mapping);
			throw new IOException("unable to load input stream: " + mapping);
		}

		String data = DataUtils.extract(in);

		String result = req.put(Entity.entity(data, MediaType.APPLICATION_JSON), String.class);

		indices.add(index);

		log.info("created index: {}", index);
		log.debug("posted data:\n{}", data.toString());
		log.debug("result: {}", result);
	}

	private String postBulkData(String data)
	{
		log.debug("posting data:\n{}", data.toString());

		WebTarget t = elasticTarget.path("_bulk");
		Builder req = t.request(MediaType.APPLICATION_JSON);
		String result = req.post(Entity.entity(data, MediaType.APPLICATION_JSON), String.class);

		log.debug("bulk result: {}", result);

		t = elasticTarget.path("_refresh");
		req = t.request(MediaType.APPLICATION_JSON);
		result = req.post(Entity.text(""), String.class);

		log.debug("refresh result: {}", result);

		return result;
	}
}
