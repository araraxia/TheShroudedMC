package zyx.araxia.shrouded.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import zyx.araxia.shrouded.item.ShroudedClassItems;
import zyx.araxia.shrouded.item.ShroudedItems;

/**
 * Handles right-click use of the
 * {@link ShroudedClassItems#TYPE_POISON_WAVE_WEATH_COP_LANTERN Toxic Cloud}
 * weathered copper lantern.
 *
 * <p>
 * On use, three spherical particle clouds are launched outward in the
 * player's facing direction. Each cloud is centred at a configured range from
 * the caster and appears after its own spawn-delay. While a cloud is active,
 * any player (other than the Shrouded user) whose position lies within the
 * sphere radius is:
 * <ul>
 * <li>inflicted with {@link PotionEffectType#POISON} for
 * {@code shrouded-class.toxic-cloud-poison-dot-duration-seconds}; and</li>
 * <li>dealt {@code shrouded-class.toxic-cloud-cloud-dot-damage} raw damage</li>
 * </ul>
 * once every {@code shrouded-class.toxic-cloud-cloud-dot-interval-seconds}.
 *
 * <p>
 * The ability is put on cooldown for
 * {@code shrouded-class.toxic-cloud-cooldown-seconds} after activation.
 */
public class ShroudedToxicCloudListener implements Listener {

    private final JavaPlugin plugin;

    /**
     * Tracks when each player's toxic-cloud cooldown expires (epoch
     * milliseconds).
     */
    private final Map<UUID, Long> cooldownExpiry = new HashMap<>();

    public ShroudedToxicCloudListener(JavaPlugin plugin) {
        this.plugin = plugin;
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
        if (!ShroudedClassItems.TYPE_POISON_WAVE_WEATH_COP_LANTERN.equals(type))
            return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long expiry = cooldownExpiry.getOrDefault(player.getUniqueId(), 0L);

        if (now < expiry) {
            long remaining = (expiry - now + 999L) / 1000L;
            player.sendActionBar(Component.text(
                    "Toxic Cloud on cooldown: " + remaining + "s remaining",
                    NamedTextColor.RED));
            return;
        }

        // -----------------------------------------------------------------
        // Read all config values up front
        // -----------------------------------------------------------------
        double cooldownSeconds = plugin.getConfig()
                .getDouble("shrouded-class.toxic-cloud-cooldown-seconds", 120.0);
        double poisonDotDurationSeconds = plugin.getConfig()
                .getDouble("shrouded-class.toxic-cloud-poison-dot-duration-seconds", 5.0);
        double cloudDotDamage = plugin.getConfig()
                .getDouble("shrouded-class.toxic-cloud-cloud-dot-damage", 1.0);
        double cloudDotIntervalSeconds = plugin.getConfig()
                .getDouble("shrouded-class.toxic-cloud-cloud-dot-interval-seconds", 1.0);

        final long dotIntervalTicks = Math.max(1L, (long) (cloudDotIntervalSeconds * 20.0));
        final int poisonDurationTicks = (int) (poisonDotDurationSeconds * 20.0);

        final double[] radii = {
                plugin.getConfig().getDouble("shrouded-class.toxic-cloud-hitbox-1-radius", 1.0),
                plugin.getConfig().getDouble("shrouded-class.toxic-cloud-hitbox-2-radius", 2.0),
                plugin.getConfig().getDouble("shrouded-class.toxic-cloud-hitbox-3-radius", 3.0)
        };
        final double[] ranges = {
                plugin.getConfig().getDouble("shrouded-class.toxic-cloud-hitbox-1-range", 2.0),
                plugin.getConfig().getDouble("shrouded-class.toxic-cloud-hitbox-2-range", 5.0),
                plugin.getConfig().getDouble("shrouded-class.toxic-cloud-hitbox-3-range", 8.0)
        };
        final long[] cloudDurationTicks = {
                (long) (plugin.getConfig().getDouble("shrouded-class.toxic-cloud-hitbox-1-duration-seconds", 5.0)
                        * 20.0),
                (long) (plugin.getConfig().getDouble("shrouded-class.toxic-cloud-hitbox-2-duration-seconds", 7.0)
                        * 20.0),
                (long) (plugin.getConfig().getDouble("shrouded-class.toxic-cloud-hitbox-3-duration-seconds", 10.0)
                        * 20.0)
        };
        final long[] spawnDelays = {
                plugin.getConfig().getLong("shrouded-class.toxic-cloud-hitbox-1-spawn-delay-ticks", 5L),
                plugin.getConfig().getLong("shrouded-class.toxic-cloud-hitbox-2-spawn-delay-ticks", 15L),
                plugin.getConfig().getLong("shrouded-class.toxic-cloud-hitbox-3-spawn-delay-ticks", 30L)
        };

        // Particle settings
        final boolean useForce = plugin.getConfig()
                .getBoolean("shrouded-class.toxic-cloud-particle-useForce", true);

        final Particle primaryParticle = parseParticle(
                plugin.getConfig().getString("shrouded-class.toxic-cloud-particle-primary-enum", "ENTITY_EFFECT"),
                Particle.ENTITY_EFFECT);
        final Color primaryColor = readColor(
                plugin.getConfig().getDouble("shrouded-class.toxic-cloud-particle-primary-color-a", 0.35),
                plugin.getConfig().getInt("shrouded-class.toxic-cloud-particle-primary-color-r", 120),
                plugin.getConfig().getInt("shrouded-class.toxic-cloud-particle-primary-color-g", 255),
                plugin.getConfig().getInt("shrouded-class.toxic-cloud-particle-primary-color-b", 120));
        final int[] primaryCounts = {
                plugin.getConfig().getInt("shrouded-class.toxic-cloud-particle-primary-hitbox-1-count", 20),
                plugin.getConfig().getInt("shrouded-class.toxic-cloud-particle-primary-hitbox-2-count", 40),
                plugin.getConfig().getInt("shrouded-class.toxic-cloud-particle-primary-hitbox-3-count", 60)
        };

        final Particle secondaryParticle = parseParticle(
                plugin.getConfig().getString("shrouded-class.toxic-cloud-particle-secondary-enum", "COPPER_FIRE_FLAME"),
                Particle.ENTITY_EFFECT);
        final Color secondaryColor = readColor(
                plugin.getConfig().getDouble("shrouded-class.toxic-cloud-particle-secondary-color-a", 0.35),
                plugin.getConfig().getInt("shrouded-class.toxic-cloud-particle-secondary-color-r", 120),
                plugin.getConfig().getInt("shrouded-class.toxic-cloud-particle-secondary-color-g", 255),
                plugin.getConfig().getInt("shrouded-class.toxic-cloud-particle-secondary-color-b", 120));
        final int[] secondaryCounts = {
                plugin.getConfig().getInt("shrouded-class.toxic-cloud-particle-secondary-hitbox-1-count", 10),
                plugin.getConfig().getInt("shrouded-class.toxic-cloud-particle-secondary-hitbox-2-count", 20),
                plugin.getConfig().getInt("shrouded-class.toxic-cloud-particle-secondary-hitbox-3-count", 30)
        };

        // -----------------------------------------------------------------
        // Compute the horizontal facing direction and cloud centre locations
        // -----------------------------------------------------------------
        Vector dir = player.getLocation().getDirection();
        dir.setY(0.0);
        // Guard against looking straight up or straight down
        if (dir.lengthSquared() < 1e-6) {
            dir = new Vector(1.0, 0.0, 0.0);
        } else {
            dir.normalize();
        }

        // Use the player's mid-body height as the cloud origin
        Location origin = player.getLocation().clone().add(0.0, player.getEyeHeight() * 0.5, 0.0);

        final UUID shooterUUID = player.getUniqueId();

        // -----------------------------------------------------------------
        // Schedule each cloud
        // -----------------------------------------------------------------
        for (int i = 0; i < 3; i++) {
            final Location cloudCenter = origin.clone().add(dir.clone().multiply(ranges[i]));
            final double cloudRadius = radii[i];
            final long maxTicks = cloudDurationTicks[i];
            final int pCount = primaryCounts[i];
            final int sCount = secondaryCounts[i];
            final double dotDamage = cloudDotDamage;
            final int poisonTicks = poisonDurationTicks;
            final long dotTicks = dotIntervalTicks;

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                final long[] elapsed = { 0L };
                final BukkitTask[] ref = { null };

                ref[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                    if (elapsed[0] >= maxTicks) {
                        ref[0].cancel();
                        return;
                    }

                    // Render – scatter particles across the sphere shell
                    spawnSphereParticles(cloudCenter, cloudRadius,
                            primaryParticle, pCount, primaryColor, useForce);
                    spawnSphereParticles(cloudCenter, cloudRadius,
                            secondaryParticle, sCount, secondaryColor, useForce);

                    // Damage & poison check on the configured interval
                    if (elapsed[0] % dotTicks == 0L) {
                        for (Player p : cloudCenter.getWorld().getPlayers()) {
                            if (p.getUniqueId().equals(shooterUUID))
                                continue;
                            // Check player body centre against the cloud centre
                            Location bodyCentre = p.getLocation().add(0.0, 1.0, 0.0);
                            if (bodyCentre.distance(cloudCenter) <= cloudRadius) {
                                p.addPotionEffect(new PotionEffect(
                                        PotionEffectType.POISON,
                                        poisonTicks, 0, true, true, true));
                                p.damage(dotDamage);
                            }
                        }
                    }

                    elapsed[0]++;
                }, 0L, 1L);
            }, spawnDelays[i]);
        }

        // -----------------------------------------------------------------
        // Apply cooldown
        // -----------------------------------------------------------------
        long cooldownMillis = (long) (cooldownSeconds * 1000.0);
        int cooldownTicksInt = (int) (cooldownSeconds * 20.0);
        cooldownExpiry.put(player.getUniqueId(), now + cooldownMillis);
        player.setCooldown(Material.WEATHERED_COPPER_LANTERN, cooldownTicksInt);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Spawns {@code count} particles scattered randomly across the surface of a
     * sphere centred at {@code center} with the given {@code radius}.
     *
     * <p>
     * When the particle type is {@link Particle#ENTITY_EFFECT} the
     * {@link Color} value is forwarded as particle data. All other types are
     * spawned without extra data.
     */
    private static void spawnSphereParticles(Location center, double radius,
            Particle particle, int count, Color color, boolean force) {
        for (int i = 0; i < count; i++) {
            // Uniform spherical distribution (Marsaglia / trigonometric method)
            double theta = Math.random() * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * Math.random() - 1.0);
            double sinPhi = Math.sin(phi);
            double x = radius * sinPhi * Math.cos(theta);
            double y = radius * sinPhi * Math.sin(theta);
            double z = radius * Math.cos(phi);
            Location loc = center.clone().add(x, y, z);

            if (particle == Particle.ENTITY_EFFECT) {
                center.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0, color, force);
            } else {
                center.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0, null, force);
            }
        }
    }

    /** Parses a {@link Particle} by name, returning {@code fallback} on error. */
    private static Particle parseParticle(String name, Particle fallback) {
        try {
            return Particle.valueOf(name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /**
     * Builds a {@link Color} from an alpha-in-[0,1] double and raw RGB ints
     * [0–255].
     */
    private static Color readColor(double alpha01, int r, int g, int b) {
        int a = Math.max(0, Math.min(255, (int) (alpha01 * 255.0)));
        return Color.fromARGB(a, r, g, b);
    }
}
