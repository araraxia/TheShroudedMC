package zyx.araxia.shrouded.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
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
import zyx.araxia.shrouded.projectile.SurvivorBombProjectile;

/**
 * Handles right-click use of the {@link SurvivorClassItems#TYPE_SURVIVOR_BOMB
 * Impact Bomb}.
 * <p>
 * On right-click, one bomb is consumed from the player's stack and a
 * {@link SurvivorBombProjectile} is launched in the direction the player is
 * looking.
 */
public class SurvivorBombListener implements Listener {

	private final JavaPlugin plugin;

	/** Tracks when each player's bomb cooldown expires (epoch milliseconds). */
	private final Map<UUID, Long> cooldownExpiry = new HashMap<>();

	/**
	 * @param plugin           plugin instance used to schedule the projectile
	 *                         task
	 * @param explosionRadius  blast radius in blocks
	 * @param explosionDamage  flat damage dealt to every entity in the blast
	 *                         radius
	 * @param cooldownTicks    ticks between throws
	 * @param drag             fraction of velocity retained each tick
	 * @param gravity          downward acceleration per tick (blocks/tick²)
	 * @param maxSpeed         terminal velocity (blocks/tick)
	 * @param hitboxRadius     AABB half-extent for entity collision
	 * @param maxLifetimeTicks ticks before the bomb despawns without impact
	 */
	public SurvivorBombListener(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Action action = event.getAction();
		if (action != Action.RIGHT_CLICK_AIR
				&& action != Action.RIGHT_CLICK_BLOCK)
			return;

		// Only handle the main hand to avoid firing twice
		if (event.getHand() != EquipmentSlot.HAND)
			return;

		ItemStack item = event.getItem();
		if (item == null || !ShroudedItems.isShroudedItem(item))
			return;

		String type = item.getItemMeta().getPersistentDataContainer()
				.get(ShroudedItems.ITEM_TYPE, PersistentDataType.STRING);
		if (!SurvivorClassItems.TYPE_SURVIVOR_BOMB.equals(type))
			return;

		event.setCancelled(true);

		Player player = event.getPlayer();
		long now = System.currentTimeMillis();
		long expiry = cooldownExpiry.getOrDefault(player.getUniqueId(), 0L);

		if (now < expiry) {
			long remaining = (expiry - now + 999) / 1000;
			player.sendActionBar(Component.text(
					"Impact Bomb on cooldown: " + remaining + "s remaining",
					NamedTextColor.RED));
			return;
		}

		// Get physics values from config at call time so that a server reload picks up changes
		double explosionRadius = plugin.getConfig().getDouble("survivor.bomb-explosion-radius", 1.5);
		double explosionDamage = plugin.getConfig().getDouble("survivor.bomb-explosion-damage", 6.0);	
		double drag = plugin.getConfig().getDouble("survivor.bomb-drag", 0.99);
		double gravity = plugin.getConfig().getDouble("survivor.bomb-gravity",
				0.06);
		double maxSpeed = plugin.getConfig().getDouble("survivor.bomb-max-speed", 2.0);
		double hitboxRadius = plugin.getConfig()
				.getDouble("survivor.bomb-hitbox-radius", 0.125);
		int maxLifetimeTicks = plugin.getConfig()
				.getInt("survivor.bomb-max-lifetime-ticks", 60);
		// Launch the physics projectile (runs every tick)
		new SurvivorBombProjectile(player, explosionRadius, explosionDamage,
				drag, gravity, maxSpeed, hitboxRadius, maxLifetimeTicks)
						.runTaskTimer(plugin, 0L, 1L);
		
		double cooldownSeconds = plugin.getConfig().getDouble("survivor.bomb-cooldown-seconds", 60.0);
		int cooldownTicks = (int) (cooldownSeconds * 20);
		long cooldownMillis = (long) (cooldownSeconds * 1000);
		// Consume one bomb from the stack, put on cooldown if it was the last one
		if (item.getAmount() > 1) {
			item.setAmount(item.getAmount() - 1);
		} else {
			cooldownExpiry.put(player.getUniqueId(), now + cooldownMillis);
			player.setCooldown(Material.PITCHER_POD, cooldownTicks);
		}

	}
}
