package com.krazune.monkeybusiness;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

public class BusinessManager
{
	private final Client client;

	private final EventBus eventBus;

	private final MonkeyBusinessPluginConfig config;

	private Map<Integer, Map<Integer, Map<Integer, Business>>> businessLocations; // X, Y, and Plane.

	private int planeId;

	@Inject
	public BusinessManager(Client client, EventBus eventBus, MonkeyBusinessPluginConfig config)
	{
		this.client = client;
		this.eventBus = eventBus;
		this.config = config;
		this.planeId = client.getPlane();

		this.eventBus.register(this);

		businessLocations = new HashMap<>();
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
		// The objects disappear on game state LOGGED_IN change.
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

		Business newBusiness = createRandomBusiness(worldPoint);

		if (newBusiness == null)
		{
			return;
		}

		newBusiness.spawn();

		cacheBusiness(newBusiness);
	}

	public void clearAll()
	{
		for (Integer x : businessLocations.keySet())
		{
			Map<Integer, Map<Integer, Business>> yMap = businessLocations.get(x);

			for (Integer y : yMap.keySet())
			{
				Map<Integer, Business> planeMap = yMap.get(y);

				for (Integer plane : planeMap.keySet())
				{
					Business currentBusiness = planeMap.get(plane);

					if (currentBusiness == null)
					{
						continue;
					}

					currentBusiness.despawn();
				}
			}
		}

		businessLocations = new HashMap<>();
	}

	private void recreateObjects()
	{
		for (Integer x : businessLocations.keySet())
		{
			Map<Integer, Map<Integer, Business>> yMap = businessLocations.get(x);

			for (Integer y : yMap.keySet())
			{
				Map<Integer, Business> planeMap = yMap.get(y);

				for (Integer plane : planeMap.keySet())
				{
					planeMap.get(plane).spawn();
				}
			}
		}
	}

	private boolean worldPointIsEmpty(WorldPoint worldPoint)
	{
		return getBusinessFromBusinessLocations(worldPoint) == null;
	}

	private Business getBusinessFromBusinessLocations(WorldPoint worldPoint)
	{
		int x = worldPoint.getX();
		int y = worldPoint.getY();
		int plane = worldPoint.getPlane();

		Map<Integer, Map<Integer, Business>> xMapping = businessLocations.get(x);

		if (xMapping == null)
		{
			return null;
		}

		Map<Integer, Business> yMapping = xMapping.get(y);

		if (yMapping == null)
		{
			return null;
		}

		return yMapping.get(plane);
	}

	// This function is not very well thought out, but it will do for now.
	private Business createRandomBusiness(WorldPoint worldPoint)
	{
		BusinessType newBusinessType = getRandomBusinessTypeOrNull(worldPoint);

		if (newBusinessType == null)
		{
			if (!config.continuousBusiness())
			{
				return null;
			}

			newBusinessType = BusinessType.FLOOR_MARKS;
		}

		return new Business(client, worldPoint, newBusinessType);
	}

	private BusinessType getRandomBusinessTypeOrNull(WorldPoint worldPoint)
	{
		Random random = new Random(getRandomSeed(worldPoint));
		int continueRandom = random.nextInt(3);

		// One turd chance of returning no business:
		// 	0 -> no model
		// 	1 -> random model
		// 	2 -> random model
		if (continueRandom != 0)
		{
			return null;
		}

		int businessTypeRandom = random.nextInt(100);

		// 1% chance of getting chosen.
		if (businessTypeRandom == 0)
		{
			return BusinessType.GIGANTIC_PILE;
		}

		// 15% chance of getting chosen.
		if (businessTypeRandom <= 15)
		{
			return BusinessType.SMALL_PILE;
		}

		// 15% chance of getting chosen.
		if (businessTypeRandom <= 30)
		{
			return BusinessType.MEDIUM_PEBBLES;
		}

		// 30% chance of getting chosen.
		if (businessTypeRandom <= 60)
		{
			return BusinessType.SMALL_PEBBLES;
		}

		// 39% chance of getting chosen.
		return BusinessType.FLOOR_MARKS;
	}

	private int getRandomSeed(WorldPoint worldPoint)
	{
		String worldPointString = "x" + worldPoint.getX() + "y" + worldPoint.getY() + "p" + worldPoint.getPlane();

		return worldPointString.hashCode(); // This might cause predictable patterns.
	}

	private void cacheBusiness(Business newBusiness)
	{
		WorldPoint newBusinessLocation = newBusiness.getLocation();

		int x = newBusinessLocation.getX();
		int y = newBusinessLocation.getY();
		int plane = newBusinessLocation.getPlane();

		businessLocations.putIfAbsent(x, new HashMap<>());
		businessLocations.get(x).putIfAbsent(y, new HashMap<>());
		businessLocations.get(x).get(y).putIfAbsent(plane, newBusiness);
	}
}
