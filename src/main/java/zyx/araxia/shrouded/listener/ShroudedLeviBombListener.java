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
import zyx.araxia.shrouded.item.ShroudedClassItems;
import zyx.araxia.shrouded.item.ShroudedItems;
import zyx.araxia.shrouded.projectile.LeviBombProjectile;

/**
 * Handles right-click use of the
 * {@link ShroudedClassItems#TYPE_LEVI_BOMB_CHORUS_FLOWER Levitation Bomb}.
 * <p>
 * On right-click the bomb is thrown as a {@link LeviBombProjectile}. When it
 * detonates, every {@link org.bukkit.entity.LivingEntity} within the explosion
 * radius (excluding the thrower) is inflicted with
 * {@link org.bukkit.potion.PotionEffectType#LEVITATION} for the configured
 * duration.
 */
public class ShroudedLeviBombListener implements Listener {

	private final JavaPlugin plugin;

	/**
	 * Tracks when each player's levi bomb cooldown expires (epoch
	 * milliseconds).
	 */
	private final Map<UUID, Long> cooldownExpiry = new HashMap<>();

	public ShroudedLeviBombListener(JavaPlugin plugin) {
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
		if (!ShroudedClassItems.TYPE_LEVI_BOMB_CHORUS_FLOWER.equals(type))
			return;

		event.setCancelled(true);

		Player player = event.getPlayer();
		long now = System.currentTimeMillis();
		long expiry = cooldownExpiry.getOrDefault(player.getUniqueId(), 0L);

		if (now < expiry) {
			long remaining = (expiry - now + 999) / 1000;
			player.sendActionBar(Component.text(
					"Levitation Bomb on cooldown: " + remaining + "s remaining",
					NamedTextColor.RED));
			return;
		}

		// Read physics/effect values from config at call time so a reload picks
		// up changes.
		// Note: drag and gravity keys use the "labi-bomb" prefix as defined in
		// config.yml.
		double explosionRadius = plugin.getConfig()
				.getDouble("shrouded-class.levi-bomb-explosion-radius", 2.0);
		double levitationDurationSecs = plugin.getConfig().getDouble(
				"shrouded-class.levi-bomb-levitation-duration-seconds", 3.0);
		int levitationDurationTicks = (int) (levitationDurationSecs * 20);
		double drag = plugin.getConfig()
				.getDouble("shrouded-class.labi-bomb-drag", 0.99);
		double gravity = plugin.getConfig()
				.getDouble("shrouded-class.labi-bomb-gravity", 0.06);
		double maxSpeed = plugin.getConfig()
				.getDouble("shrouded-class.levi-bomb-max-speed", 2.0);
		double hitboxRadius = plugin.getConfig()
				.getDouble("shrouded-class.levi-bomb-hitbox-radius", 0.125);
		int maxLifetimeTicks = plugin.getConfig()
				.getInt("shrouded-class.levi-bomb-max-lifetime-ticks", 60);
		double throwVelocity = plugin.getConfig()
				.getDouble("shrouded-class.levi-bomb-throw-velocity", 1.0);

		new LeviBombProjectile(player, explosionRadius, levitationDurationTicks,
				drag, gravity, maxSpeed, hitboxRadius, maxLifetimeTicks,
				throwVelocity).runTaskTimer(plugin, 0L, 1L);

		double cooldownSeconds = plugin.getConfig()
				.getDouble("shrouded-class.levi-bomb-cooldown-seconds", 120.0);
		int cooldownTicks = (int) (cooldownSeconds * 20);
		long cooldownMillis = (long) (cooldownSeconds * 1000);

		if (item.getAmount() > 1) {
			item.setAmount(item.getAmount() - 1);
		} else {
			cooldownExpiry.put(player.getUniqueId(), now + cooldownMillis);
			player.setCooldown(Material.CHORUS_FLOWER, cooldownTicks);
		}
	}
}
