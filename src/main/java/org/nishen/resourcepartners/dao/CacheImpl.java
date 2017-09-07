package org.nishen.resourcepartners.dao;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nishen.resourcepartners.model.Partner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheImpl implements Cache
{
	private static final Logger log = LoggerFactory.getLogger(CacheImpl.class);

	private static final long CACHE_TTL = 1800;

	private ConcurrentMap<String, LocalDateTime> cacheExpire;
	
	private ConcurrentMap<String, ConcurrentMap<String, Partner>> cache;

	public CacheImpl()
	{
		cache = new ConcurrentHashMap<String, ConcurrentMap<String, Partner>>();
		cacheExpire = new ConcurrentHashMap<String, LocalDateTime>();

		log.debug("instantiated class: {}", this.getClass().getName());
	}

	public Optional<ConcurrentMap<String, Partner>> get(String key)
	{
		if (!cacheExpire.containsKey(key) || LocalDateTime.now().isAfter(cacheExpire.get(key)))
			return Optional.empty();

		return Optional.ofNullable(cache.get(key));
	}

	public void put(String key, ConcurrentMap<String, Partner> value)
	{
		cacheExpire.put(key, LocalDateTime.now().plus(CACHE_TTL, ChronoUnit.SECONDS));
		cache.put(key, value);
	}
}
