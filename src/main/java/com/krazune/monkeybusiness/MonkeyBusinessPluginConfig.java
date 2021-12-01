package com.krazune.monkeybusiness;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("monkeybusiness")
public interface MonkeyBusinessPluginConfig extends Config
{
	@ConfigItem(
		position = 0,
		keyName = "continuousBusiness",
		name = "Continuous",
		description = "Business frequency."
	)
	default boolean continuousBusiness()
	{
		return true;
	}
}
