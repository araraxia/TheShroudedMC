package zyx.araxia.shrouded.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import zyx.araxia.shrouded.game.PlayerClass;
import zyx.araxia.shrouded.item.ShroudedClassItems;
import zyx.araxia.shrouded.item.ShroudedItems;
import zyx.araxia.shrouded.lobby.LobbyManager;
import zyx.araxia.shrouded.lobby.LobbySession;

public class ShroudedSwordStabListener implements Listener {

	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private static final Logger logger = Logger
			.getLogger(ShroudedSwordStabListener.class.getName());
	private final Map<UUID, Long> cooldownExpiry = new HashMap<>();

	public ShroudedSwordStabListener(JavaPlugin plugin,
			LobbyManager lobbyManager) {
		this.plugin = plugin;
		this.lobbyManager = lobbyManager;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Action action = event.getAction();
		if (action != Action.RIGHT_CLICK_AIR
				&& action != Action.RIGHT_CLICK_BLOCK)
			return;
		if (event.getHand() != EquipmentSlot.HAND)
			return;

		ItemStack item = event.getItem();
		if (item == null || !ShroudedItems.isShroudedItem(item))
			return;

		String type = item.getItemMeta().getPersistentDataContainer()
				.get(ShroudedItems.ITEM_TYPE, PersistentDataType.STRING);
		if (!ShroudedClassItems.TYPE_SHROUDED_IRON_SWORD.equals(type))
			return;

		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();

		// Only shrouded-class players may perform the stab
		LobbySession session = lobbyManager.getSessionForPlayer(uuid);
		if (session == null) {
			logger.log(Level.FINE,
					"[SwordStab] {0} right-clicked shrouded sword but is not in any session — ignoring.",
					player.getName());
			return;
		}
		if (session.getChosenClass(uuid) != PlayerClass.SHROUDED) {
			logger.log(Level.FINE,
					"[SwordStab] {0} right-clicked shrouded sword but class is {1} — ignoring.",
					new Object[] {
							player.getName(), session.getChosenClass(uuid)
					});
			return;
		}

		event.setCancelled(true);

		// Check cooldown
		long now = System.currentTimeMillis();
		long expiry = cooldownExpiry.getOrDefault(uuid, 0L);
		if (now < expiry) {
			long remaining = (expiry - now + 999) / 1000;
			logger.log(Level.FINE,
					"[SwordStab] {0} attempted stab but is on cooldown ({1}s remaining).",
					new Object[] {
							player.getName(), remaining
					});
			player.sendActionBar(
					Component.text("Sword Stab on cooldown: " + remaining + "s",
							NamedTextColor.RED));
			return;
		}

		double chargeSeconds = plugin.getConfig().getDouble(
				"shrouded-class.sword-stab-charge-time-seconds", 1.0);
		int chargeTicks = Math.max(1, (int) (chargeSeconds * 20));

		// Play charge wind-up sound and display the cooldown overlay for the
		// charge
		// duration
		double upVolume = plugin.getConfig()
				.getDouble("shrouded-class.sword-stab-charge-up-volume", 1.0);
		double upPitch = plugin.getConfig()
				.getDouble("shrouded-class.sword-stab-charge-up-pitch", 0.6);
		player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT,
				(float) upVolume, (float) upPitch);
		player.setCooldown(Material.IRON_SWORD, chargeTicks);

		// Block extra right-clicks during the charge phase
		cooldownExpiry.put(uuid, now + (long) (chargeSeconds * 1_000));
		logger.log(Level.FINE,
				"[SwordStab] {0} began sword stab charge ({1} ticks).",
				new Object[] {
						player.getName(), chargeTicks
				});

		new BukkitRunnable() {
			@Override
			public void run() {
				if (!player.isOnline())
					return;

				double damage = plugin.getConfig()
						.getDouble("shrouded-class.sword-stab-damage", 40.0);
				double knockbackVelocity = plugin.getConfig().getDouble(
						"shrouded-class.sword-stab-knockback-velocity", 5.0);
				double reach = plugin.getConfig()
						.getDouble("shrouded-class.sword-stab-reach", 4.0);
				double successSeconds = plugin.getConfig().getDouble(
						"shrouded-class.sword-stab-cooldown-seconds-success",
						3.0);
				double failSeconds = plugin.getConfig().getDouble(
						"shrouded-class.sword-stab-cooldown-seconds-fail",
						10.0);

				// Ray-trace for the first living entity the player is facing
				logger.log(Level.FINE,
						"[SwordStab] {0} resolving stab — reach={1}, dir={2}.",
						new Object[] {
								player.getName(), reach,
								player.getLocation().getDirection()
				});
				RayTraceResult result = player.getWorld().rayTraceEntities(
						player.getEyeLocation(),
						player.getLocation().getDirection(), reach, 0.5,
						e -> e instanceof LivingEntity && !e.equals(player));

				long hitTime = System.currentTimeMillis();

				if (result != null && result
						.getHitEntity() instanceof LivingEntity target) {
					// Hit — deal damage and apply knockback in the stab
					// direction
					logger.log(Level.FINE,
							"[SwordStab] {0} HIT {1} for {2} damage (knockback={3}). Success cooldown={4}s.",
							new Object[] {
									player.getName(), target.getName(), damage,
									knockbackVelocity, successSeconds
					});
					target.damage(damage, player);
					target.setVelocity(player.getLocation().getDirection()
							.normalize().multiply(knockbackVelocity));

					double successVolume = plugin.getConfig().getDouble(
							"shrouded-class.sword-stab-success-volume", 1.0);
					double successPitch = plugin.getConfig().getDouble(
							"shrouded-class.sword-stab-success-pitch", 1.0);

					player.playSound(player.getLocation(),
							Sound.ENTITY_PLAYER_ATTACK_CRIT,
							(float) successVolume, (float) successPitch);

					int successTicks = (int) (successSeconds * 20);
					player.setCooldown(Material.IRON_SWORD, successTicks);
					cooldownExpiry.put(uuid,
							hitTime + (long) (successSeconds * 1_000));
				} else {
					// Miss
					logger.log(Level.FINE,
							"[SwordStab] {0} MISSED stab. Fail cooldown={1}s.",
							new Object[] {
									player.getName(), failSeconds
					});
					double missVolume = plugin.getConfig().getDouble(
							"shrouded-class.sword-stab-miss-volume", 1.0);
					double missPitch = plugin.getConfig().getDouble(
							"shrouded-class.sword-stab-miss-pitch", 0.8);

					player.playSound(player.getLocation(),
							Sound.ENTITY_BREEZE_LAND, (float) missVolume,
							(float) missPitch);

					int failTicks = (int) (failSeconds * 20);
					player.setCooldown(Material.IRON_SWORD, failTicks);
					cooldownExpiry.put(uuid,
							hitTime + (long) (failSeconds * 1_000));
				}
			}
		}.runTaskLater(plugin, chargeTicks);
	}
}
