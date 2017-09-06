package org.nishen.resourcepartners.dao;

import java.util.concurrent.ConcurrentMap;

import org.nishen.resourcepartners.SyncException;
import org.nishen.resourcepartners.model.Partner;

public interface AlmaDAO
{
	public ConcurrentMap<String, Partner> getPartners() throws SyncException;
}
