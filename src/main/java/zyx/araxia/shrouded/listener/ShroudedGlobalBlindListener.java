package zyx.araxia.shrouded.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import zyx.araxia.shrouded.item.ShroudedClassItems;
import zyx.araxia.shrouded.item.ShroudedItems;
import zyx.araxia.shrouded.lobby.Arena;
import zyx.araxia.shrouded.lobby.ArenaManager;

/**
 * Handles right-click use of the
 * {@link ShroudedClassItems#TYPE_GLOBAL_BLIND_SCULK Lights Out} sculk item.
 *
 * <p>
 * On use, every player (other than the caster) currently inside the same
 * arena region receives {@link PotionEffectType#BLINDNESS} for
 * {@code shrouded-class.blind-lightsout-duration-seconds}.
 *
 * <ul>
 * <li>The caster is shown the particle configured at
 * {@code shrouded-class.blind-lightsout-itemuse-particle-enum}.</li>
 * <li>Each blinded player is shown the particle configured at
 * {@code shrouded-class.blind-lightsout-blinded-player-particle-enum}.</li>
 * </ul>
 *
 * <p>
 * The item is put on cooldown for
 * {@code shrouded-class.blind-lightsout-cooldown-seconds} after activation.
 */
public class ShroudedGlobalBlindListener implements Listener {

    private final JavaPlugin plugin;
    private final ArenaManager arenaManager;

    /** Tracks when each player's lights-out cooldown expires (epoch ms). */
    private final Map<UUID, Long> cooldownExpiry = new HashMap<>();

    public ShroudedGlobalBlindListener(JavaPlugin plugin,
            ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    // -------------------------------------------------------------------------
    // Event handler
    // -------------------------------------------------------------------------

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK)
            return;

        // Fire only for the main hand to avoid double-firing
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        ItemStack item = event.getItem();
        if (item == null || !ShroudedItems.isShroudedItem(item))
            return;

        String type = item.getItemMeta().getPersistentDataContainer()
                .get(ShroudedItems.ITEM_TYPE, PersistentDataType.STRING);
        if (!ShroudedClassItems.TYPE_GLOBAL_BLIND_SCULK.equals(type))
            return;

        event.setCancelled(true);

        Player caster = event.getPlayer();
        long now = System.currentTimeMillis();
        long expiry = cooldownExpiry.getOrDefault(caster.getUniqueId(), 0L);

        if (now < expiry) {
            long remaining = (expiry - now + 999L) / 1000L;
            caster.sendActionBar(Component.text(
                    "Lights Out on cooldown: " + remaining + "s remaining",
                    NamedTextColor.RED));
            return;
        }

        // -----------------------------------------------------------------
        // Resolve the arena the caster is standing in
        // -----------------------------------------------------------------
        Arena arena = arenaManager.getArenaContaining(caster.getLocation());
        if (arena == null) {
            caster.sendActionBar(Component.text(
                    "You must be inside an arena to use this ability.",
                    NamedTextColor.RED));
            return;
        }

        // -----------------------------------------------------------------
        // Read config values
        // -----------------------------------------------------------------
        double cooldownSeconds = plugin.getConfig()
                .getDouble("shrouded-class.blind-lightsout-cooldown-seconds", 180.0);
        double durationSeconds = plugin.getConfig()
                .getDouble("shrouded-class.blind-lightsout-duration-seconds", 3.0);
        int durationTicks = (int) (durationSeconds * 20.0);

        Particle casterParticle = parseParticle(
                plugin.getConfig().getString(
                        "shrouded-class.blind-lightsout-itemuse-particle-enum",
                        "SCULK_SOUL"),
                Particle.SCULK_SOUL);

        Particle blindedParticle = parseParticle(
                plugin.getConfig().getString(
                        "shrouded-class.blind-lightsout-blinded-player-particle-enum",
                        "SQUID_INK"),
                Particle.SQUID_INK);

        // -----------------------------------------------------------------
        // Collect arena players (excluding the caster)
        // -----------------------------------------------------------------
        List<Player> targets = new ArrayList<>();
        for (Player p : caster.getWorld().getPlayers()) {
            if (p.getUniqueId().equals(caster.getUniqueId()))
                continue;
            if (arena.contains(p.getLocation()))
                targets.add(p);
        }

        // -----------------------------------------------------------------
        // Apply blindness and per-player particles
        // -----------------------------------------------------------------
        PotionEffect blindEffect = new PotionEffect(
                PotionEffectType.BLINDNESS, durationTicks, 0, true, true, true);

        for (Player target : targets) {
            target.addPotionEffect(blindEffect);
            spawnPlayerParticles(target, blindedParticle, 30);
        }

        // -----------------------------------------------------------------
        // Caster particles (visual feedback at the use location)
        // -----------------------------------------------------------------
        spawnPlayerParticles(caster, casterParticle, 20);

        // -----------------------------------------------------------------
        // Apply cooldown
        // -----------------------------------------------------------------
        long cooldownMillis = (long) (cooldownSeconds * 1000.0);
        int cooldownTicks = (int) (cooldownSeconds * 20.0);
        cooldownExpiry.put(caster.getUniqueId(), now + cooldownMillis);
        caster.setCooldown(Material.SCULK, cooldownTicks);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Spawns {@code count} particles scattered randomly around the player's
     * body centre (±0.5 blocks on each axis).
     */
    private static void spawnPlayerParticles(Player player, Particle particle,
            int count) {
        player.getWorld().spawnParticle(
                particle,
                player.getLocation().add(0.0, 1.0, 0.0),
                count,
                0.5, 0.6, 0.5,
                0.05,
                null,
                true);
    }

    /** Parses a {@link Particle} by name, returning {@code fallback} on failure. */
    private static Particle parseParticle(String name, Particle fallback) {
        if (name == null)
            return fallback;
        try {
            // Accept both dash-separated and underscore-separated names
            return Particle.valueOf(name.replace('-', '_').toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
