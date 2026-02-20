package zyx.araxia.shrouded.lobby;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import zyx.araxia.shrouded.TheShrouded;

/**
 * Immutable snapshot of a player's state (world, location, inventory,
 * equipment, XP, and active potion effects) captured at the moment they join
 * a lobby session, before they are teleported.
 *
 * <p>
 * All {@link ItemStack} data is encoded as Base64 strings using Bukkit's
 * own serialisation layer so that the snapshot round-trips safely through
 * Gson without losing NBT data.
 */
public class PlayerSnapshot {

    private static final Logger LOGGER =
            JavaPlugin.getPlugin(TheShrouded.class).getLogger();

    // -------------------------------------------------------------------------
    // Fields – serialised by Gson
    // -------------------------------------------------------------------------

    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    /**
     * Base64-encoded {@link ItemStack} for each of the 36 main inventory slots
     * (index 0–35). Null entries represent empty slots.
     */
    private final String[] inventoryContents;

    // Armour slots and hands
    private final String helmetB64;
    private final String chestplateB64;
    private final String leggingsB64;
    private final String bootsB64;
    private final String mainHandB64;
    private final String offHandB64;

    private final int xpLevel;
    private final float xpProgress;

    private final List<PotionEffectData> potionEffects;

    // -------------------------------------------------------------------------
    // Inner DTO – potion effect
    // -------------------------------------------------------------------------

    /** Serialisable representation of a single active {@link PotionEffect}. */
    public static class PotionEffectData {
        private final String key; // NamespacedKey string, e.g. "minecraft:speed"
        private final int duration;
        private final int amplifier;
        private final boolean ambient;
        private final boolean particles;
        private final boolean icon;

        public PotionEffectData(PotionEffect effect) {
            this.key = effect.getType().getKey().toString();
            this.duration = effect.getDuration();
            this.amplifier = effect.getAmplifier();
            this.ambient = effect.isAmbient();
            this.particles = effect.hasParticles();
            this.icon = effect.hasIcon();
        }

        /**
         * Rebuilds the Bukkit {@link PotionEffect}, or returns {@code null} if
         * the effect type is no longer registered.
         */
        public PotionEffect toBukkit() {
            NamespacedKey nsKey = NamespacedKey.fromString(key);
            if (nsKey == null)
                return null;
            PotionEffectType type = Registry.EFFECT.get(nsKey);
            if (type == null)
                return null;
            return new PotionEffect(type, duration, amplifier, ambient, particles, icon);
        }
    }

    // -------------------------------------------------------------------------
    // Private constructor – use capture()
    // -------------------------------------------------------------------------

    private PlayerSnapshot(
            String worldName,
            double x, double y, double z,
            float yaw, float pitch,
            String[] inventoryContents,
            String helmetB64, String chestplateB64,
            String leggingsB64, String bootsB64,
            String mainHandB64, String offHandB64,
            int xpLevel, float xpProgress,
            List<PotionEffectData> potionEffects) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.inventoryContents = inventoryContents;
        this.helmetB64 = helmetB64;
        this.chestplateB64 = chestplateB64;
        this.leggingsB64 = leggingsB64;
        this.bootsB64 = bootsB64;
        this.mainHandB64 = mainHandB64;
        this.offHandB64 = offHandB64;
        this.xpLevel = xpLevel;
        this.xpProgress = xpProgress;
        this.potionEffects = potionEffects;
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Captures the current state of {@code player} and returns it as a
     * {@link PlayerSnapshot}.
     *
     * @param player the online player to snapshot
     * @return a new snapshot
     */
    public static PlayerSnapshot capture(Player player) {
        LOGGER.log(Level.FINE, "Capturing snapshot of player {0} ({1})",
                new Object[] { player.getName(), player.getUniqueId() });
        player.closeInventory(); // flushes crafting grid → inventory/floor
        Location loc = player.getLocation();
        PlayerInventory inv = player.getInventory();

        // Main inventory (hotbar + storage = slots 0–35)
        ItemStack[] contents = inv.getContents();
        if (contents == null) {
            contents = new ItemStack[36]; // should never happen, but just in case
        }

        String[] encoded = new String[contents.length];
        for (int i = 0; i < contents.length; i++) {
            encoded[i] = itemToBase64(contents[i]);
        }

        // Armour and off-hand
        String helmetB64 = itemToBase64(inv.getHelmet());
        String chestplateB64 = itemToBase64(inv.getChestplate());
        String leggingsB64 = itemToBase64(inv.getLeggings());
        String bootsB64 = itemToBase64(inv.getBoots());
        String mainHandB64 = itemToBase64(inv.getItemInMainHand());
        String offHandB64 = itemToBase64(inv.getItemInOffHand());

        // Active potion effects
        Collection<PotionEffect> effects = player.getActivePotionEffects();
        List<PotionEffectData> effectData = new ArrayList<>(effects.size());
        for (PotionEffect e : effects) {
            effectData.add(new PotionEffectData(e));
        }

        if (loc == null) {
            // Should never happen, but just in case
            loc = new Location(Bukkit.getWorlds().get(0), 0, 64, 0);
        }

        return new PlayerSnapshot(
                loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch(),
                encoded,
                helmetB64, chestplateB64, leggingsB64, bootsB64,
                mainHandB64, offHandB64,
                player.getLevel(), player.getExp(),
                effectData);
    }

    // -------------------------------------------------------------------------
    // Restore
    // -------------------------------------------------------------------------

    /**
     * Restores this snapshot onto {@code player}: teleports them to the saved
     * location, restores inventory, equipment, XP, and potion effects.
     *
     * @param player the online player to restore
     */
    public void restoreTo(Player player) {
        LOGGER.log(Level.INFO, "Restoring snapshot for player {0} ({1})",
                new Object[] { player.getName(), player.getUniqueId() });
        // Flush any items the player may have stashed in the crafting grid
        // during the session so they cannot smuggle lobby items out.
        player.closeInventory();

        // Teleport
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world != null) {
            player.teleport(new Location(world, x, y, z, yaw, pitch));
        }

        PlayerInventory inv = player.getInventory();
        inv.clear();

        // Main inventory
        for (int i = 0; i < inventoryContents.length; i++) {
            ItemStack item = base64ToItem(inventoryContents[i]);
            if (item != null) {
                inv.setItem(i, item);
            }
        }

        // Armour and off-hand
        inv.setHelmet(base64ToItem(helmetB64));
        inv.setChestplate(base64ToItem(chestplateB64));
        inv.setLeggings(base64ToItem(leggingsB64));
        inv.setBoots(base64ToItem(bootsB64));
        inv.setItemInMainHand(base64ToItem(mainHandB64));
        inv.setItemInOffHand(base64ToItem(offHandB64));

        // XP
        player.setLevel(xpLevel);
        player.setExp(xpProgress);

        // Potion effects – clear first, then re-apply
        for (PotionEffect active : player.getActivePotionEffects()) {
            player.removePotionEffect(active.getType());
        }
        for (PotionEffectData data : potionEffects) {
            PotionEffect effect = data.toBukkit();
            if (effect != null) {
                player.addPotionEffect(effect);
            } else {
                LOGGER.log(Level.WARNING,
                        "[TheShrouded] Could not restore potion effect ''{0}'' for {1} — type not registered.",
                        new Object[] { data.key, player.getName() });
            }
        }
        LOGGER.log(Level.INFO, "Snapshot restore complete for player {0} ({1})",
                new Object[] { player.getName(), player.getUniqueId() });
    }

    // -------------------------------------------------------------------------
    // Serialisation helpers
    // -------------------------------------------------------------------------

    /**
     * Encodes an {@link ItemStack} to a Base64 string using
     * {@link ItemStack#serializeAsBytes()}; returns {@code null} for null or
     * air stacks.
     */
    private static String itemToBase64(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    /**
     * Decodes a Base64 string back into an {@link ItemStack} using
     * {@link ItemStack#deserializeBytes(byte[])}; returns {@code null} for a
     * null input.
     */
    private static ItemStack base64ToItem(String base64) {
        if (base64 == null) {
            return null;
        }
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(base64));
    }
}
