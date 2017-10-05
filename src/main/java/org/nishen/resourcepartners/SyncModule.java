package org.nishen.resourcepartners;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.xml.bind.Marshaller;

import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jaxb.internal.JaxbMessagingBinder;
import org.glassfish.jersey.jaxb.internal.JaxbParamConverterBinder;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.nishen.resourcepartners.dao.AlmaDAO;
import org.nishen.resourcepartners.dao.AlmaDAOFactory;
import org.nishen.resourcepartners.dao.AlmaDAOImpl;
import org.nishen.resourcepartners.dao.Cache;
import org.nishen.resourcepartners.dao.CacheImpl;
import org.nishen.resourcepartners.dao.Config;
import org.nishen.resourcepartners.dao.ConfigFactory;
import org.nishen.resourcepartners.dao.ConfigImpl;
import org.nishen.resourcepartners.dao.ElasticSearchDAO;
import org.nishen.resourcepartners.dao.ElasticSearchDAOImpl;
import org.nishen.resourcepartners.entity.SyncPayload;
import org.nishen.resourcepartners.model.Partner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;

public class SyncModule extends AbstractModule
{
	private static final Logger log = LoggerFactory.getLogger(SyncModule.class);

	@Override
	protected void configure()
	{
		// bind instances
		bind(ElasticSearchDAO.class).to(ElasticSearchDAOImpl.class).in(Scopes.SINGLETON);

		bind(Cache.class).to(CacheImpl.class).in(Scopes.SINGLETON);

		FactoryModuleBuilder factoryModuleBuilder = new FactoryModuleBuilder();
		install(factoryModuleBuilder.implement(Config.class, ConfigImpl.class).build(ConfigFactory.class));
		install(factoryModuleBuilder.implement(AlmaDAO.class, AlmaDAOImpl.class).build(AlmaDAOFactory.class));
		install(factoryModuleBuilder.implement(SyncProcessor.class, SyncProcessorImpl.class)
		                            .build(SyncProcessorFactory.class));
	}

	@Provides
	@Singleton
	@Named("ws.elastic")
	protected WebTarget provideWebTargetElastic() throws Exception
	{
		String usr = System.getenv("ELASTIC_USR");
		String pwd = System.getenv("ELASTIC_PWD");
		HttpAuthenticationFeature auth = HttpAuthenticationFeature.basic(usr, pwd);

		SSLContext sslcontext = SSLContext.getInstance("TLS");
		sslcontext.init(null, new TrustManager[] { new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
			{}

			public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
			{}

			public X509Certificate[] getAcceptedIssuers()
			{
				return new X509Certificate[0];
			}
		} }, new java.security.SecureRandom());

		Client client = ClientBuilder.newBuilder().sslContext(sslcontext).hostnameVerifier((s1, s2) -> true)
		                             .register(new JaxbMessagingBinder()).register(new JaxbParamConverterBinder())
		                             .register(new MoxyJsonFeature()).register(auth)
		                             .property(UnmarshallerProperties.JSON_ATTRIBUTE_PREFIX, "@")
		                             .property(UnmarshallerProperties.JSON_INCLUDE_ROOT, false)
		                             .property(MarshallerProperties.JSON_ATTRIBUTE_PREFIX, "@")
		                             .property(Marshaller.JAXB_ENCODING, "UTF-8").build();

		WebTarget elasticTarget = client.target(System.getenv("ELASTIC_URL"));

		return elasticTarget;
	}

	@Provides
	@Singleton
	@Named("ws.alma")
	protected WebTarget provideWebTargetAlma()
	{
		String url = System.getenv("ALMA_URL");

		Client client =
		        ClientBuilder.newClient().register(new JaxbMessagingBinder()).register(new JaxbParamConverterBinder());
		log.info("using alma api: {}", url);
		WebTarget almaTarget = client.target(url);

		return almaTarget;
	}

	@Provides
	@Singleton
	protected Cache<ConcurrentMap<String, Partner>> providePartnerCache()
	{
		Cache<ConcurrentMap<String, Partner>> partnerCache = new CacheImpl<ConcurrentMap<String, Partner>>();

		return partnerCache;
	}

	@Provides
	@Singleton
	protected Cache<SyncPayload> provideSyncPayloadCache()
	{
		Cache<SyncPayload> syncPayloadCache = new CacheImpl<SyncPayload>();

		return syncPayloadCache;
	}
}
