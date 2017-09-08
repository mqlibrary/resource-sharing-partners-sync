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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaxbUtilModel
{
	private static final Logger log = LoggerFactory.getLogger(JaxbUtilModel.class);

	private static final String JAXB_PACKAGE = "org.nishen.resourcepartners.model";

	private static JAXBContext context = null;

	private static Map<String, Marshaller> marshallers = null;

	private static Map<String, Unmarshaller> unmarshallers = null;

	static
	{
		marshallers = new HashMap<String, Marshaller>();

		unmarshallers = new HashMap<String, Unmarshaller>();

		try
		{
			context = JAXBContext.newInstance(JAXB_PACKAGE);
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
						unmarshaller.setProperty(UnmarshallerProperties.JSON_INCLUDE_ROOT, Boolean.FALSE);
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
			log.error("failed to obtain representation [{}]: {}", objectClass.getName(), e.getMessage(), e);
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
		String classname = item != null ? item.getClass().getName() : "null";
		try
		{
			Marshaller m = getMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);

			m.marshal(item, out);
		}
		catch (Exception e)
		{
			log.error("failed to obtain {} representation: {}", classname, e.getMessage(), e);
		}
	}

	public static <T> String formatPretty(T item)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		formatPretty(item, out);

		return new String(out.toByteArray());
	}

	public static <T> void formatPretty(T item, OutputStream out)
	{
		String classname = item != null ? item.getClass().getName() : "null";
		try
		{
			Marshaller m = getMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			m.marshal(item, out);
		}
		catch (Exception e)
		{
			log.error("failed to obtain {} representation: {}", classname, e.getMessage(), e);
		}
	}
}