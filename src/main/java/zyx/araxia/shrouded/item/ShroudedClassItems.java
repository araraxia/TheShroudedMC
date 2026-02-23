package zyx.araxia.shrouded.item;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Factory and helpers for items exclusive to the
 * {@link zyx.araxia.shrouded.game.PlayerClass#SHROUDED SHROUDED} class.
 *
 * <p>Every item created here should be tagged via
 * {@link ShroudedItems#tagItem} with:
 * <ul>
 *   <li>{@link ShroudedItems#IS_SHROUDED_ITEM} = 1</li>
 *   <li>{@link ShroudedItems#CLASS_TAG} = {@value #CLASS_VALUE}</li>
 *   <li>{@link ShroudedItems#LOCATION_TAG} =
 *       {@link ShroudedItems#LOCATION_ARENA_ALIVE}</li>
 * </ul>
 */
public final class ShroudedClassItems {

    /** Value written to {@link ShroudedItems#CLASS_TAG} for every item in this class. */
    public static final String CLASS_VALUE = "shrouded";

    // -------------------------------------------------------------------------
    // item_type values
    // -------------------------------------------------------------------------
    // TODO: add TYPE_* constants here as class items are designed.
    // Example: public static final String TYPE_SMOKE_BOMB = "shrouded_smoke_bomb";

    private ShroudedClassItems() {}

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------
    // TODO: add createXxx() factory methods here as class items are designed.
    // Example:
    //   public static ItemStack createSmokeBomb() {
    //       ItemStack item = new ItemStack(Material.FIRE_CHARGE);
    //       ItemMeta meta = item.getItemMeta();
    //       meta.displayName(...);
    //       ShroudedItems.tagItem(meta, TYPE_SMOKE_BOMB, ShroudedItems.LOCATION_ARENA_ALIVE, CLASS_VALUE);
    //       item.setItemMeta(meta);
    //       return item;
    //   }

    // -------------------------------------------------------------------------
    // Inspection helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if {@code item} carries the
     * {@link ShroudedItems#CLASS_TAG} value for this class.
     */
    public static boolean isItem(ItemStack item) {
        if (!ShroudedItems.isShroudedItem(item)) return false;
        String tag = item.getItemMeta()
                .getPersistentDataContainer()
                .get(ShroudedItems.CLASS_TAG, PersistentDataType.STRING);
        return CLASS_VALUE.equals(tag);
    }

    /**
     * Removes every Shrouded-class-tagged item from the player's inventory.
     */
    public static void removeItems(Player player) {
        ShroudedItems.removeItemsMatching(player, ShroudedClassItems::isItem);
    }
}
