package org.nishen.resourcepartners.entity;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.nishen.resourcepartners.model.Address;

@XmlRootElement(name = "address")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "addressStatus", "addressType", "addressDetail" })
public class ElasticSearchPartnerAddress implements Serializable
{
	@XmlTransient
	private static final long serialVersionUID = -7788532956616833550L;

	@XmlElement(name = "address_status")
	private String addressStatus;

	@XmlElement(name = "address_type")
	private String addressType;

	@XmlElement(name = "address_detail")
	private Address addressDetail;

	public String getAddressStatus()
	{
		return addressStatus;
	}

	public void setAddressStatus(String addressStatus)
	{
		this.addressStatus = addressStatus;
	}

	public String getAddressType()
	{
		return addressType;
	}

	public void setAddressType(String addressType)
	{
		this.addressType = addressType;
	}

	public Address getAddressDetail()
	{
		return addressDetail;
	}

	public void setAddressDetail(Address addressDetail)
	{
		this.addressDetail = addressDetail;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((addressDetail == null) ? 0 : addressDetail.hashCode());
		result = prime * result + ((addressStatus == null) ? 0 : addressStatus.hashCode());
		result = prime * result + ((addressType == null) ? 0 : addressType.hashCode());
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
		ElasticSearchPartnerAddress other = (ElasticSearchPartnerAddress) obj;
		if (addressDetail == null)
		{
			if (other.addressDetail != null)
				return false;
		}
		else if (!addressDetail.equals(other.addressDetail))
			return false;
		if (addressStatus == null)
		{
			if (other.addressStatus != null)
				return false;
		}
		else if (!addressStatus.equals(other.addressStatus))
			return false;
		if (addressType == null)
		{
			if (other.addressType != null)
				return false;
		}
		else if (!addressType.equals(other.addressType))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("ElasticSearchPartnerAddress [addressStatus=");
		builder.append(addressStatus);
		builder.append(", addressType=");
		builder.append(addressType);
		builder.append(", addressDetail=");
		builder.append(addressDetail);
		builder.append("]");
		return builder.toString();
	}
}
