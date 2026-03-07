package zyx.araxia.shrouded.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import zyx.araxia.shrouded.item.ShroudedClassItems;
import zyx.araxia.shrouded.item.ShroudedItems;

/**
 * Handles the {@link ShroudedClassItems#TYPE_LEAP_WOODEN_SPEAR Leap Spear}
 * ability for the Shrouded class.
 * <h3>Mechanic</h3>
 * <ol>
 * <li>Left-click or right-click the spear while <em>not</em> charging → begin
 * charging. The player's XP bar fills over
 * {@code shrouded-class.leap-spear-charge-up-time-ticks} ticks.</li>
 * <li>Click again while charging → fire early at the current charge
 * percentage.</li>
 * <li>Auto-fires at full charge when max ticks elapse.</li>
 * <li>Switching away from the spear while charging cancels the charge without
 * firing and restores the player's XP bar.</li>
 * </ol>
 * <p>
 * All combat damage from the spear is suppressed — it is a mobility-only item.
 */
public class ShroudedLeapSpearListener implements Listener {

	private final JavaPlugin plugin;
	private static final Logger logger = Logger
			.getLogger(ShroudedLeapSpearListener.class.getName());

	/** How many charge ticks each player has accumulated. */
	private final Map<UUID, Integer> chargeTicks = new HashMap<>();

	/** The repeating task driving each player's charge build-up. */
	private final Map<UUID, BukkitTask> chargeTasks = new HashMap<>();

	/**
	 * Saved XP state per player: {@code [level (float), exp (float)]}. Stored
	 * on charge start and restored when the charge ends.
	 */
	private final Map<UUID, float[]> savedXp = new HashMap<>();

	/** Cooldown expiry per player (epoch milliseconds). */
	private final Map<UUID, Long> cooldownExpiry = new HashMap<>();

	public ShroudedLeapSpearListener(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	// -------------------------------------------------------------------------
	// Suppress all combat damage from the leap spear
	// -------------------------------------------------------------------------

	/**
	 * Cancels any entity-damage event where the attacker is a player holding
	 * the leap spear. The spear deals no damage — it is mobility-only.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Player player))
			return;
		if (!isLeapSpear(player.getInventory().getItemInMainHand()))
			return;
		event.setCancelled(true);
	}

	// -------------------------------------------------------------------------
	// Click handler — start charging or fire early
	// -------------------------------------------------------------------------

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Action action = event.getAction();
		boolean isLeftClick = (action == Action.LEFT_CLICK_AIR
				|| action == Action.LEFT_CLICK_BLOCK);
		boolean isRightClick = (action == Action.RIGHT_CLICK_AIR
				|| action == Action.RIGHT_CLICK_BLOCK);

		if (!isLeftClick && !isRightClick)
			return;

		// Only handle the main hand to avoid double-firing
		if (event.getHand() != EquipmentSlot.HAND)
			return;

		ItemStack item = event.getItem();
		if (!isLeapSpear(item))
			return;

		// Suppress attack / block interactions for non-ability purposes
		event.setCancelled(true);

		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();

		// ---- Cooldown check ----
		long now = System.currentTimeMillis();
		long expiry = cooldownExpiry.getOrDefault(uuid, 0L);
		if (now < expiry) {
			long remaining = (expiry - now + 999L) / 1000L;
			player.sendActionBar(
					Component.text("Leap on cooldown: " + remaining + "s",
							NamedTextColor.RED));
			return;
		}

		// ---- Already charging → early release ----
		if (chargeTicks.containsKey(uuid)) {
			int current = chargeTicks.getOrDefault(uuid, 0);
			int maxTicks = getMaxChargeTicks();
			float pct = Math.min(1.0f, (float) current / maxTicks);
			cancelCharge(uuid, player);
			launchPlayer(player, pct);
			return;
		}

		// ---- Begin new charge ----
		startCharge(player);
	}

	// -------------------------------------------------------------------------
	// Cancel charge when the player switches away from the spear
	// -------------------------------------------------------------------------

	@EventHandler
	public void onItemHeld(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		if (!chargeTasks.containsKey(uuid))
			return;

		// Cancel only if the newly selected slot does not hold the spear
		ItemStack incoming = player.getInventory().getItem(event.getNewSlot());
		if (!isLeapSpear(incoming)) {
			cancelCharge(uuid, player);
		}
	}

	// -------------------------------------------------------------------------
	// Internal helpers
	// -------------------------------------------------------------------------

	private boolean isLeapSpear(ItemStack item) {
		if (item == null || !ShroudedItems.isShroudedItem(item))
			return false;
		if (item.getItemMeta() == null)
			return false;
		String type = item.getItemMeta().getPersistentDataContainer()
				.get(ShroudedItems.ITEM_TYPE, PersistentDataType.STRING);
		return ShroudedClassItems.TYPE_LEAP_WOODEN_SPEAR.equals(type);
	}

	private int getMaxChargeTicks() {
		return Math.max(1, plugin.getConfig()
				.getInt("shrouded-class.leap-spear-charge-up-time-ticks", 20));
	}

	private void startCharge(Player player) {
		UUID uuid = player.getUniqueId();
		chargeTicks.put(uuid, 0);

		// Persist the player's current XP state so it can be restored later
		savedXp.put(uuid, new float[] {
				player.getLevel(), player.getExp()
		});

		logger.log(Level.FINE, "[LeapSpear] {0} started charging.",
				player.getName());

		BukkitTask task = new BukkitRunnable() {
			@Override
			public void run() {
				if (!player.isOnline()) {
					chargeTicks.remove(uuid);
					chargeTasks.remove(uuid);
					savedXp.remove(uuid);
					return;
				}

				// Cancel if the player switched off the spear between ticks
				if (!isLeapSpear(player.getInventory().getItemInMainHand())) {
					cancelCharge(uuid, player);
					return;
				}

				int maxTicks = getMaxChargeTicks();
				int current = chargeTicks.getOrDefault(uuid, 0) + 1;
				chargeTicks.put(uuid, current);
				float pct = Math.min(1.0f, (float) current / maxTicks);

				// Fill XP bar to reflect charge percentage
				player.setLevel(0);
				player.setExp(pct);

				if (current >= maxTicks) {
					// Auto-fire at full charge
					cancelCharge(uuid, player);
					launchPlayer(player, 1.0f);
				}
			}
		}.runTaskTimer(plugin, 1L, 1L);

		chargeTasks.put(uuid, task);
	}

	/**
	 * Stops the charge ticker, removes all tracking state, and restores the
	 * player's XP bar to the values it had before charging began.
	 */
	private void cancelCharge(UUID uuid, Player player) {
		BukkitTask task = chargeTasks.remove(uuid);
		if (task != null)
			task.cancel();
		chargeTicks.remove(uuid);

		float[] xpState = savedXp.remove(uuid);
		if (xpState != null && player != null && player.isOnline()) {
			player.setLevel((int) xpState[0]);
			player.setExp(xpState[1]);
		}
	}

	private void launchPlayer(Player player, float chargePct) {
		double maxVelocity = plugin.getConfig().getDouble(
				"shrouded-class.leap-spear-player-launch-velocity", 1.0);
		double cooldownSeconds = plugin.getConfig()
				.getDouble("shrouded-class.leap-spear-cooldown-seconds", 2.0);

		Vector direction = player.getLocation().getDirection().normalize();
		double speed = maxVelocity * chargePct;
		Vector velocity = direction.multiply(speed);
		player.setVelocity(velocity);

		// Play the configured launch sound
		String soundName = plugin.getConfig().getString(
				"shrouded-class.leap-spear-launch-sound-enum",
				"ITEM_TRIDENT_THROW");
		float volume = (float) plugin.getConfig().getDouble(
				"shrouded-class.leap-spear-launch-sound-volume", 1.0);
		float pitch = (float) plugin.getConfig()
				.getDouble("shrouded-class.leap-spear-launch-sound-pitch", 1.0);

		Sound launchSound = parseSound(soundName, Sound.ITEM_TRIDENT_THROW);
		player.playSound(player.getLocation(), launchSound, volume, pitch);

		// Apply item cooldown and internal cooldown
		int cooldownTicks = (int) (cooldownSeconds * 20);
		long cooldownMillis = (long) (cooldownSeconds * 1_000);
		cooldownExpiry.put(player.getUniqueId(),
				System.currentTimeMillis() + cooldownMillis);
		player.setCooldown(Material.WOODEN_SWORD, cooldownTicks);

		logger.log(Level.FINE,
				"[LeapSpear] {0} leaped — charge={1}%, speed={2}.",
				new Object[] {
						player.getName(), (int) (chargePct * 100), speed
				});
	}

	/**
	 * Looks up a {@link Sound} by its config string, trying both
	 * {@code minecraft:dot.separated} and the plain lowercase forms via
	 * {@link Registry#SOUNDS}. Returns {@code fallback} if no match is found.
	 *
	 * @param name     config value, e.g. {@code "ITEM_TRIDENT_THROW"}
	 * @param fallback safe default used when the name cannot be resolved
	 */
	private static Sound parseSound(String name, Sound fallback) {
		if (name == null)
			return fallback;

		// Attempt 1: convert ENUM_NAME → dot.separated (standard Minecraft key
		// format)
		String dotKey = name.toLowerCase().replace('_', '.');
		Sound s = Registry.SOUNDS.get(NamespacedKey.minecraft(dotKey));
		if (s != null)
			return s;

		// Attempt 2: try the raw lowercased string (e.g. already in dot
		// notation)
		s = Registry.SOUNDS.get(NamespacedKey.minecraft(name.toLowerCase()));
		if (s != null)
			return s;

		logger.log(Level.WARNING,
				"[LeapSpear] Unknown sound ''{0}'', falling back to default.",
				name);
		return fallback;
	}
}
