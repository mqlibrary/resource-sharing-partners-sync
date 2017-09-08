package org.nishen.resourcepartners;

import java.util.Optional;

import org.nishen.resourcepartners.entity.SyncPayload;

public interface SyncProcessor
{
	public Optional<SyncPayload> sync(boolean preview) throws SyncException;
}