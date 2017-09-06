package org.nishen.resourcepartners;

public class SyncException extends Exception
{
	private static final long serialVersionUID = -2153804468705212252L;

	public SyncException()
	{
		super();
	}

	public SyncException(String msg)
	{
		super(msg);
	}

	public SyncException(Throwable t)
	{
		super(t);
	}
}
