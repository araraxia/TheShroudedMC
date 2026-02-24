package zyx.araxia.shrouded.game;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import zyx.araxia.shrouded.TheShrouded;
import zyx.araxia.shrouded.item.ShroudedItems;
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
        this.plugin.getLogger().log(
                java.util.logging.Level.FINE,
                "Equipping survivor class for player {0}",
                player.getName());
        // Hotbar
        player.getInventory().setItem(0,
                SurvivorClassItems.createSurvivorIronSword());
        player.getInventory().setItem(1,
                SurvivorClassItems.createSurvivorHealthSplashPotion1());
        player.getInventory().setItem(2,
                SurvivorClassItems.createSurvivorBomb());
        player.getInventory().setItem(3,
                SurvivorClassItems.createSurvivorWeb());
        player.getInventory().setItem(4,
                SurvivorClassItems.createSurvivorWindCharge());

        // Last hotbar slot — utility
        player.getInventory().setItem(8,
                ShroudedItems.createReturnToLobby());

        // Armour
        player.getInventory().setHelmet(
                SurvivorClassItems.createSurvivorChainHelmet());
        player.getInventory().setChestplate(
                SurvivorClassItems.createSurvivorChainChestplate());
        player.getInventory().setLeggings(
                SurvivorClassItems.createSurvivorChainLeggings());
        player.getInventory().setBoots(
                SurvivorClassItems.createSurvivorChainBoots());
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
