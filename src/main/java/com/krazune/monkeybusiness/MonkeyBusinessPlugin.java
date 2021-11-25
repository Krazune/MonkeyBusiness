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
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Monkey Business",
	description = "This plugin lets you do your monkey business.",
	tags = {
		"monkey",
		"business",
		"cursed",
		"banana",
		"poop"
	}
)
public class MonkeyBusinessPlugin extends Plugin
{
	private final int CURSED_BANANA_ID = 25500;

	@Inject
	private Client client;

	@Inject
	private BusinessManager businessManager;

	private Queue<WorldPoint> worldPointsQueue;
	private int worldPointsToBeProcessedNext = 0;

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
