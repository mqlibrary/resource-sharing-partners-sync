package org.nishen.resourcepartners.entity;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "suspension")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "suspensionAdded", "suspensionStatus", "suspensionStart", "suspensionEnd", "suspensionCode",
                       "suspensionReason" })
public class ElasticSearchSuspension implements Serializable
{
	@XmlTransient
	private static final long serialVersionUID = 5265098248606932836L;

	@XmlTransient
	public static final String SUSPENDED = "suspended";

	@XmlTransient
	public static final String NOT_SUSPENDED = "not suspended";

	@XmlTransient
	public static final String UNKNOWN = "unknown";

	@XmlElement(name = "suspension_added")
	private String suspensionAdded;

	@XmlElement(name = "suspension_status")
	private String suspensionStatus;

	@XmlElement(name = "suspension_start")
	private String suspensionStart;

	@XmlElement(name = "suspension_end")
	private String suspensionEnd;

	@XmlElement(name = "suspension_code")
	private String suspensionCode;

	@XmlElement(name = "suspension_reason")
	private String suspensionReason;

	public String getSuspensionAdded()
	{
		return suspensionAdded;
	}

	public void setSuspensionAdded(String suspensionAdded)
	{
		this.suspensionAdded = suspensionAdded;
	}

	public String getSuspensionStatus()
	{
		return suspensionStatus;
	}

	public void setSuspensionStatus(String suspensionStatus)
	{
		this.suspensionStatus = suspensionStatus;
	}

	public String getSuspensionStart()
	{
		return suspensionStart;
	}

	public void setSuspensionStart(String suspensionStart)
	{
		this.suspensionStart = suspensionStart;
	}

	public String getSuspensionEnd()
	{
		return suspensionEnd;
	}

	public void setSuspensionEnd(String suspensionEnd)
	{
		this.suspensionEnd = suspensionEnd;
	}

	public String getSuspensionCode()
	{
		return suspensionCode;
	}

	public void setSuspensionCode(String suspensionCode)
	{
		this.suspensionCode = suspensionCode;
	}

	public String getSuspensionReason()
	{
		return suspensionReason;
	}

	public void setSuspensionReason(String suspensionReason)
	{
		this.suspensionReason = suspensionReason;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((suspensionAdded == null) ? 0 : suspensionAdded.hashCode());
		result = prime * result + ((suspensionCode == null) ? 0 : suspensionCode.hashCode());
		result = prime * result + ((suspensionEnd == null) ? 0 : suspensionEnd.hashCode());
		result = prime * result + ((suspensionReason == null) ? 0 : suspensionReason.hashCode());
		result = prime * result + ((suspensionStart == null) ? 0 : suspensionStart.hashCode());
		result = prime * result + ((suspensionStatus == null) ? 0 : suspensionStatus.hashCode());
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
		ElasticSearchSuspension other = (ElasticSearchSuspension) obj;
		if (suspensionAdded == null)
		{
			if (other.suspensionAdded != null)
				return false;
		}
		else if (!suspensionAdded.equals(other.suspensionAdded))
			return false;
		if (suspensionCode == null)
		{
			if (other.suspensionCode != null)
				return false;
		}
		else if (!suspensionCode.equals(other.suspensionCode))
			return false;
		if (suspensionEnd == null)
		{
			if (other.suspensionEnd != null)
				return false;
		}
		else if (!suspensionEnd.equals(other.suspensionEnd))
			return false;
		if (suspensionReason == null)
		{
			if (other.suspensionReason != null)
				return false;
		}
		else if (!suspensionReason.equals(other.suspensionReason))
			return false;
		if (suspensionStart == null)
		{
			if (other.suspensionStart != null)
				return false;
		}
		else if (!suspensionStart.equals(other.suspensionStart))
			return false;
		if (suspensionStatus == null)
		{
			if (other.suspensionStatus != null)
				return false;
		}
		else if (!suspensionStatus.equals(other.suspensionStatus))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("ElasticSearchSuspension [suspensionAdded=");
		builder.append(suspensionAdded);
		builder.append(", suspensionStatus=");
		builder.append(suspensionStatus);
		builder.append(", suspensionStart=");
		builder.append(suspensionStart);
		builder.append(", suspensionEnd=");
		builder.append(suspensionEnd);
		builder.append(", suspensionCode=");
		builder.append(suspensionCode);
		builder.append(", suspensionReason=");
		builder.append(suspensionReason);
		builder.append("]");
		return builder.toString();
	}
}
