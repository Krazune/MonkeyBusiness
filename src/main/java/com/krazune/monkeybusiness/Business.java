package com.krazune.monkeybusiness;

import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

public class Business
{
	private final WorldPoint location;
	private final BusinessType type;

	private final Client client;

	private RuneLiteObject object;

	public Business(Client client, WorldPoint location, BusinessType type)
	{
		this.client = client;
		this.location = location;
		this.type = type;
	}

	public WorldPoint getLocation()
	{
		return location;
	}

	public BusinessType getType()
	{
		return type;
	}

	public boolean spawn()
	{
		if (object != null)
		{
			object.setActive(false);
		}

		LocalPoint localLocation = LocalPoint.fromWorld(client, location);

		if (localLocation == null)
		{
			return false;
		}

		RuneLiteObject newObject = client.createRuneLiteObject();
		Model newModel = loadModel();

		if (newModel == null)
		{
			return false;
		}

		newObject.setLocation(localLocation, location.getPlane());
		newObject.setModel(newModel);
		newObject.setActive(true);

		this.object = newObject;

		return true;
	}

	public void despawn()
	{
		if (object == null)
		{
			return;
		}

		object.setActive(false);

		object = null;
	}

	private Model loadModel()
	{
		return client.loadModel(type.getValue());
	}
}
