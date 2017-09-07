package org.nishen.resourcepartners;

import java.util.Map;
import java.util.Optional;

import org.nishen.resourcepartners.model.Partner;

public interface SyncProcessor
{
	public Optional<Map<String, Partner>> sync(boolean preview) throws SyncException;
}