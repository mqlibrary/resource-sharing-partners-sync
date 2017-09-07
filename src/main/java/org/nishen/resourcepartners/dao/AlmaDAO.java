package org.nishen.resourcepartners.dao;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import org.nishen.resourcepartners.SyncException;
import org.nishen.resourcepartners.model.Partner;

public interface AlmaDAO
{
	public ConcurrentMap<String, Partner> getPartners() throws SyncException;

	public Optional<Partner> getPartner(String nuc) throws SyncException;

	public void savePartner(Partner p) throws SyncException;

	public void savePartners(Map<String, Partner> partner) throws SyncException;
}
