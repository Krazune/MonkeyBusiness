package com.krazune.monkeybusiness;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

public class BusinessManager
{
	private Client client;

	private EventBus eventBus;

	private MonkeyBusinessPluginConfig config;

	private Map<Integer, Map<Integer, Map<Integer, RuneLiteObject>>> businessObjectsWorldPointMap; // X, Y, and Plane.

	@Inject
	public BusinessManager(Client client, EventBus eventBus, MonkeyBusinessPluginConfig config)
	{
		this.client = client;
		this.eventBus = eventBus;
		this.config = config;

		this.eventBus.register(this);

		businessObjectsWorldPointMap = new HashMap<>();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		for (Integer x : businessObjectsWorldPointMap.keySet())
		{
			Map<Integer, Map<Integer, RuneLiteObject>> yMap = businessObjectsWorldPointMap.get(x);

			for (Integer y : yMap.keySet())
			{
				Map<Integer, RuneLiteObject> planeMap = yMap.get(y);

				for (Integer plane : planeMap.keySet())
				{
					createRandomBusinessObject(new WorldPoint(x, y, plane));
				}
			}
		}
	}

	public void doBusiness(WorldPoint worldPoint)
	{
		if (!worldPointIsEmpty(worldPoint))
		{
			return;
		}

		RuneLiteObject newBusinessObject = createRandomBusinessObject(worldPoint);

		if (newBusinessObject == null)
		{
			return;
		}

		cacheBusinessObject(newBusinessObject);
	}

	private int getRandomModelIdOrNothing(WorldPoint worldPoint)
	{
		Random random = new Random(getRandomSeed(worldPoint));

		int randomInt = random.nextInt(1000); // Random int from 0 to 999.

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

	private int getRandomSeed(WorldPoint worldPoint)
	{
		String worldPointString = "x" + worldPoint.getX() + "y" + worldPoint.getY() + "p" + worldPoint.getPlane();

		return worldPointString.hashCode(); // This might cause predictable patterns.
	}

	public void clearAll()
	{
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

	// This function is not very well thought out, but it will do for now.
	private RuneLiteObject createRandomBusinessObject(WorldPoint worldPoint)
	{
		int modelId = getRandomModelIdOrNothing(worldPoint);

		if (modelId == -1)
		{
			return null;
		}

		RuneLiteObject newBusinessObject = client.createRuneLiteObject();
		Model newBusinessModel = client.loadModel(modelId);

		if (newBusinessModel == null)
		{
			return null;
		}

		newBusinessObject.setModel(newBusinessModel);

		if (newBusinessObject == null)
		{
			return null;
		}

		LocalPoint newBusinessLocalPoint = LocalPoint.fromWorld(client, worldPoint);

		if (newBusinessLocalPoint == null)
		{
			return null;
		}

		newBusinessObject.setLocation(newBusinessLocalPoint, worldPoint.getPlane());
		newBusinessObject.setActive(true);

		return newBusinessObject;
	}

	private void cacheBusinessObject(RuneLiteObject newBusinessObject)
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
