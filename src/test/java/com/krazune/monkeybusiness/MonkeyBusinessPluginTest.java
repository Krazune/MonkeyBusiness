package com.krazune.monkeybusiness;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class MonkeyBusinessPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MonkeyBusinessPlugin.class);
		RuneLite.main(args);
	}
}
