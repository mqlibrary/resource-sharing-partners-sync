
package org.nishen.resourcepartners.dao;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import org.nishen.resourcepartners.model.Partner;

public interface Cache
{
	public Optional<ConcurrentMap<String, Partner>> get(String key);

	public void put(String key, ConcurrentMap<String, Partner> value);
}
