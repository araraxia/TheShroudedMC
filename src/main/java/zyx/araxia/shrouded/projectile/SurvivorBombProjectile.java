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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import zyx.araxia.shrouded.TheShrouded;

/**
 * Physics-driven projectile for the Survivor's Impact Bomb.
 *
 * <p>Each server tick the runnable:
 * <ol>
 *   <li>Applies drag to the current velocity.</li>
 *   <li>Subtracts gravity from the Y component.</li>
 *   <li>Clamps speed to the configured terminal velocity.</li>
 *   <li>Ray-traces ahead for block collisions.</li>
 *   <li>Teleports the {@link ItemDisplay} visual to the new position.</li>
 *   <li>Checks for entity collisions within the configured hitbox radius.</li>
 * </ol>
 * On any collision (or after the configured lifetime) the bomb
 * detonates: a visual explosion is spawned and all {@link LivingEntity}s within
 * the configured radius receive flat damage and knockback.
 * <p>
 * All physics and explosion parameters are injected at construction time from
 * {@code config.yml}.
 */
public class SurvivorBombProjectile extends BukkitRunnable {

    // -------------------------------------------------------------------------
    // Physics constants (match spec)
    // -------------------------------------------------------------------------
    /** Fraction of velocity retained each tick. */
    private final double drag;

    /** Downward acceleration applied each tick (blocks/tick²). */
    private final double gravity;

    /** Maximum speed in any direction (blocks/tick). */
    private final double maxSpeed;

    /** Half-extents of the AABB used for entity collision (= hitbox / 2). */
    private final double hitboxRadius;

    /** Ticks before the bomb self-destructs without hitting anything. */
    private final int maxLifetimeTicks;

    private static final Logger LOGGER =
            JavaPlugin.getPlugin(TheShrouded.class).getLogger();

    // -------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------
    private final Player owner;
    private final double explosionRadius;
    private final double explosionDamage;

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
     * @param owner           the player who threw the bomb
     * @param explosionRadius blast radius (blocks)
     * @param explosionDamage flat damage dealt to every entity in range
     * @param drag            fraction of velocity retained each tick
     * @param gravity         downward acceleration per tick (blocks/tick²)
     * @param maxSpeed        terminal velocity (blocks/tick)
     * @param hitboxRadius    AABB half-extent for entity collision
     * @param maxLifetimeTicks ticks before the bomb despawns without impact
     */
    public SurvivorBombProjectile(Player owner,
            double explosionRadius,
            double explosionDamage,
            double drag,
            double gravity,
            double maxSpeed,
            double hitboxRadius,
            int maxLifetimeTicks) {
        this.owner = owner;
        this.explosionRadius = explosionRadius;
        this.explosionDamage = explosionDamage;
        this.drag = drag;
        this.gravity = gravity;
        this.maxSpeed = maxSpeed;
        this.hitboxRadius = hitboxRadius;
        this.maxLifetimeTicks = maxLifetimeTicks;

        // Spawn slightly ahead of the player's eyes so the sphere doesn't
        // immediately overlap the thrower's hitbox.
        Vector look = owner.getLocation().getDirection().normalize();
        this.position = owner.getEyeLocation().add(look.clone().multiply(0.6));
        this.velocity = look.clone(); // 1 block/tick initial speed

        // Spawn the visual entity
        this.display = owner.getWorld().spawn(position, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.PITCHER_POD));
            d.setBillboard(Billboard.CENTER);
            // Scale the display to match the 0.25 × 0.25 hitbox
            d.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    new Vector3f(0.25f, 0.25f, 0.25f),
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
            // Silently despawn — hit nothing
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

        // --- Block collision (ray trace current → next) ---
        // Extend the ray by hitboxRadius so the shell of the sphere hits first.
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

        // Visual + audio
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 4f, 1f);

        // Flat damage + knockback to all living entities within radius
        for (LivingEntity entity : world.getNearbyLivingEntities(loc, explosionRadius)) {
            // Exclude the thrower from receiving damage
            if (entity.getUniqueId().equals(owner.getUniqueId()))
                continue;

            entity.damage(explosionDamage, owner);

            // Radial knockback away from explosion centre
            Vector knockback = entity.getLocation().toVector()
                    .subtract(loc.toVector());
            if (knockback.lengthSquared() > 0) {
                knockback.normalize().multiply(1.5);
            } else {
                knockback.setY(1.0); // directly on top → launch upward
            }
            entity.setVelocity(entity.getVelocity().add(knockback));
        }

        LOGGER.log(Level.FINE,
                "[TheShrouded] SurvivorBomb detonated at ({0}, {1}, {2}) by {3}",
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
