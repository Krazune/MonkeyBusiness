package com.krazune.monkeybusiness;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

public class BusinessManager
{
	private Client client;

	private MonkeyBusinessPluginConfig config;

	private Set<RuneLiteObject> businessObjects;
	private Map<Integer, Map<Integer, Map<Integer, RuneLiteObject>>> businessObjectsWorldPointMap; // X, Y, and Plane.

	@Inject
	public BusinessManager(Client client, MonkeyBusinessPluginConfig config)
	{
		this.client = client;
		this.config = config;

		businessObjects = new HashSet<>();
		businessObjectsWorldPointMap = new HashMap<>();
	}

	public void doBusiness(WorldPoint worldPoint)
	{
		if (!worldPointIsEmpty(worldPoint))
		{
			return;
		}

		// WIP
		int modelId = getRandomModelIdOrNothing();

		if (modelId == -1)
		{
			return;
		}

		RuneLiteObject newBusinessObject = createBusinessObject(modelId);

		if (newBusinessObject == null)
		{
			return;
		}

		LocalPoint newBusinessLocalPoint = LocalPoint.fromWorld(client, worldPoint);

		newBusinessObject.setLocation(newBusinessLocalPoint, worldPoint.getPlane());
		newBusinessObject.setActive(true);

		cacheBusinessObject(newBusinessObject);
	}

	private int getRandomModelIdOrNothing()
	{
		int randomInt = ThreadLocalRandom.current().nextInt(0, 1000); // Random int from 0 to 999.

		if (randomInt == 1)
		{
			return 1465;
		}

		if (randomInt < 20)
		{
			return 1757;
		}

		if (randomInt < 40)
		{
			return 5690;
		}

		if (randomInt < 60)
		{
			return 11451;
		}

		if (randomInt < 100 || config.continuousBusiness())
		{
			return 1124;
		}

		return -1;
	}

	public void clearAll()
	{
		Iterator i = businessObjects.iterator();

		while (i.hasNext())
		{
			((RuneLiteObject)i.next()).setActive(false);
		}

		businessObjects = new HashSet<>();
		businessObjectsWorldPointMap = new HashMap<>();
	}

	private boolean worldPointIsEmpty(WorldPoint worldPoint)
	{
		return getBusinessObjectFromMap(worldPoint) == null;
	}

	private RuneLiteObject getBusinessObjectFromMap(WorldPoint worldPoint)
	{
		int x = worldPoint.getX();
		int y = worldPoint.getY();
		int plane = worldPoint.getPlane();

		Map<Integer, Map<Integer, RuneLiteObject>> xMapping = businessObjectsWorldPointMap.get(x);

		if (xMapping == null)
		{
			return null;
		}

		Map<Integer, RuneLiteObject> yMapping = xMapping.get(y);

		if (yMapping == null)
		{
			return null;
		}

		return yMapping.get(plane);
	}

	private RuneLiteObject createBusinessObject(int modelId)
	{
		RuneLiteObject newBusinessObject = client.createRuneLiteObject();
		Model newBusinessModel = client.loadModel(modelId);

		if (newBusinessModel == null)
		{
			return null;
		}

		newBusinessObject.setModel(newBusinessModel);

		return newBusinessObject;
	}

	private void cacheBusinessObject(RuneLiteObject newBusinessObject)
	{
		addToBusinessObjects(newBusinessObject);
		addToBusinessObjectsWorldPointMap(newBusinessObject);
	}

	private void addToBusinessObjects(RuneLiteObject newBusinessObject)
	{
		businessObjects.add(newBusinessObject);
	}

	private void addToBusinessObjectsWorldPointMap(RuneLiteObject newBusinessObject)
	{
		WorldPoint newBusinessObjectWorldPoint = WorldPoint.fromLocal(client, newBusinessObject.getLocation());

		int x = newBusinessObjectWorldPoint.getX();
		int y = newBusinessObjectWorldPoint.getY();
		int plane = newBusinessObjectWorldPoint.getPlane();

		businessObjectsWorldPointMap.putIfAbsent(x, new HashMap<>());
		businessObjectsWorldPointMap.get(x).putIfAbsent(y, new HashMap<>());
		businessObjectsWorldPointMap.get(x).get(y).putIfAbsent(plane, newBusinessObject);
	}
}
