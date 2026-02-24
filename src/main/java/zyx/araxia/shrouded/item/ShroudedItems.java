package zyx.araxia.shrouded.item;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

// Logger imports for debugging
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import zyx.araxia.shrouded.TheShrouded;

/**
 * Factory and utility class for all plugin-managed {@link ItemStack}s.
 * <h3>NBT tag scheme</h3>
 * <ul>
 * <li>{@code shrouded:is_shrouded_item} {@code (byte 1)} — present on
 * <em>every</em> item created by this plugin; used for inventory sweeps on
 * session end.</li>
 * <li>{@code shrouded:item_type} {@code (String)} — identifies the specific
 * role of the item (e.g. {@value #TYPE_CLASS_SELECTOR}).</li>
 * <li>{@code shrouded:location_tag} {@code (String)} — identifies the logical
 * location where this item should be used (e.g. {@value #LOCATION_LOBBY}); used
 * for easier cleanup of items that should only exist in certain regions.</li>
 * </ul>
 * <h3>Public methods</h3>
 * <ul>
 * <li>{@link #createClassSelector()} — factory method for the class selector
 * item.</li>
 * <li>{@link #createHowToPlayGuide()} — factory method for the how-to-play
 * guide book item.</li>
 * <li>{@link #isShroudedItem(ItemStack)} — returns true if the stack carries
 * the global plugin marker.</li>
 * <li>{@link #isClassSelector(ItemStack)} — returns true if the stack is the
 * class selector item.</li>
 * <li>{@link #removeShroudedItems(Player)} — removes every plugin-tagged item
 * from the player's inventory; call this when a player leaves a session to
 * prevent lobby items from escaping into the world.</li>
 * <li>{@link #removeLobbyItems(Player)} — removes every lobby-tagged item from
 * the player's inventory; call this when a player leaves a lobby session to
 * prevent lobby items from escaping into the world.</li>
 * <li>{@link #removeArenaAliveItems(Player)} — removes every arena-alive-tagged
 * item from the player's inventory; call this when a player dies in the arena
 * to prevent alive-only items from being used by spectators.</li>
 * <li>{@link #removeArenaSpectatorItems(Player)} — removes every
 * arena-spectator-tagged item from the player's inventory; call this when a
 * player leaves spectator mode to prevent spectator-only items from being used
 * by alive players.</li>
 * </ul>
 * <em>Note: All factory methods return a new item instance; they never return
 * null or reuse an existing instance.</em>
 */
public final class ShroudedItems {

    private static final Logger LOGGER = JavaPlugin.getPlugin(TheShrouded.class)
            .getLogger();

    // -------------------------------------------------------------------------
    // NBT keys (namespace "shrouded" is owned by this plugin)
    // -------------------------------------------------------------------------

    /**
     * Marker present on every item the plugin creates. Value is always
     * {@code (byte) 1}. Useful for a fast "is this ours?" check without
     * inspecting the item type.
     */
    public static final NamespacedKey IS_SHROUDED_ITEM = new NamespacedKey(
            "shrouded", "is_shrouded_item");

    /**
     * Identifies the logical role of an item created by this plugin.
     *
     * @see #TYPE_CLASS_SELECTOR
     */
    public static final NamespacedKey ITEM_TYPE = new NamespacedKey("shrouded",
            "item_type");

    /**
     * Tag for location exclusive items to make cleaning inventories after
     * leaving a specific region easier.
     */
    public static final NamespacedKey LOCATION_TAG = new NamespacedKey(
            "shrouded", "location_tag");

    /**
     * Identifies which {@link zyx.araxia.shrouded.game.PlayerClass} an item
     * belongs to. Value matches the lower-case enum constant name, e.g.
     * {@value Shrouded#CLASS_VALUE} or {@value Survivor#CLASS_VALUE}.
     */
    public static final NamespacedKey CLASS_TAG = new NamespacedKey("shrouded",
            "class_tag");

    // -------------------------------------------------------------------------
    // item_type values
    // -------------------------------------------------------------------------

    /** Value of {@link #ITEM_TYPE} for the class-selector item. */
    public static final String TYPE_CLASS_SELECTOR = "class_selector";

    /** Value of {@link #ITEM_TYPE} for the how-to-play guide book. */
    public static final String TYPE_HOW_TO_PLAY = "how_to_play";

    /** Value of {@link #ITEM_TYPE} for the return-to-lobby item. */
    public static final String TYPE_RETURN_TO_LOBBY = "return_to_lobby";

    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // location_tag values
    // -------------------------------------------------------------------------

    /** Values of {@link #LOCATION_TAG} for location-specific items */
    public static final String LOCATION_LOBBY = "lobby";
    public static final String LOCATION_ARENA_ALIVE = "arena_alive";
    public static final String LOCATION_ARENA_SPECTATOR = "arena_spectator";

    private ShroudedItems() {
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Stamps the three standard plugin tags onto {@code meta} in one call. Used
     * by every factory method to guarantee the global marker, item-type
     * identifier, and (where applicable) location / class tags are always
     * written together.
     *
     * @param meta        the {@link ItemMeta} to tag (mutated in place)
     * @param itemType    value for {@link #ITEM_TYPE}
     * @param locationTag value for {@link #LOCATION_TAG}, or {@code null} to
     *                    skip
     * @param classTag    value for {@link #CLASS_TAG}, or {@code null} to skip
     */
    static void tagItem(ItemMeta meta, String itemType, String locationTag,
            String classTag) {
        meta.getPersistentDataContainer().set(IS_SHROUDED_ITEM,
                PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(ITEM_TYPE,
                PersistentDataType.STRING, itemType);
        if (locationTag != null) {
            meta.getPersistentDataContainer().set(LOCATION_TAG,
                    PersistentDataType.STRING, locationTag);
        }
        if (classTag != null) {
            meta.getPersistentDataContainer().set(CLASS_TAG,
                    PersistentDataType.STRING, classTag);
        }
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates the class-selector item: a
     * {@link Material#RIB_ARMOR_TRIM_SMITHING_TEMPLATE} with display name,
     * lore, and the two plugin NBT tags.
     *
     * @return a new {@link ItemStack} that is never null
     */
    public static ItemStack createClassSelector() {
        LOGGER.log(Level.FINE, "[TheShrouded] Creating class selector item.",
                new Object[] {});
        try {
            ItemStack item = new ItemStack(
                    Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
            ItemMeta meta = item.getItemMeta();

            meta.displayName(
                    Component.text("Choose Your Class", NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false));

            meta.lore(List.of(
                    Component
                            .text("Right-click to open the",
                                    NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("class selection menu.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));

            tagItem(meta, TYPE_CLASS_SELECTOR, LOCATION_LOBBY, null);

            item.setItemMeta(meta);
            return item;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating class selector item: {0}",
                    e.getMessage());
            return new ItemStack(Material.COBBLESTONE);
        }
    }

    /**
     * Creates the how-to-play guide: a signed {@link Material#WRITTEN_BOOK}
     * pre-filled with gameplay instructions and tagged with the global plugin
     * NBT marker so it can be swept from inventories on session end.
     *
     * @return a new {@link ItemStack} that is never null
     */
    public static ItemStack createHowToPlayGuide() {
        LOGGER.log(Level.FINE, "[TheShrouded] Creating how-to-play guide item.",
                new Object[] {});
        try {
            ItemStack howToBookItem = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta bookMeta = (BookMeta) howToBookItem.getItemMeta();

            // Item display name (shown in inventory tooltip)
            bookMeta.displayName(
                    Component.text("How to Play", NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false));

            // Book title and author (shown on the book's cover and opening
            // screen)
            bookMeta.title(Component.text("How to Play", NamedTextColor.AQUA));
            bookMeta.author(
                    Component.text("The Shrouded", NamedTextColor.GRAY));

            // Pages — add content here
            bookMeta.addPages(
                    // Page 1 – overview
                    Component.text("Welcome to The Shrouded!\n\n")
                            .append(Component
                                    .text("""
                                            One player is chosen to be the Shrouded. The others must either survive until time runs out or defeat the Shrouded to win!

                                            """)),

                    // Page 2 – how to pick a class
                    Component.text("Choosing a Class\n\n").append(Component
                            .text("""
                                    Right-click the armor template item in your inventory to open the class selection menu.

                                    Pick a class that suits your playstyle!
                                    """)));
            // ToDo: Finish How to Play guide content once game is fleshed out
            // more

            tagItem(bookMeta, TYPE_HOW_TO_PLAY, LOCATION_LOBBY, null);

            howToBookItem.setItemMeta(bookMeta);
            return howToBookItem;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Error creating how-to-play guide item: {0}",
                    e.getMessage());
            return new ItemStack(Material.COBBLESTONE);
        }
    }

    /**
     * Creates the return-to-lobby item: a {@link Material#PAPER} item tagged
     * with {@link #LOCATION_ARENA_ALIVE} so it is only present while the player
     * is alive in the arena and is swept on death / session end.
     *
     * @return a new {@link ItemStack} that is never null
     */
    public static ItemStack createReturnToLobby() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(
                Component.text("Return to Lobby", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
        meta.itemName(Component.text("Return to Lobby", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        tagItem(meta, TYPE_RETURN_TO_LOBBY, LOCATION_ARENA_ALIVE, null);
        item.setItemMeta(meta);
        return item;
    }

    // -------------------------------------------------------------------------
    // Inspection helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the given stack carries the
     * {@link #IS_SHROUDED_ITEM} marker, meaning it was created by this plugin.
     */
    public static boolean isShroudedItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .has(IS_SHROUDED_ITEM, PersistentDataType.BYTE);
    }

    /**
     * Returns {@code true} if the given stack is the class-selector item.
     */
    public static boolean isClassSelector(ItemStack item) {
        if (!isShroudedItem(item)) {
            return false;
        }
        String type = item.getItemMeta().getPersistentDataContainer()
                .get(ITEM_TYPE, PersistentDataType.STRING);
        return TYPE_CLASS_SELECTOR.equals(type);
    }

    public static boolean isLobbyItem(ItemStack item) {
        if (!isShroudedItem(item)) {
            return false;
        }
        String locationTag = item.getItemMeta().getPersistentDataContainer()
                .get(LOCATION_TAG, PersistentDataType.STRING);
        return LOCATION_LOBBY.equals(locationTag);
    }

    public static boolean isArenaAliveItem(ItemStack item) {
        if (!isShroudedItem(item)) {
            return false;
        }
        String locationTag = item.getItemMeta().getPersistentDataContainer()
                .get(LOCATION_TAG, PersistentDataType.STRING);
        return LOCATION_ARENA_ALIVE.equals(locationTag);
    }

    public static boolean isArenaSpectatorItem(ItemStack item) {
        if (!isShroudedItem(item)) {
            return false;
        }
        String locationTag = item.getItemMeta().getPersistentDataContainer()
                .get(LOCATION_TAG, PersistentDataType.STRING);
        return LOCATION_ARENA_SPECTATOR.equals(locationTag);
    }

    // -------------------------------------------------------------------------
    // Inventory utilities
    // -------------------------------------------------------------------------

    /**
     * Removes every plugin-tagged item from the player's inventory (all 36 main
     * slots, armour slots, and off-hand). Call this when a player leaves a
     * session to prevent lobby items from escaping into the world.
     *
     * @param player the online player whose inventory should be swept
     */
    public static void removeShroudedItems(Player player) {
        PlayerInventory inv = player.getInventory();

        // Main inventory + hotbar (slots 0–35)
        for (int i = 0; i < inv.getSize(); i++) {
            if (isShroudedItem(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }

        // Armour
        if (isShroudedItem(inv.getHelmet()))
            inv.setHelmet(null);
        if (isShroudedItem(inv.getChestplate()))
            inv.setChestplate(null);
        if (isShroudedItem(inv.getLeggings()))
            inv.setLeggings(null);
        if (isShroudedItem(inv.getBoots()))
            inv.setBoots(null);

        // Off-hand
        if (isShroudedItem(inv.getItemInOffHand())) {
            inv.setItemInOffHand(null);
        }
    }

    public static void removeLobbyItems(Player player) {
        PlayerInventory inv = player.getInventory();

        // Main inventory + hotbar (slots 0–35)
        for (int i = 0; i < inv.getSize(); i++) {
            if (isLobbyItem(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }

        // Armour
        if (isLobbyItem(inv.getHelmet()))
            inv.setHelmet(null);
        if (isLobbyItem(inv.getChestplate()))
            inv.setChestplate(null);
        if (isLobbyItem(inv.getLeggings()))
            inv.setLeggings(null);
        if (isLobbyItem(inv.getBoots()))
            inv.setBoots(null);

        // Off-hand
        if (isLobbyItem(inv.getItemInOffHand())) {
            inv.setItemInOffHand(null);
        }
    }

    public static void removeArenaAliveItems(Player player) {
        PlayerInventory inv = player.getInventory();

        // Main inventory + hotbar (slots 0–35)
        for (int i = 0; i < inv.getSize(); i++) {
            if (isArenaAliveItem(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }

        // Armour
        if (isArenaAliveItem(inv.getHelmet()))
            inv.setHelmet(null);
        if (isArenaAliveItem(inv.getChestplate()))
            inv.setChestplate(null);
        if (isArenaAliveItem(inv.getLeggings()))
            inv.setLeggings(null);
        if (isArenaAliveItem(inv.getBoots()))
            inv.setBoots(null);

        // Off-hand
        if (isArenaAliveItem(inv.getItemInOffHand())) {
            inv.setItemInOffHand(null);
        }
    }

    public static void removeArenaSpectatorItems(Player player) {
        PlayerInventory inv = player.getInventory();

        // Main inventory + hotbar (slots 0–35)
        for (int i = 0; i < inv.getSize(); i++) {
            if (isArenaSpectatorItem(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }

        // Armour
        if (isArenaSpectatorItem(inv.getHelmet()))
            inv.setHelmet(null);
        if (isArenaSpectatorItem(inv.getChestplate()))
            inv.setChestplate(null);
        if (isArenaSpectatorItem(inv.getLeggings()))
            inv.setLeggings(null);
        if (isArenaSpectatorItem(inv.getBoots()))
            inv.setBoots(null);

        // Off-hand
        if (isArenaSpectatorItem(inv.getItemInOffHand())) {
            inv.setItemInOffHand(null);
        }
    }

    // -------------------------------------------------------------------------
    // Package-private sweep helper (used by ShroudedClassItems,
    // SurvivorClassItems,
    // etc.)
    // -------------------------------------------------------------------------

    /**
     * Removes every item from {@code player}'s inventory (main, armour,
     * off-hand) that satisfies {@code predicate}.
     */
    static void removeItemsMatching(Player player,
            java.util.function.Predicate<ItemStack> predicate) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (predicate.test(inv.getItem(i)))
                inv.setItem(i, null);
        }
        if (predicate.test(inv.getHelmet()))
            inv.setHelmet(null);
        if (predicate.test(inv.getChestplate()))
            inv.setChestplate(null);
        if (predicate.test(inv.getLeggings()))
            inv.setLeggings(null);
        if (predicate.test(inv.getBoots()))
            inv.setBoots(null);
        if (predicate.test(inv.getItemInOffHand()))
            inv.setItemInOffHand(null);
    }
}
