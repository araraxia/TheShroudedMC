package zyx.araxia.shrouded.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import zyx.araxia.shrouded.item.ShroudedItems;
import zyx.araxia.shrouded.item.SurvivorClassItems;

/**
 * Handles right-click use of the {@link SurvivorClassItems#TYPE_SURVIVOR_WEB
 * Survivor Web}.
 * <p>
 * On right-click against a block face, a cobweb is placed on the adjacent air
 * block. The last item in the stack is never consumed — it is instead put on a
 * cooldown from config ({@code survivor.web-cooldown-seconds}).
 */
public class SurvivorWebListener implements Listener {

	private final JavaPlugin plugin;

	/** Tracks when each player's web cooldown expires (epoch milliseconds). */
	private final Map<UUID, Long> cooldownExpiry = new HashMap<>();

	public SurvivorWebListener(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		// Web can only be placed against an actual block face
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		// Only handle the main hand to avoid firing twice
		if (event.getHand() != EquipmentSlot.HAND)
			return;

		ItemStack item = event.getItem();
		if (item == null || !ShroudedItems.isShroudedItem(item))
			return;

		String type = item.getItemMeta().getPersistentDataContainer()
				.get(ShroudedItems.ITEM_TYPE, PersistentDataType.STRING);
		if (!SurvivorClassItems.TYPE_SURVIVOR_WEB.equals(type))
			return;

		event.setCancelled(true);

		Player player = event.getPlayer();
		long now = System.currentTimeMillis();
		long expiry = cooldownExpiry.getOrDefault(player.getUniqueId(), 0L);

		if (now < expiry) {
			long remaining = (expiry - now + 999) / 1000;
			player.sendActionBar(Component.text(
					"Survivor Web on cooldown: " + remaining + "s remaining",
					NamedTextColor.RED));
			return;
		}

		// Determine the block adjacent to the clicked face
		Block clickedBlock = event.getClickedBlock();
		if (clickedBlock == null)
			return;
		Block targetBlock = clickedBlock.getRelative(event.getBlockFace());

		if (!targetBlock.isPassable()
				&& targetBlock.getType() != Material.AIR) {
			player.sendActionBar(Component.text("No space to place web there!",
					NamedTextColor.RED));
			return;
		}

		targetBlock.setType(Material.COBWEB);

		// Read cooldown from config at call time so reloads take effect
		double cooldownSeconds = plugin.getConfig()
				.getDouble("survivor.web-cooldown-seconds", 30.0);
		int cooldownTicks = (int) (cooldownSeconds * 20);
		long cooldownMillis = (long) (cooldownSeconds * 1000);

		if (item.getAmount() > 1) {
			item.setAmount(item.getAmount() - 1);
		} else {
			cooldownExpiry.put(player.getUniqueId(), now + cooldownMillis);
			player.setCooldown(Material.COBWEB, cooldownTicks);
		}
	}
}
