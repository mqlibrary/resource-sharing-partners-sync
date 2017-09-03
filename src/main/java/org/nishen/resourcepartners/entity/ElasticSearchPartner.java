package org.nishen.resourcepartners.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "partner")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "nuc", "updated", "name", "enabled", "isoIll", "status", "emailMain", "emailIll", "phoneMain",
                       "phoneIll", "phoneFax", "suspensions", "addresses" })
public class ElasticSearchPartner implements ElasticSearchEntity, Serializable
{
	@XmlTransient
	private static final long serialVersionUID = 974331238066994902L;

	@XmlElement(name = "nuc")
	private String nuc;

	@XmlElement(name = "updated")
	private String updated;

	@XmlElement(name = "name")
	private String name;

	@XmlElement(name = "enabled")
	private boolean enabled;

	@XmlElement(name = "iso_ill")
	private boolean isoIll;

	@XmlElement(name = "status")
	private String status;

	@XmlElement(name = "email_main")
	private String emailMain;

	@XmlElement(name = "email_ill")
	private String emailIll;

	@XmlElement(name = "phone_main")
	private String phoneMain;

	@XmlElement(name = "phone_ill")
	private String phoneIll;

	@XmlElement(name = "phone_fax")
	private String phoneFax;

	@XmlElement(name = "suspensions")
	private Set<ElasticSearchSuspension> suspensions = new LinkedHashSet<ElasticSearchSuspension>();

	@XmlElement(name = "addresses")
	private List<ElasticSearchPartnerAddress> addresses = new ArrayList<ElasticSearchPartnerAddress>();

	@Override
	public String getElasticSearchId()
	{
		return nuc;
	}

	@Override
	public String getElasticSearchIndex()
	{
		return "partners";
	}

	@Override
	public String getElasticSearchType()
	{
		return "partner";
	}

	@Override
	public String getTime()
	{
		return updated;
	}

	public String getNuc()
	{
		return nuc;
	}

	public void setNuc(String nuc)
	{
		this.nuc = nuc;
	}

	public String getUpdated()
	{
		return updated;
	}

	public void setUpdated(String updated)
	{
		this.updated = updated;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public boolean isIsoIll()
	{
		return isoIll;
	}

	public void setIsoIll(boolean isoIll)
	{
		this.isoIll = isoIll;
	}

	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

	public String getEmailMain()
	{
		return emailMain;
	}

	public void setEmailMain(String emailMain)
	{
		this.emailMain = emailMain;
	}

	public String getEmailIll()
	{
		return emailIll;
	}

	public void setEmailIll(String emailIll)
	{
		this.emailIll = emailIll;
	}

	public String getPhoneMain()
	{
		return phoneMain;
	}

	public void setPhoneMain(String phoneMain)
	{
		this.phoneMain = phoneMain;
	}

	public String getPhoneIll()
	{
		return phoneIll;
	}

	public void setPhoneIll(String phoneIll)
	{
		this.phoneIll = phoneIll;
	}

	public String getPhoneFax()
	{
		return phoneFax;
	}

	public void setPhoneFax(String phoneFax)
	{
		this.phoneFax = phoneFax;
	}

	public Set<ElasticSearchSuspension> getSuspensions()
	{
		return suspensions;
	}

	public void setSuspensions(Set<ElasticSearchSuspension> suspensions)
	{
		this.suspensions = suspensions;
	}

	public List<ElasticSearchPartnerAddress> getAddresses()
	{
		return addresses;
	}

	public void setAddresses(List<ElasticSearchPartnerAddress> addresses)
	{
		this.addresses = addresses;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((addresses == null) ? 0 : addresses.hashCode());
		result = prime * result + ((emailIll == null) ? 0 : emailIll.hashCode());
		result = prime * result + ((emailMain == null) ? 0 : emailMain.hashCode());
		result = prime * result + (enabled ? 1231 : 1237);
		result = prime * result + (isoIll ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((nuc == null) ? 0 : nuc.hashCode());
		result = prime * result + ((phoneFax == null) ? 0 : phoneFax.hashCode());
		result = prime * result + ((phoneIll == null) ? 0 : phoneIll.hashCode());
		result = prime * result + ((phoneMain == null) ? 0 : phoneMain.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((suspensions == null) ? 0 : suspensions.hashCode());
		result = prime * result + ((updated == null) ? 0 : updated.hashCode());
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
		ElasticSearchPartner other = (ElasticSearchPartner) obj;
		if (addresses == null)
		{
			if (other.addresses != null)
				return false;
		}
		else if (!addresses.equals(other.addresses))
			return false;
		if (emailIll == null)
		{
			if (other.emailIll != null)
				return false;
		}
		else if (!emailIll.equals(other.emailIll))
			return false;
		if (emailMain == null)
		{
			if (other.emailMain != null)
				return false;
		}
		else if (!emailMain.equals(other.emailMain))
			return false;
		if (enabled != other.enabled)
			return false;
		if (isoIll != other.isoIll)
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		if (nuc == null)
		{
			if (other.nuc != null)
				return false;
		}
		else if (!nuc.equals(other.nuc))
			return false;
		if (phoneFax == null)
		{
			if (other.phoneFax != null)
				return false;
		}
		else if (!phoneFax.equals(other.phoneFax))
			return false;
		if (phoneIll == null)
		{
			if (other.phoneIll != null)
				return false;
		}
		else if (!phoneIll.equals(other.phoneIll))
			return false;
		if (phoneMain == null)
		{
			if (other.phoneMain != null)
				return false;
		}
		else if (!phoneMain.equals(other.phoneMain))
			return false;
		if (status == null)
		{
			if (other.status != null)
				return false;
		}
		else if (!status.equals(other.status))
			return false;
		if (suspensions == null)
		{
			if (other.suspensions != null)
				return false;
		}
		else if (!suspensions.equals(other.suspensions))
			return false;
		if (updated == null)
		{
			if (other.updated != null)
				return false;
		}
		else if (!updated.equals(other.updated))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("ElasticSearchPartner [nuc=");
		builder.append(nuc);
		builder.append(", updated=");
		builder.append(updated);
		builder.append(", name=");
		builder.append(name);
		builder.append(", enabled=");
		builder.append(enabled);
		builder.append(", isoIll=");
		builder.append(isoIll);
		builder.append(", status=");
		builder.append(status);
		builder.append(", emailMain=");
		builder.append(emailMain);
		builder.append(", emailIll=");
		builder.append(emailIll);
		builder.append(", phoneMain=");
		builder.append(phoneMain);
		builder.append(", phoneIll=");
		builder.append(phoneIll);
		builder.append(", phoneFax=");
		builder.append(phoneFax);
		builder.append(", suspensions=");
		builder.append(suspensions);
		builder.append(", addresses=");
		builder.append(addresses);
		builder.append("]");
		return builder.toString();
	}
}
