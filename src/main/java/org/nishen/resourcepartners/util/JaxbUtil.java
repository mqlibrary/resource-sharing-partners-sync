package org.nishen.resourcepartners.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.nishen.resourcepartners.entity.ElasticSearchChangeRecord;
import org.nishen.resourcepartners.entity.ElasticSearchPartner;
import org.nishen.resourcepartners.entity.ElasticSearchPartnerAddress;
import org.nishen.resourcepartners.entity.ElasticSearchSuspension;
import org.nishen.resourcepartners.model.Address;
import org.nishen.resourcepartners.model.Addresses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaxbUtil
{
	private static final Logger log = LoggerFactory.getLogger(JaxbUtil.class);

	private static JAXBContext context = null;

	private static Map<String, Marshaller> marshallers = null;

	private static Map<String, Unmarshaller> unmarshallers = null;

	static
	{
		marshallers = new HashMap<String, Marshaller>();

		unmarshallers = new HashMap<String, Unmarshaller>();

		try
		{
			Class<?>[] classes = new Class<?>[] { ElasticSearchPartner.class, ElasticSearchPartnerAddress.class,
			                                      ElasticSearchChangeRecord.class, ElasticSearchSuspension.class,
			                                      Addresses.class, Address.class };
			context = JAXBContext.newInstance(classes);
		}
		catch (Exception e)
		{
			log.error("failed to initialise jaxbcontext: {}", e.getMessage());
		}
	}

	public static Marshaller getMarshaller()
	{
		Marshaller marshaller = marshallers.get(Thread.currentThread().getName());
		if (marshaller == null)
		{
			synchronized (marshallers)
			{
				if (marshaller == null)
				{
					try
					{
						marshaller = context.createMarshaller();
						marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
						marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
						marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
						marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
						marshaller.setProperty(MarshallerProperties.JSON_ATTRIBUTE_PREFIX, "@");
						marshallers.put(Thread.currentThread().getName(), marshaller);
					}
					catch (Exception e)
					{
						log.error("failed to create marshaller [{}]: {}", Thread.currentThread().getName(),
						          e.getMessage());
					}
				}
			}
		}

		return marshaller;
	}

	public static Unmarshaller getUnmarshaller()
	{
		Unmarshaller unmarshaller = unmarshallers.get(Thread.currentThread().getName());
		if (unmarshaller == null)
		{
			synchronized (unmarshallers)
			{
				if (unmarshaller == null)
				{
					try
					{
						unmarshaller = context.createUnmarshaller();
						unmarshaller.setProperty(UnmarshallerProperties.MEDIA_TYPE, "application/json");
						unmarshaller.setProperty(UnmarshallerProperties.JSON_INCLUDE_ROOT, false);
						unmarshaller.setProperty(UnmarshallerProperties.UNMARSHALLING_CASE_INSENSITIVE, true);
						unmarshaller.setProperty(UnmarshallerProperties.JSON_ATTRIBUTE_PREFIX, "@");

						unmarshallers.put(Thread.currentThread().getName(), unmarshaller);
					}
					catch (Exception e)
					{
						log.error("failed to create unmarshaller [{}]: {}", Thread.currentThread().getName(),
						          e.getMessage());
					}
				}
			}
		}

		return unmarshaller;
	}

	public static <T> T get(String json, Class<T> objectClass)
	{
		T item = null;

		try
		{
			Unmarshaller u = getUnmarshaller();

			ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes());
			JAXBElement<T> result = u.unmarshal(new StreamSource(is), objectClass);
			item = result.getValue();
		}
		catch (Exception e)
		{
			log.error("failed to obtain ElasticSearchPartner representation: {}", e.getMessage(), e);
		}

		return item;
	}

	public static <T> String format(T item)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		format(item, out);

		return new String(out.toByteArray());
	}

	public static <T> void format(T item, OutputStream out)
	{
		try
		{
			Marshaller m = getMarshaller();

			m.marshal(item, out);
		}
		catch (Exception e)
		{
			log.error("failed to obtain Address representation: {}", e.getMessage(), e);
		}
	}
}