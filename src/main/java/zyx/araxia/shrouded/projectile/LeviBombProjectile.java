package zyx.araxia.shrouded.projectile;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import zyx.araxia.shrouded.TheShrouded;

/**
 * Physics-driven projectile for the Shrouded class's Levitation Bomb.
 *
 * <p>Follows the same tick-based physics loop as {@link SurvivorBombProjectile}.
 * On detonation (block/entity collision or lifetime expiry) every
 * {@link LivingEntity} within the configured explosion radius (excluding the
 * thrower) receives a {@link PotionEffectType#LEVITATION} effect for the
 * configured duration.
 */
public class LeviBombProjectile extends BukkitRunnable {

    // -------------------------------------------------------------------------
    // Physics constants
    // -------------------------------------------------------------------------
    private final double drag;
    private final double gravity;
    private final double maxSpeed;
    private final double hitboxRadius;
    private final int maxLifetimeTicks;

    private static final Logger LOGGER =
            JavaPlugin.getPlugin(TheShrouded.class).getLogger();

    // -------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------
    private final Player owner;
    private final double explosionRadius;
    private final int levitationDurationTicks;

    private final Location position;
    private final Vector velocity;
    private final ItemDisplay display;
    private int ticksLived = 0;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Spawns the visual {@link ItemDisplay} and prepares the initial velocity.
     *
     * @param owner                   the player who threw the bomb
     * @param explosionRadius         blast radius (blocks)
     * @param levitationDurationTicks how many ticks the levitation effect lasts
     * @param drag                    fraction of velocity retained each tick
     * @param gravity                 downward acceleration per tick (blocks/tick²)
     * @param maxSpeed                terminal velocity (blocks/tick)
     * @param hitboxRadius            AABB half-extent for entity collision
     * @param maxLifetimeTicks        ticks before the bomb despawns without impact
     * @param throwVelocity           initial speed multiplier (blocks/tick)
     */
    public LeviBombProjectile(Player owner,
            double explosionRadius,
            int levitationDurationTicks,
            double drag,
            double gravity,
            double maxSpeed,
            double hitboxRadius,
            int maxLifetimeTicks,
            double throwVelocity) {
        this.owner = owner;
        this.explosionRadius = explosionRadius;
        this.levitationDurationTicks = levitationDurationTicks;
        this.drag = drag;
        this.gravity = gravity;
        this.maxSpeed = maxSpeed;
        this.hitboxRadius = hitboxRadius;
        this.maxLifetimeTicks = maxLifetimeTicks;

        Vector look = owner.getLocation().getDirection().normalize();
        this.position = owner.getEyeLocation().add(look.clone().multiply(0.6));
        this.velocity = look.clone().multiply(throwVelocity);

        this.display = owner.getWorld().spawn(position, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.CHORUS_FLOWER));
            d.setBillboard(Billboard.CENTER);
            d.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    new Vector3f(0.3f, 0.3f, 0.3f),
                    new Quaternionf()));
        });
    }

    // -------------------------------------------------------------------------
    // BukkitRunnable
    // -------------------------------------------------------------------------

    @Override
    public void run() {
        if (display.isDead() || !display.isValid()) {
            cancel();
            return;
        }

        if (++ticksLived > maxLifetimeTicks) {
            explode(position.clone());
            return;
        }

        World world = position.getWorld();

        // --- Physics step ---
        velocity.multiply(drag);
        velocity.setY(velocity.getY() - gravity);
        double speed = velocity.length();
        if (speed > maxSpeed) {
            velocity.multiply(maxSpeed / speed);
        }

        // --- Block collision ---
        RayTraceResult blockHit = world.rayTraceBlocks(
                position,
                velocity.clone().normalize(),
                velocity.length() + hitboxRadius,
                FluidCollisionMode.NEVER,
                true);
        if (blockHit != null) {
            explode(blockHit.getHitPosition().toLocation(world));
            return;
        }

        // --- Move ---
        position.add(velocity);
        display.teleport(position);

        // --- Entity collision ---
        Collection<Entity> hit = world.getNearbyEntities(
                position,
                hitboxRadius, hitboxRadius, hitboxRadius,
                e -> e instanceof LivingEntity
                        && !e.getUniqueId().equals(owner.getUniqueId())
                        && e != display);
        if (!hit.isEmpty()) {
            explode(position.clone());
        }
    }

    // -------------------------------------------------------------------------
    // Explosion
    // -------------------------------------------------------------------------

    private void explode(Location loc) {
        World world = loc.getWorld();

        // Visual + audio — portal particles suit the End-themed Chorus Flower
        world.spawnParticle(Particle.PORTAL, loc, 80, 0.5, 0.5, 0.5, 0.2);
        world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 3f, 0.8f);

        // Levitation I for the configured duration, ambient so no particles spam
        PotionEffect levitation = new PotionEffect(
                PotionEffectType.LEVITATION,
                levitationDurationTicks,
                0,     // amplifier 0 = Levitation I
                false, // not ambient
                true); // show particles

        for (LivingEntity entity : world.getNearbyLivingEntities(loc, explosionRadius)) {
            if (entity.getUniqueId().equals(owner.getUniqueId()))
                continue;
            entity.addPotionEffect(levitation);
        }

        LOGGER.log(Level.FINE,
                "[TheShrouded] LeviBomb detonated at ({0}, {1}, {2}) by {3}",
                new Object[] {
                        loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                        owner.getName() });

        cleanup();
    }

    private void cleanup() {
        if (!display.isDead()) {
            display.remove();
        }
        if (!isCancelled()) {
            cancel();
        }
    }
}
