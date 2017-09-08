
package org.nishen.resourcepartners.dao;

import java.util.Optional;

public interface Cache<T>
{
	public Optional<T> get(String key);

	public void put(String key, T value);

	public void expire(String key);

	public void expireAll();
}
