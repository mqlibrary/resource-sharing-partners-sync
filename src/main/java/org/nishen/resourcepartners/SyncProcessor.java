package org.nishen.resourcepartners;

import java.util.Optional;

import org.nishen.resourcepartners.entity.ElasticSearchPartner;
import org.nishen.resourcepartners.entity.SyncPayload;
import org.nishen.resourcepartners.model.Partner;

public interface SyncProcessor
{
	public Optional<SyncPayload> sync(boolean preview) throws SyncException;

	public Partner makePartner(ElasticSearchPartner e);
}