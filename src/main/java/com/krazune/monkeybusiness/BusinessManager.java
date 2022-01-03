/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2021, Miguel Sousa
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.krazune.monkeybusiness;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

public class BusinessManager
{
	private final Client client;

	private final ClientThread clientThread;

	private final EventBus eventBus;

	private final MonkeyBusinessPluginConfig config;

	private final Duration BUSINESS_DURATION = Duration.ofMinutes(1);

	private Map<WorldPoint, Business> businessLocations;
	private Map<Business, Instant> businessSpawnInstants;

	private int planeId;

	@Inject
	public BusinessManager(Client client, ClientThread clientThread, EventBus eventBus, MonkeyBusinessPluginConfig config)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.config = config;
		this.planeId = client.getPlane();

		this.eventBus.register(this);

		businessLocations = new HashMap<>();
		businessSpawnInstants = new HashMap<>();
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		removeOldBusiness();

		if (planeId == client.getPlane())
		{
			return;
		}

		spawnAll();

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

		spawnAll();
	}

	public void doBusiness(WorldPoint worldPoint)
	{
		Business existentBusiness = businessLocations.get(worldPoint);

		if (existentBusiness != null)
		{
			addBusinessInstant(existentBusiness);

			return;
		}

		Business newBusiness = createRandomBusiness(worldPoint);

		if (newBusiness == null)
		{
			return;
		}

		newBusiness.setActive(true);

		businessLocations.put(newBusiness.getLocation(), newBusiness);
		addBusinessInstant(newBusiness);
	}

	public void spawnAll()
	{
		for (Business business : businessLocations.values())
		{
			business.setActive(true);
		}
	}

	public void despawnAll()
	{
		for (Business business : businessLocations.values())
		{
			business.setActive(false);
		}
	}

	public void clearAll()
	{
		despawnAll();

		businessLocations = new HashMap<>();
		businessSpawnInstants = new HashMap<>();
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

		return new Business(client, clientThread, worldPoint, newBusinessType);
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
		String worldPointString = "w" + client.getWorld() + "x" + worldPoint.getX() + "y" + worldPoint.getY() + "p" + worldPoint.getPlane();

		return worldPointString.hashCode(); // This might cause predictable patterns.
	}

	private void addBusinessInstant(Business business)
	{
		businessSpawnInstants.put(business, Instant.now());
	}

	private void removeOldBusiness()
	{
		Iterator<Business> i = businessSpawnInstants.keySet().iterator();

		while (i.hasNext())
		{
			Business currentBusiness = i.next();

			if (isOldBusiness(businessSpawnInstants.get(currentBusiness)))
			{
				i.remove(); // This should be moved to the business removal function.

				removeBusiness(currentBusiness);
			}
		}
	}

	private void removeBusiness(Business business)
	{
		WorldPoint businessLocation = business.getLocation();
		Business cachedBusiness = businessLocations.get(businessLocation);

		if (cachedBusiness == null)
		{
			return;
		}

		if (business != cachedBusiness)
		{
			return;
		}

		business.setActive(false);

		businessLocations.remove(business);
	}

	private boolean isOldBusiness(Instant spawnInstant)
	{
		return Duration.between(spawnInstant, Instant.now()).compareTo(BUSINESS_DURATION) >= 0;
	}
}
