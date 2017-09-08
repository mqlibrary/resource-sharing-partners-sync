package org.nishen.resourcepartners.dao;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheImpl<T> implements Cache<T>
{
	private static final Logger log = LoggerFactory.getLogger(CacheImpl.class);

	private static final long CACHE_TTL = 1800;

	private ConcurrentMap<String, LocalDateTime> cacheExpire;

	private ConcurrentMap<String, T> cache;

	public CacheImpl()
	{
		cache = new ConcurrentHashMap<String, T>();
		cacheExpire = new ConcurrentHashMap<String, LocalDateTime>();

		log.debug("instantiated class: {}", this.getClass().getName());
	}

	@Override
	public Optional<T> get(String key)
	{
		if (!cacheExpire.containsKey(key) || LocalDateTime.now().isAfter(cacheExpire.get(key)))
			return Optional.empty();

		return Optional.ofNullable(cache.get(key));
	}

	@Override
	public void put(String key, T value)
	{
		cacheExpire.put(key, LocalDateTime.now().plus(CACHE_TTL, ChronoUnit.SECONDS));
		cache.put(key, value);
	}

	@Override
	public void expire(String key)
	{
		cacheExpire.remove(key);
		cache.remove(key);
	}

	@Override
	public void expireAll()
	{
		cacheExpire.clear();
		cache.clear();
	}
}
