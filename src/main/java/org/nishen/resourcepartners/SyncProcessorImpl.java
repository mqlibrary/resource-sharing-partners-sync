package org.nishen.resourcepartners;

import org.nishen.resourcepartners.dao.AlmaDAO;
import org.nishen.resourcepartners.dao.AlmaDAOFactory;
import org.nishen.resourcepartners.dao.ElasticSearchDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class SyncProcessorImpl implements SyncProcessor
{
	private static final Logger log = LoggerFactory.getLogger(SyncProcessorImpl.class);

	private ElasticSearchDAO elastic;

	private AlmaDAO alma;

	private String nuc;

	@Inject
	public SyncProcessorImpl(ElasticSearchDAO elastic, AlmaDAOFactory almaFactory, @Assisted("nuc") String nuc,
	                         @Assisted("apikey") String apikey)
	{
		this.elastic = elastic;
		this.alma = almaFactory.create(apikey);
		this.nuc = nuc;

		log.debug("instantiated class: {}", this.getClass().getName());
	}

	@Override
	public void sync()
	{
		log.info("sync with [{}]: {}", nuc);
	}
}
