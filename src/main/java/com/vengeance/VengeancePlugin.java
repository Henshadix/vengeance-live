package com.vengeance;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;
import net.runelite.http.api.item.ItemPrice;

@Slf4j
@PluginDescriptor(
	name = "Vengeance Live",
	description = "Stuurt collection log, hoge loot-drops en PK/loot key naar je Vengeance-site.",
	tags = {"pvp", "drops", "collection", "website", "vengeance"}
)
public class VengeancePlugin extends Plugin
{
	private static final Pattern[] COLLECTION_PATTERNS = {
		Pattern.compile("(?i)New item added to your collection log:\\s*(.+)"),
		Pattern.compile("(?i)You have added a new item to your collection log:\\s*(.+)"),
		Pattern.compile("(?i)Congratulations, you've completed a new collection log entry:\\s*(.+)"),
	};

	private static final int[] PVP_LOOT_KEY_INVENTORIES = {
		InventoryID.DEADMAN_LOOT_INV0,
		InventoryID.DEADMAN_LOOT_INV1,
		InventoryID.DEADMAN_LOOT_INV2,
		InventoryID.DEADMAN_LOOT_INV3,
		InventoryID.DEADMAN_LOOT_INV4,
	};

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private VengeanceConfig config;

	@Inject
	private VengeanceApiClient apiClient;

	private String lastChestSignature = "";
	private long lastChestAtMs;

	@Provides
	@Singleton
	VengeanceConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(VengeanceConfig.class);
	}

	private String localPlayerName()
	{
		if (client.getLocalPlayer() == null)
		{
			return "Unknown";
		}
		return client.getLocalPlayer().getName();
	}

	private long sumGe(Collection<ItemStack> items)
	{
		long t = 0L;
		for (ItemStack s : items)
		{
			if (s.getId() <= 0)
			{
				continue;
			}
			t += (long) itemManager.getItemPrice(s.getId()) * s.getQuantity();
		}
		return t;
	}

	private ItemStack bestStack(Collection<ItemStack> items)
	{
		ItemStack best = null;
		long bestVal = -1L;
		for (ItemStack s : items)
		{
			if (s.getId() <= 0)
			{
				continue;
			}
			long v = (long) itemManager.getItemPrice(s.getId()) * s.getQuantity();
			if (v > bestVal)
			{
				bestVal = v;
				best = s;
			}
		}
		return best;
	}

	private String stackSignature(Collection<ItemStack> items)
	{
		return items.stream()
			.filter(s -> s.getId() > 0)
			.sorted((a, b) -> {
				int c = Integer.compare(a.getId(), b.getId());
				return c != 0 ? c : Integer.compare(a.getQuantity(), b.getQuantity());
			})
			.map(s -> s.getId() + "x" + s.getQuantity())
			.collect(Collectors.joining(","));
	}

	private void postValuableLoot(String source, Collection<ItemStack> items, String itemNameOverride, long minGp)
	{
		if (!config.enabled() || items == null || items.isEmpty())
		{
			return;
		}
		long total = sumGe(items);
		if (total < minGp)
		{
			return;
		}
		ItemStack best = bestStack(items);
		if (best == null)
		{
			return;
		}
		String itemName = itemNameOverride;
		if (itemName == null || itemName.isEmpty())
		{
			itemName = itemManager.getItemComposition(best.getId()).getName();
		}
		apiClient.postDropAsync(localPlayerName(), best.getId(), itemName, best.getQuantity(), total, source);
	}

	private void postCollectionLog(String rawItemName)
	{
		if (!config.enabled())
		{
			return;
		}
		String cleaned = Text.removeTags(rawItemName).trim();
		if (cleaned.isEmpty())
		{
			return;
		}
		int itemId = resolveItemIdByName(cleaned);
		if (itemId <= 0)
		{
			log.debug("Vengeance: geen itemId voor collection log: {}", cleaned);
			return;
		}
		apiClient.postDropAsync(localPlayerName(), itemId, cleaned, 1, null, "collection");
	}

	@SuppressWarnings("unchecked")
	private int resolveItemIdByName(String name)
	{
		List<ItemPrice> found = itemManager.search(name);
		if (found == null || found.isEmpty())
		{
			return -1;
		}
		for (ItemPrice p : found)
		{
			if (p.getName().equalsIgnoreCase(name))
			{
				return p.getId();
			}
		}
		for (ItemPrice p : found)
		{
			if (p.getName().toLowerCase().startsWith(name.toLowerCase()))
			{
				return p.getId();
			}
		}
		return found.get(0).getId();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}
		String msg = event.getMessage();
		for (Pattern p : COLLECTION_PATTERNS)
		{
			Matcher m = p.matcher(msg);
			if (m.find())
			{
				postCollectionLog(m.group(1));
				return;
			}
		}
	}

	@Subscribe
	public void onServerNpcLoot(ServerNpcLoot event)
	{
		Collection<ItemStack> items = event.getItems();
		if (items == null || items.isEmpty())
		{
			return;
		}
		postValuableLoot("loot", items, null, config.minLootGp());
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event)
	{
		Collection<ItemStack> items = event.getItems();
		if (items == null || items.isEmpty())
		{
			return;
		}
		postValuableLoot("loot", items, null, config.minLootGp());
	}

	@Subscribe
	public void onPlayerLootReceived(PlayerLootReceived event)
	{
		Collection<ItemStack> items = event.getItems();
		if (items == null || items.isEmpty())
		{
			return;
		}
		String victim = Text.removeTags(event.getPlayer().getName());
		String label = "PK: " + victim;
		postValuableLoot("pk", items, label, config.minLootGp());
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() != InterfaceID.WILDY_LOOT_CHEST)
		{
			return;
		}
		List<ItemStack> stacks = new ArrayList<>();
		for (int containerId : PVP_LOOT_KEY_INVENTORIES)
		{
			ItemContainer container = client.getItemContainer(containerId);
			if (container == null)
			{
				continue;
			}
			for (net.runelite.api.Item item : container.getItems())
			{
				if (item != null && item.getId() > 0)
				{
					stacks.add(new ItemStack(item.getId(), item.getQuantity()));
				}
			}
		}
		if (stacks.isEmpty())
		{
			return;
		}
		String sig = stackSignature(stacks);
		long now = System.currentTimeMillis();
		if (sig.equals(lastChestSignature) && (now - lastChestAtMs) < 4000L)
		{
			return;
		}
		lastChestSignature = sig;
		lastChestAtMs = now;
		postValuableLoot("pk", stacks, "Wilderness loot key", config.minLootGp());
	}
}
