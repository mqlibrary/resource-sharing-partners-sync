package org.nishen.resourcepartners;

import com.google.inject.assistedinject.Assisted;

public interface SyncProcessorFactory
{
	public SyncProcessor create(@Assisted("nuc") String nuc, @Assisted("apikey") String apikey);
}
