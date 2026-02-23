package zyx.araxia.shrouded.game;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import zyx.araxia.shrouded.TheShrouded;
import zyx.araxia.shrouded.item.SurvivorClassItems;

/**
 * Handles equipping and managing the {@link PlayerClass#SURVIVOR} kit for a
 * player.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>{@link #equip(Player)} — gives the player their full survivor kit,
 * reading item stats from {@code config.yml} at call time.</li>
 * <li>{@link #unequip(Player)} — removes every survivor-tagged item from
 * the player's inventory.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * 
 * <pre>{@code
 * // When the match starts and a player is assigned SURVIVOR:
 * SurvivorClass survivorClass = new SurvivorClass(plugin);
 * survivorClass.equip(player);
 *
 * // When the player leaves or dies:
 * survivorClass.unequip(player);
 * }</pre>
 */
public class SurvivorClass {

    private final JavaPlugin plugin;

    public SurvivorClass(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Kit management
    // -------------------------------------------------------------------------

    /**
     * Gives the player their full survivor kit. Item stats (damage multiplier,
     * swing cooldown, heal level) are read from {@code config.yml} at call time
     * so that a server reload is picked up without a restart.
     *
     * @param player the online player to equip
     */
    public void equip(Player player) {
        double damageMultiplier = plugin.getConfig().getDouble("survivor.sword-damage-multiplier", 1.0);
        double swingCooldownSeconds = plugin.getConfig().getDouble("survivor.sword-swing-cooldown-seconds", 0.625);
        int healLevel = plugin.getConfig().getInt("survivor.health-potion-heal-level", 2);

        // Hotbar
        player.getInventory().setItem(0,
                SurvivorClassItems.createSurvivorIronSword(damageMultiplier, swingCooldownSeconds));
        player.getInventory().setItem(1, SurvivorClassItems.createSurvivorHealthSplashPotion1(healLevel));

        // TODO: give remaining kit items as they are implemented
        // (bomb, web, wind charge, chain armour set, etc.)
    }

    /**
     * Removes every survivor-class-tagged item from the player's inventory.
     * Call this when the player dies, leaves, or the round ends.
     *
     * @param player the online player to strip
     */
    public void unequip(Player player) {
        SurvivorClassItems.removeItems(player);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Convenience factory; resolves the plugin instance automatically.
     */
    public static SurvivorClass create() {
        return new SurvivorClass(JavaPlugin.getPlugin(TheShrouded.class));
    }
}
