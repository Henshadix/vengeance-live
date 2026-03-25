package com.vengeance;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("vengeance")
public interface VengeanceConfig extends Config
{
	@ConfigItem(
		keyName = "enabled",
		name = "Ingeschakeld",
		description = "Stuur events naar de Vengeance-website wanneer aan."
	)
	default boolean enabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "apiBaseUrl",
		name = "API basis-URL",
		description = "Basis-URL van je site (zonder slash aan het eind), bv. https://jouwdomein.nl"
	)
	default String apiBaseUrl()
	{
		return "";
	}

	@ConfigItem(
		keyName = "minLootGp",
		name = "Min. loot (GP)",
		description = "Minimale totale waarde (Grand Exchange) voor monster-drops, PK-ground loot en loot key-inhoud. Collection log stuurt altijd."
	)
	default int minLootGp()
	{
		return 1_000_000;
	}
}
