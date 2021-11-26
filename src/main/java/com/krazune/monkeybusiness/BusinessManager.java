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
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

public class BusinessManager
{
	private final int GIGANTIC_PILE = 1465;
	private final int SMALL_PILE = 1757;
	private final int MEDIUM_PEBBLES = 5690;
	private final int SMALL_PEBBLES = 11451;
	private final int FLOOR_MARKS = 1124;

	private final Client client;

	private final EventBus eventBus;

	private final MonkeyBusinessPluginConfig config;

	private Map<Integer, Map<Integer, Map<Integer, RuneLiteObject>>> businessObjectsWorldPointMap; // X, Y, and Plane.

	private int planeId;

	@Inject
	public BusinessManager(Client client, EventBus eventBus, MonkeyBusinessPluginConfig config)
	{
		this.client = client;
		this.eventBus = eventBus;
		this.config = config;
		this.planeId = client.getPlane();

		this.eventBus.register(this);

		businessObjectsWorldPointMap = new HashMap<>();
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (planeId == client.getPlane())
		{
			return;
		}

		recreateObjects();

		planeId = client.getPlane();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		recreateObjects();
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

	public void clearAll()
	{
		for (Integer x : businessObjectsWorldPointMap.keySet())
		{
			Map<Integer, Map<Integer, RuneLiteObject>> yMap = businessObjectsWorldPointMap.get(x);

			for (Integer y : yMap.keySet())
			{
				Map<Integer, RuneLiteObject> planeMap = yMap.get(y);

				for (Integer plane : planeMap.keySet())
				{
					RuneLiteObject currentObject = planeMap.get(plane);

					if (currentObject == null)
					{
						continue;
					}

					currentObject.setActive(false);
				}
			}
		}

		businessObjectsWorldPointMap = new HashMap<>();
	}

	private void recreateObjects()
	{
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

	private int getRandomModelIdOrNothing(WorldPoint worldPoint)
	{
		Random random = new Random(getRandomSeed(worldPoint));
		int continueRandom = random.nextInt(3);

		// One turd chance of returning no model:
		// 	0 -> no model
		// 	1 -> random model
		// 	2 -> random model
		if (continueRandom != 0)
		{
			return -1;
		}

		int modelRandom = random.nextInt(100);

		// 1% chance of getting chosen.
		if (modelRandom == 0)
		{
			return GIGANTIC_PILE;
		}

		// 15% chance of getting chosen.
		if (modelRandom <= 15)
		{
			return SMALL_PILE;
		}

		// 15% chance of getting chosen.
		if (modelRandom <= 30)
		{
			return MEDIUM_PEBBLES;
		}

		// 30% chance of getting chosen.
		if (modelRandom <= 60)
		{
			return SMALL_PEBBLES;
		}

		// 39% chance of getting chosen.
		return FLOOR_MARKS;
	}

	private int getRandomSeed(WorldPoint worldPoint)
	{
		String worldPointString = "x" + worldPoint.getX() + "y" + worldPoint.getY() + "p" + worldPoint.getPlane();

		return worldPointString.hashCode(); // This might cause predictable patterns.
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
			if (!config.continuousBusiness())
			{
				return null;
			}

			modelId = FLOOR_MARKS;
		}

		RuneLiteObject newBusinessObject = client.createRuneLiteObject();
		Model newBusinessModel = client.loadModel(modelId);

		if (newBusinessModel == null)
		{
			return null;
		}

		newBusinessObject.setModel(newBusinessModel);

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
