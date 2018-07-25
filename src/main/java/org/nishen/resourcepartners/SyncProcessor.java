package org.nishen.resourcepartners;

import java.util.List;
import java.util.Optional;

import org.nishen.resourcepartners.entity.ElasticSearchChangeRecord;
import org.nishen.resourcepartners.entity.ElasticSearchPartner;
import org.nishen.resourcepartners.entity.SyncPayload;
import org.nishen.resourcepartners.model.Partner;

public interface SyncProcessor
{
	public Optional<SyncPayload> sync(boolean preview) throws SyncException;

	public List<ElasticSearchChangeRecord> comparePartners(Partner a, Partner b);

	public Partner makePartner(ElasticSearchPartner p) throws SyncException;
}