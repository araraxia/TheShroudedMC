package zyx.araxia.shrouded.item;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import zyx.araxia.shrouded.TheShrouded;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Factory and helpers for items exclusive to the
 * {@link zyx.araxia.shrouded.game.PlayerClass#SURVIVOR SURVIVOR} class.
 * <p>
 * Every item created here should be tagged via {@link ShroudedItems#tagItem}
 * with:
 * <ul>
 * <li>{@link ShroudedItems#IS_SHROUDED_ITEM} = 1</li>
 * <li>{@link ShroudedItems#CLASS_TAG} = {@value #CLASS_VALUE}</li>
 * <li>{@link ShroudedItems#LOCATION_TAG} =
 * {@link ShroudedItems#LOCATION_ARENA_ALIVE}</li>
 * </ul>
 */
public final class SurvivorClassItems {

        /**
         * Value written to {@link ShroudedItems#CLASS_TAG} for every item in
         * this class.
         */
        public static final String CLASS_VALUE = "survivor";

        // -------------------------------------------------------------------------
        // item_type values
        // -------------------------------------------------------------------------
        // Example: public static final String TYPE_TRACKER_COMPASS =
        // "survivor_tracker_compass";
        public static final String TYPE_SURVIVOR_IRON_SWORD = "survivor_iron_sword";
        public static final String TYPE_SURVIVOR_HEALTH_SPLASH_POTION_1 = "survivor_health_splash_potion_1";
        public static final String TYPE_SURVIVOR_BOMB = "survivor_bomb";
        public static final String TYPE_SURVIVOR_WEB = "survivor_web";
        public static final String TYPE_SURVIVOR_WIND_CHARGE = "survivor_wind_charge";
        public static final String TYPE_SURVIVOR_CHAIN_HELMET = "survivor_chain_helmet";
        public static final String TYPE_SURVIVOR_CHAIN_CHESTPLATE = "survivor_chain_chestplate";
        public static final String TYPE_SURVIVOR_CHAIN_LEGGINGS = "survivor_chain_leggings";
        public static final String TYPE_SURVIVOR_CHAIN_BOOTS = "survivor_chain_boots";

        private SurvivorClassItems() {
        }

        // -------------------------------------------------------------------------
        // Factory methods
        // -------------------------------------------------------------------------

        public static ItemStack createSurvivorIronSword() {
                JavaPlugin plugin = JavaPlugin.getPlugin(TheShrouded.class);
                double damageMultiplier = plugin.getConfig().getDouble(
                                "survivor.sword-damage-multiplier", 1.0);

                double swingCooldownSeconds = plugin.getConfig().getDouble(
                                "survivor.sword-swing-cooldown-seconds", 0.625);
                                
                ItemStack item = new ItemStack(org.bukkit.Material.IRON_SWORD);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component
                                .text("Survivor's Sword", NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false));
                meta.itemName(Component
                                .text("Survivor's Sword", NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false));
                meta.setUnbreakable(true);

                // Scale attack damage: MULTIPLY_SCALAR_1 adds base *
                // (multiplier - 1.0)
                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                                new AttributeModifier(new NamespacedKey(
                                                "shrouded",
                                                "survivor_sword_damage"),
                                                damageMultiplier - 1.0,
                                                AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                                                EquipmentSlotGroup.MAINHAND));

                // Set attack speed so that cooldown = swingCooldownSeconds.
                // Iron sword applies its own -2.4 ADD_NUMBER modifier on
                // ATTACK_SPEED
                // (base 4.0), yielding 1.6 attacks/sec. Our modifier adjusts
                // from that default:
                // desired speed = 1.0 / swingCooldownSeconds
                // our modifier = desired - 1.6
                double desiredSpeed = 1.0 / swingCooldownSeconds;
                meta.addAttributeModifier(Attribute.ATTACK_SPEED,
                                new AttributeModifier(new NamespacedKey(
                                                "shrouded",
                                                "survivor_sword_speed"),
                                                desiredSpeed - 1.6,
                                                AttributeModifier.Operation.ADD_NUMBER,
                                                EquipmentSlotGroup.MAINHAND));

                ShroudedItems.tagItem(meta, TYPE_SURVIVOR_IRON_SWORD,
                                ShroudedItems.LOCATION_ARENA_ALIVE,
                                CLASS_VALUE);
                item.setItemMeta(meta);
                return item;
        }

        public static ItemStack createSurvivorChainHelmet() {
                ItemStack item = new ItemStack(
                                org.bukkit.Material.CHAINMAIL_HELMET);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component
                                .text("Survivor's Helmet", NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false));
                meta.itemName(Component
                                .text("Survivor's Helmet", NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false));
                meta.setUnbreakable(true);
                ShroudedItems.tagItem(meta, TYPE_SURVIVOR_CHAIN_HELMET,
                                ShroudedItems.LOCATION_ARENA_ALIVE,
                                CLASS_VALUE);
                item.setItemMeta(meta);
                return item;
        }

        public static ItemStack createSurvivorChainChestplate() {
                ItemStack item = new ItemStack(
                                org.bukkit.Material.CHAINMAIL_CHESTPLATE);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component
                                .text("Survivor's Chestplate",
                                                NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false));
                meta.itemName(Component
                                .text("Survivor's Chestplate",
                                                NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false));
                meta.setUnbreakable(true);
                ShroudedItems.tagItem(meta, TYPE_SURVIVOR_CHAIN_CHESTPLATE,
                                ShroudedItems.LOCATION_ARENA_ALIVE,
                                CLASS_VALUE);
                item.setItemMeta(meta);
                return item;
        }

        public static ItemStack createSurvivorChainLeggings() {
                ItemStack item = new ItemStack(
                                org.bukkit.Material.CHAINMAIL_LEGGINGS);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component
                                .text("Survivor's Leggings",
                                                NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false));
                meta.itemName(Component
                                .text("Survivor's Leggings",
                                                NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false));
                meta.setUnbreakable(true);
                ShroudedItems.tagItem(meta, TYPE_SURVIVOR_CHAIN_LEGGINGS,
                                ShroudedItems.LOCATION_ARENA_ALIVE,
                                CLASS_VALUE);
                item.setItemMeta(meta);
                return item;
        }

        public static ItemStack createSurvivorChainBoots() {
                ItemStack item = new ItemStack(
                                org.bukkit.Material.CHAINMAIL_BOOTS);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component
                                .text("Survivor's Boots", NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false));
                meta.itemName(Component
                                .text("Survivor's Boots", NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false));
                meta.setUnbreakable(true);
                ShroudedItems.tagItem(meta, TYPE_SURVIVOR_CHAIN_BOOTS,
                                ShroudedItems.LOCATION_ARENA_ALIVE,
                                CLASS_VALUE);
                item.setItemMeta(meta);
                return item;
        }

        public static ItemStack createSurvivorHealthSplashPotion1() {
                JavaPlugin plugin = JavaPlugin.getPlugin(TheShrouded.class);

                int healLevel = plugin.getConfig().getInt(
                                "survivor.health-potion-heal-level", 1);

                int stackSize = plugin.getConfig().getInt(
                                "survivor.health-potion-stack-size", 1);
                                
                ItemStack item = new ItemStack(
                                org.bukkit.Material.SPLASH_POTION);
                PotionMeta meta = (PotionMeta) item.getItemMeta();
                meta.displayName(Component
                                .text("Survivor's Health Potion",
                                                NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false));
                meta.itemName(Component
                                .text("Survivor's Health Potion",
                                                NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false));
                // amplifier is 0-indexed (0 = level 1)
                meta.addCustomEffect(new PotionEffect(
                                PotionEffectType.INSTANT_HEALTH, 1,
                                healLevel - 1), true);
                ShroudedItems.tagItem(meta,
                                TYPE_SURVIVOR_HEALTH_SPLASH_POTION_1,
                                ShroudedItems.LOCATION_ARENA_ALIVE,
                                CLASS_VALUE);
                item.setItemMeta(meta);
                item.setAmount(stackSize);
                return item;
        }

        public static ItemStack createSurvivorBomb() {
                int stackSize = JavaPlugin.getPlugin(TheShrouded.class).getConfig()
                                .getInt("survivor.bomb-stack-size", 1);
                ItemStack item = new ItemStack(org.bukkit.Material.PITCHER_POD);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component
                                .text("Impact Bomb", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                meta.itemName(Component.text("Impact Bomb", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                // Use the custom resource pack model (pitcher_pod rotated
                // upside down)
                meta.setItemModel(
                                new NamespacedKey("shrouded", "survivor_bomb"));
                ShroudedItems.tagItem(meta, TYPE_SURVIVOR_BOMB,
                                ShroudedItems.LOCATION_ARENA_ALIVE,
                                CLASS_VALUE);
                item.setItemMeta(meta);
                item.setAmount(stackSize);
                return item;
        }

        public static ItemStack createSurvivorWeb() {
                int stackSize = JavaPlugin.getPlugin(TheShrouded.class).getConfig()
                                .getInt("survivor.web-stack-size", 4);
                ItemStack item = new ItemStack(org.bukkit.Material.COBWEB);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component
                                .text("Web Grenade", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                meta.itemName(Component.text("Web Grenade", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                ShroudedItems.tagItem(meta, TYPE_SURVIVOR_WEB,
                                ShroudedItems.LOCATION_ARENA_ALIVE,
                                CLASS_VALUE);
                item.setItemMeta(meta);
                item.setAmount(stackSize);
                return item;
        }

        public static ItemStack createSurvivorWindCharge() {
                int stackSize = JavaPlugin.getPlugin(TheShrouded.class).getConfig()
                                .getInt("survivor.wind-charge-stack-size", 2);
                ItemStack item = new ItemStack(org.bukkit.Material.WIND_CHARGE);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component
                                .text("Wind Charge", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                meta.itemName(Component.text("Wind Charge", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                ShroudedItems.tagItem(meta, TYPE_SURVIVOR_WIND_CHARGE,
                                ShroudedItems.LOCATION_ARENA_ALIVE,
                                CLASS_VALUE);
                item.setItemMeta(meta);
                item.setAmount(stackSize);
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
                String tag = item.getItemMeta().getPersistentDataContainer()
                                .get(ShroudedItems.CLASS_TAG,
                                                PersistentDataType.STRING);
                return CLASS_VALUE.equals(tag);
        }

        /**
         * Removes every Survivor-class-tagged item from the player's inventory.
         */
        public static void removeItems(Player player) {
                ShroudedItems.removeItemsMatching(player,
                                SurvivorClassItems::isItem);
        }
}
