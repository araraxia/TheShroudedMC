package zyx.araxia.shrouded.listener;

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
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import zyx.araxia.shrouded.item.ShroudedClassItems;
import zyx.araxia.shrouded.item.ShroudedItems;

/**
 * Handles the {@link ShroudedClassItems#TYPE_LEAP_WOODEN_SPEAR Leap} bow
 * ability for the Shrouded class.
 *
 * <h3>Mechanic</h3>
 * <p>The item is a {@link org.bukkit.Material#BOW BOW} with
 * {@link org.bukkit.enchantments.Enchantment#INFINITY INFINITY}, so Minecraft
 * natively handles the charge animation, partial-release, and draw sound.
 * When the player releases the bow, {@link EntityShootBowEvent} fires;
 * the event is cancelled (suppressing the arrow) and
 * {@link #launchPlayer(Player, float)} is called with {@code event.getForce()}
 * as the charge percentage (0.0–1.0).</p>
 *
 * <p>Item cooldown is applied via {@link Player#setCooldown} on
 * {@link Material#BOW}, which also shows the vanilla cooldown overlay and
 * prevents drawing during the cooldown window.</p>
 */
public class ShroudedLeapSpearListener implements Listener {

	private final JavaPlugin plugin;
	private static final Logger logger = Logger
			.getLogger(ShroudedLeapSpearListener.class.getName());

	public ShroudedLeapSpearListener(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	// -------------------------------------------------------------------------
	// Shoot-bow handler — cancel arrow, launch player
	// -------------------------------------------------------------------------

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onShootBow(EntityShootBowEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return;
		if (!isLeapBow(event.getBow()))
			return;
		event.setCancelled(true); // suppress arrow projectile
		launchPlayer(player, event.getForce());
	}

	// -------------------------------------------------------------------------
	// Internal helpers
	// -------------------------------------------------------------------------

	private boolean isLeapBow(ItemStack item) {
		if (item == null || !ShroudedItems.isShroudedItem(item))
			return false;
		if (item.getItemMeta() == null)
			return false;
		String type = item.getItemMeta().getPersistentDataContainer()
				.get(ShroudedItems.ITEM_TYPE, PersistentDataType.STRING);
		return ShroudedClassItems.TYPE_LEAP_WOODEN_SPEAR.equals(type);
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

		// Apply item cooldown — also shows the vanilla cooldown overlay on the bow
		int cooldownTicks = (int) (cooldownSeconds * 20);
		player.setCooldown(Material.BOW, cooldownTicks);

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
