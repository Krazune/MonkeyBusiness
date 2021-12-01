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

import com.google.inject.Provides;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Monkey Business",
	description = "This plugin lets you do your monkey business, when wielding a cursed banana.",
	tags = {
		"monkey",
		"business",
		"cursed",
		"banana",
		"poop"
		// Smithing
	}
)
public class MonkeyBusinessPlugin extends Plugin
{
	private final int CURSED_BANANA_ID = 25500;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private BusinessManager businessManager;

	private Queue<WorldPoint> worldPointsQueue;
	private int worldPointsToBeProcessedNext = 0;

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		List<Player> players = client.getPlayers();

		processWorldPointsQueue();

		for (int i = 0; i < players.size(); ++i)
		{
			Player currentPlayer = players.get(i);

			if (!playerHasCursedBanana(currentPlayer))
			{
				continue;
			}

			worldPointsQueue.add(currentPlayer.getWorldLocation());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		GameState newState = gameStateChanged.getGameState();

		if (newState != GameState.LOGIN_SCREEN && newState != GameState.HOPPING)
		{
			return;
		}

		businessManager.clearAll();
		worldPointsQueue.clear();
	}

	@Override
	protected void startUp()
	{
		worldPointsQueue = new LinkedList<>();
	}

	@Override
	protected void shutDown()
	{
		worldPointsQueue = null;

		businessManager.clearAll();
	}

	@Provides
	MonkeyBusinessPluginConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MonkeyBusinessPluginConfig.class);
	}

	private boolean playerHasCursedBanana(Player player)
	{
		if (player == null)
		{
			return false;
		}

		PlayerComposition playerComposition = player.getPlayerComposition();

		if (playerComposition == null)
		{
			return false;
		}

		return playerComposition.getEquipmentId(KitType.WEAPON) == CURSED_BANANA_ID;
	}

	private void processWorldPointsQueue()
	{
		if (worldPointsQueue.isEmpty())
		{
			return;
		}

		for (int i = 0; i < worldPointsToBeProcessedNext; ++i)
		{
			businessManager.doBusiness(worldPointsQueue.remove());
		}

		worldPointsToBeProcessedNext = worldPointsQueue.size();
	}
}
