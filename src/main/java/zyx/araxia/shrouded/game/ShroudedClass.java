package zyx.araxia.shrouded.game;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import zyx.araxia.shrouded.TheShrouded;
import zyx.araxia.shrouded.item.ShroudedClassItems;
import zyx.araxia.shrouded.item.ShroudedItems;

/**
 * Handles equipping and managing the {@link PlayerClass#SHROUDED} kit for a
 * player.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>{@link #equip(Player)} — gives the player their full shrouded kit and
 * starts hiding their equipment from other players via
 * {@link zyx.araxia.shrouded.listener.ShroudedEquipmentSpoofer}.</li>
 * <li>{@link #unequip(Player)} — removes every shrouded-tagged item from the
 * player's inventory and stops the equipment spoof.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * // When the match starts and a player is assigned SHROUDED:
 * ShroudedClass shroudedClass = new ShroudedClass(plugin);
 * shroudedClass.equip(player);
 *
 * // When the player leaves or dies:
 * shroudedClass.unequip(player);
 * }</pre>
 */
public class ShroudedClass {

    private final TheShrouded plugin;

    public ShroudedClass(JavaPlugin plugin) {
        this.plugin = (TheShrouded) plugin;
    }

    // -------------------------------------------------------------------------
    // Kit management
    // -------------------------------------------------------------------------

    /**
     * Gives the player their full shrouded kit and starts spoofing their
     * equipment so that other players cannot see what they are holding or
     * wearing.
     *
     * @param player the online player to equip
     */
    public void equip(Player player) {
        plugin.getLogger().log(
                java.util.logging.Level.FINE,
                "Equipping shrouded class for player {0}",
                player.getName());

        // Hotbar
        player.getInventory().setItem(0, ShroudedClassItems.createShroudedIronSword());
        player.getInventory().setItem(1, ShroudedClassItems.createLeviBombChorusFlower());

        // Last hotbar slot — utility
        player.getInventory().setItem(8, ShroudedItems.createReturnToLobby());

        // Armour (invisible to other players via EQUIPPABLE component + spoofer)
        player.getInventory().setHelmet(ShroudedClassItems.createShroudedNethHelmet());
        player.getInventory().setChestplate(ShroudedClassItems.createShroudedNethChestplate());
        player.getInventory().setLeggings(ShroudedClassItems.createShroudedNethLeggings());
        player.getInventory().setBoots(ShroudedClassItems.createShroudedNethBoots());

        // Begin intercepting ENTITY_EQUIPMENT packets for this player
        plugin.getEquipmentSpoofer().startSpoofing(player);
    }

    /**
     * Removes every shrouded-class-tagged item from the player's inventory and
     * stops spoofing their equipment.
     * Call this when the player dies, leaves, or the round ends.
     *
     * @param player the online player to strip
     */
    public void unequip(Player player) {
        ShroudedClassItems.removeItems(player);
        plugin.getEquipmentSpoofer().stopSpoofing(player);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Convenience factory; resolves the plugin instance automatically.
     */
    public static ShroudedClass fromPlugin() {
        return new ShroudedClass(JavaPlugin.getPlugin(TheShrouded.class));
    }
}
