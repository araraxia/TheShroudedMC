package zyx.araxia.shrouded.item;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import zyx.araxia.shrouded.TheShrouded;

/**
 * Factory and helpers for items exclusive to the
 * {@link zyx.araxia.shrouded.game.PlayerClass#SHROUDED SHROUDED} class.
 *
 * <p>
 * Every item created here should be tagged via
 * {@link ShroudedItems#tagItem} with:
 * <ul>
 * <li>{@link ShroudedItems#IS_SHROUDED_ITEM} = 1</li>
 * <li>{@link ShroudedItems#CLASS_TAG} = {@value #CLASS_VALUE}</li>
 * <li>{@link ShroudedItems#LOCATION_TAG} =
 * {@link ShroudedItems#LOCATION_ARENA_ALIVE}</li>
 * </ul>
 */
public final class ShroudedClassItems {

    /**
     * Value written to {@link ShroudedItems#CLASS_TAG} for every item in this
     * class.
     */
    public static final String CLASS_VALUE = "shrouded";

    // -------------------------------------------------------------------------
    // item_type values
    // -------------------------------------------------------------------------
    // Example: public static final String TYPE_SMOKE_BOMB = "shrouded_smoke_bomb";

    public static final String TYPE_SHROUDED_IRON_SWORD = "shrouded_iron_sword";
    public static final String TYPE_LEVI_BOMB_CHORUS_FLOWER = "shrouded_levi_bomb_chorus_flower";
    public static final String TYPE_POISON_WAVE_WEATH_COP_LANTERN = "shrouded_poison_wave_weath_cop_lantern";
    public static final String TYPE_GLOBAL_BLIND_SCULK = "shrouded_blind_sculk";
    public static final String TYPE_LEAP_WOODEN_SPEAR = "shrouded_leap_spear";
    public static final String TYPE_LEAP_BOW_ARROW = "shrouded_leap_bow_arrow";

    public static final String TYPE_SHROUDED_NETH_HELM = "shrouded_neth_helm";
    public static final String TYPE_SHROUDED_NETH_BODY = "shrouded_neth_body";
    public static final String TYPE_SHROUDED_NETH_LEGS = "shrouded_neth_legs";
    public static final String TYPE_SHROUDED_NETH_BOOT = "shrouded_neth_boot";

    private ShroudedClassItems() {
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    public static ItemStack createShroudedIronSword() {
        JavaPlugin plugin = JavaPlugin.getPlugin(TheShrouded.class);
        double damageMultiplier = plugin.getConfig()
                .getDouble("shrouded-class.sword-damage-multiplier");
        double swingCooldownSeconds = plugin.getConfig()
                .getDouble("shrouded-class.sword-swing-cooldown-seconds");

        ItemStack item = new ItemStack(org.bukkit.Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Shrouded Blade", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.itemName(Component.text("Shrouded Blade", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.setUnbreakable(true);

        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                new AttributeModifier(
                        new NamespacedKey("shrouded", "shrouded_sword_damage"),
                        damageMultiplier - 1.0,
                        AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                        EquipmentSlotGroup.MAINHAND));

        double desiredSpeed = 1.0 / swingCooldownSeconds;
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                new NamespacedKey("shrouded", "shrouded_sword_speed"),
                desiredSpeed - 1.6, AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND));

        ShroudedItems.tagItem(meta, TYPE_SHROUDED_IRON_SWORD,
                ShroudedItems.LOCATION_ARENA_ALIVE, CLASS_VALUE);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createLeviBombChorusFlower() {
        ItemStack item = new ItemStack(org.bukkit.Material.CHORUS_FLOWER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Gravity Distortion", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.itemName(Component.text("Gravity Distortion", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.setUnbreakable(true);

        ShroudedItems.tagItem(meta, TYPE_LEVI_BOMB_CHORUS_FLOWER,
                ShroudedItems.LOCATION_ARENA_ALIVE, CLASS_VALUE);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createPoisonWaveWeathCopLantern() {
        ItemStack item = new ItemStack(org.bukkit.Material.WEATHERED_COPPER_LANTERN);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Toxic Cloud", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.itemName(Component.text("Toxic Cloud", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.setUnbreakable(true);

        ShroudedItems.tagItem(meta, TYPE_POISON_WAVE_WEATH_COP_LANTERN,
                ShroudedItems.LOCATION_ARENA_ALIVE, CLASS_VALUE);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createGlobalBlindSculk() {
            ItemStack item = new ItemStack(org.bukkit.Material.SCULK);
            ItemMeta meta = item.getItemMeta();

            meta.displayName(Component.text("Lights Out", NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false));
            meta.itemName(Component.text("Lights Out", NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false));
            meta.setUnbreakable(true);

            ShroudedItems.tagItem(meta, TYPE_GLOBAL_BLIND_SCULK,
                            ShroudedItems.LOCATION_ARENA_ALIVE, CLASS_VALUE);
            item.setItemMeta(meta);
            return item;
    }

    public static ItemStack createLeapWoodenSpear() {
        ItemStack item = new ItemStack(org.bukkit.Material.BOW);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Leap", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.itemName(Component.text("Leap", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.INFINITY, 1, true);

        ShroudedItems.tagItem(meta, TYPE_LEAP_WOODEN_SPEAR,
                ShroudedItems.LOCATION_ARENA_ALIVE, CLASS_VALUE);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the single dummy arrow held in the Shrouded player's inventory
     * so the INFINITY-enchanted leap bow can be drawn. Tagged as a Shrouded
     * item so it cannot be dropped.
     */
    public static ItemStack createLeapBowArrow() {
        ItemStack item = new ItemStack(org.bukkit.Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        ShroudedItems.tagItem(meta, TYPE_LEAP_BOW_ARROW,
                ShroudedItems.LOCATION_ARENA_ALIVE, CLASS_VALUE);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createShroudedNethHelmet() {
        ItemStack item = new ItemStack(org.bukkit.Material.NETHERITE_HELMET);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Shrouded Helmet", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.itemName(Component.text("Shrouded Helmet", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.setUnbreakable(true);

        ShroudedItems.tagItem(meta, TYPE_SHROUDED_NETH_HELM,
                ShroudedItems.LOCATION_ARENA_ALIVE, CLASS_VALUE);
        item.setItemMeta(meta);
        // No model key → armor texture is not rendered on the player
        item.setData(DataComponentTypes.EQUIPPABLE,
                Equippable.equippable(EquipmentSlot.HEAD).build());
        return item;
    }

    public static ItemStack createShroudedNethChestplate() {
        ItemStack item = new ItemStack(org.bukkit.Material.NETHERITE_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Shrouded Chestplate", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.itemName(Component.text("Shrouded Chestplate", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.setUnbreakable(true);

        ShroudedItems.tagItem(meta, TYPE_SHROUDED_NETH_BODY,
                ShroudedItems.LOCATION_ARENA_ALIVE, CLASS_VALUE);
        item.setItemMeta(meta);
        // No model key → armor texture is not rendered on the player
        item.setData(DataComponentTypes.EQUIPPABLE,
                Equippable.equippable(EquipmentSlot.CHEST).build());
        return item;
    }

    public static ItemStack createShroudedNethLeggings() {
        ItemStack item = new ItemStack(org.bukkit.Material.NETHERITE_LEGGINGS);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Shrouded Leggings", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.itemName(Component.text("Shrouded Leggings", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.setUnbreakable(true);

        ShroudedItems.tagItem(meta, TYPE_SHROUDED_NETH_LEGS,
                ShroudedItems.LOCATION_ARENA_ALIVE, CLASS_VALUE);
        item.setItemMeta(meta);
        // No model key → armor texture is not rendered on the player
        item.setData(DataComponentTypes.EQUIPPABLE,
                Equippable.equippable(EquipmentSlot.LEGS).build());
        return item;
    }

    public static ItemStack createShroudedNethBoots() {
        ItemStack item = new ItemStack(org.bukkit.Material.NETHERITE_BOOTS);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Shrouded Boots", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.itemName(Component.text("Shrouded Boots", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.setUnbreakable(true);

        ShroudedItems.tagItem(meta, TYPE_SHROUDED_NETH_BOOT,
                ShroudedItems.LOCATION_ARENA_ALIVE, CLASS_VALUE);
        item.setItemMeta(meta);
        // No model key → armor texture is not rendered on the player
        item.setData(DataComponentTypes.EQUIPPABLE,
                Equippable.equippable(EquipmentSlot.FEET).build());
        return item;
    }

    // -------------------------------------------------------------------------
    // Inspection helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if {@code item} carries the
     * {@link ShroudedItems#CLASS_TAG} value for this class.
     */
    public static boolean isItem(ItemStack item) {
        if (!ShroudedItems.isShroudedItem(item))
            return false;
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
