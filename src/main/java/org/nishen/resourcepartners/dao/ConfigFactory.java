package org.nishen.resourcepartners.dao;

public interface ConfigFactory
{
	public Config fetch(String configId, boolean createIfAbsent);
}
