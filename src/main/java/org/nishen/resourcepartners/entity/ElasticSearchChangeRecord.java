package org.nishen.resourcepartners.entity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "partner-change")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "time", "sourceSystem", "nuc", "field", "before", "after" })

public class ElasticSearchChangeRecord implements ElasticSearchEntity
{
	@XmlTransient
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	@XmlTransient
	private UUID id;

	@XmlElement(name = "time")
	private String time;

	@XmlElement(name = "source_system")
	private String sourceSystem;

	@XmlElement(name = "nuc")
	private String nuc;

	@XmlElement(name = "field")
	private String field;

	@XmlElement(name = "before", nillable = true)
	private String before;

	@XmlElement(name = "after", nillable = true)
	private String after;

	public ElasticSearchChangeRecord()
	{
		id = UUID.randomUUID();
		time = sdf.format(new Date());
	}

	public ElasticSearchChangeRecord(String sourceSystem, String nuc, String field, String before, String after)
	{
		this();
		this.sourceSystem = sourceSystem;
		this.nuc = nuc;
		this.field = field;
		this.before = before;
		this.after = after;
	}

	@Override
	public String getElasticSearchId()
	{
		return id.toString();
	}

	@Override
	public String getElasticSearchIndex()
	{
		return "partners";
	}

	@Override
	public String getElasticSearchType()
	{
		return "partner-change";
	}

	@Override
	public String getTime()
	{
		return time;
	}

	public UUID getId()
	{
		return id;
	}

	public void setId(UUID id)
	{
		this.id = id;
	}

	public String getSourceSystem()
	{
		return sourceSystem;
	}

	public void setSourceSystem(String sourceSystem)
	{
		this.sourceSystem = sourceSystem;
	}

	public String getNuc()
	{
		return nuc;
	}

	public void setNuc(String nuc)
	{
		this.nuc = nuc;
	}

	public String getField()
	{
		return field;
	}

	public void setField(String field)
	{
		this.field = field;
	}

	public String getBefore()
	{
		return before;
	}

	public void setBefore(String before)
	{
		this.before = before;
	}

	public String getAfter()
	{
		return after;
	}

	public void setAfter(String after)
	{
		this.after = after;
	}

	public void setTime(String time)
	{
		this.time = time;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((after == null) ? 0 : after.hashCode());
		result = prime * result + ((before == null) ? 0 : before.hashCode());
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((nuc == null) ? 0 : nuc.hashCode());
		result = prime * result + ((sourceSystem == null) ? 0 : sourceSystem.hashCode());
		result = prime * result + ((time == null) ? 0 : time.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ElasticSearchChangeRecord other = (ElasticSearchChangeRecord) obj;
		if (after == null)
		{
			if (other.after != null)
				return false;
		}
		else if (!after.equals(other.after))
			return false;
		if (before == null)
		{
			if (other.before != null)
				return false;
		}
		else if (!before.equals(other.before))
			return false;
		if (field == null)
		{
			if (other.field != null)
				return false;
		}
		else if (!field.equals(other.field))
			return false;
		if (id == null)
		{
			if (other.id != null)
				return false;
		}
		else if (!id.equals(other.id))
			return false;
		if (nuc == null)
		{
			if (other.nuc != null)
				return false;
		}
		else if (!nuc.equals(other.nuc))
			return false;
		if (sourceSystem == null)
		{
			if (other.sourceSystem != null)
				return false;
		}
		else if (!sourceSystem.equals(other.sourceSystem))
			return false;
		if (time == null)
		{
			if (other.time != null)
				return false;
		}
		else if (!time.equals(other.time))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "ElasticSearchChangeRecord [id=" + id + ", time=" + time + ", sourceSystem=" + sourceSystem + ", nuc=" +
		       nuc + ", field=" + field + ", before=" + before + ", after=" + after + "]";
	}
}
