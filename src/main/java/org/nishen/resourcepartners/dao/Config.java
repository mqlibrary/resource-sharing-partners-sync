package org.nishen.resourcepartners.dao;

import java.util.Map;
import java.util.Optional;

public interface Config
{
	public static final String ES_INDEX = "partner-configs";
	
	public static final String ES_TYPE = "config"; 
	
	public boolean isEmpty();

	public Map<String, String> getAll();

	public Optional<String> get(String key);

	public void set(String key, String value);

	public void setAll(Map<String, String> config);
}
