package org.nishen.resourcepartners.entity;

import java.util.List;
import java.util.Map;

import org.nishen.resourcepartners.model.Partner;

public class SyncPayload
{
	private Map<String, Partner> changed;

	private Map<String, Partner> deleted;

	private Map<String, List<ElasticSearchChangeRecord>> changes;

	public SyncPayload(Map<String, Partner> changed, Map<String, Partner> deleted,
	                   Map<String, List<ElasticSearchChangeRecord>> changes)
	{
		this.changed = changed;
		this.deleted = deleted;
		this.changes = changes;
	}

	public Map<String, Partner> getChanged()
	{
		return changed;
	}

	public void setChanged(Map<String, Partner> changed)
	{
		this.changed = changed;
	}

	public Map<String, Partner> getDeleted()
	{
		return deleted;
	}

	public void setDeleted(Map<String, Partner> deleted)
	{
		this.deleted = deleted;
	}

	public Map<String, List<ElasticSearchChangeRecord>> getChanges()
	{
		return changes;
	}

	public void setChanges(Map<String, List<ElasticSearchChangeRecord>> changes)
	{
		this.changes = changes;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changed == null) ? 0 : changed.hashCode());
		result = prime * result + ((changes == null) ? 0 : changes.hashCode());
		result = prime * result + ((deleted == null) ? 0 : deleted.hashCode());
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
		SyncPayload other = (SyncPayload) obj;
		if (changed == null)
		{
			if (other.changed != null)
				return false;
		}
		else if (!changed.equals(other.changed))
			return false;
		if (changes == null)
		{
			if (other.changes != null)
				return false;
		}
		else if (!changes.equals(other.changes))
			return false;
		if (deleted == null)
		{
			if (other.deleted != null)
				return false;
		}
		else if (!deleted.equals(other.deleted))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("SyncPayload [changed=");
		builder.append(changed);
		builder.append(", deleted=");
		builder.append(deleted);
		builder.append(", changes=");
		builder.append(changes);
		builder.append("]");
		return builder.toString();
	}
}
