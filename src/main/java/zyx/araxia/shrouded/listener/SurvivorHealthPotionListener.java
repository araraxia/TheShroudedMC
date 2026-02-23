package zyx.araxia.shrouded.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import zyx.araxia.shrouded.item.ShroudedItems;
import zyx.araxia.shrouded.item.SurvivorClassItems;

/**
 * Prevents the {@link SurvivorClassItems#TYPE_SURVIVOR_HEALTH_SPLASH_POTION_1}
 * from being consumed on use. Instead it launches a {@link ThrownPotion} entity
 * and places the item on a configurable cooldown.
 */
public class SurvivorHealthPotionListener implements Listener {

    /** Cooldown duration in milliseconds, loaded from config.yml at startup. */
    private final long cooldownMillis;

    /** Cooldown duration in ticks, used for the HUD indicator effect. */
    private final int cooldownTicks;

    /** Tracks when each player's cooldown expires (epoch milliseconds). */
    private final Map<UUID, Long> cooldownExpiry = new HashMap<>();

    public SurvivorHealthPotionListener(int cooldownTicks) {
        this.cooldownMillis = cooldownTicks * 50L; // 1 tick = 50 ms
        this.cooldownTicks = cooldownTicks;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();
        if (item == null || !ShroudedItems.isShroudedItem(item))
            return;

        String type = item.getItemMeta().getPersistentDataContainer()
                .get(ShroudedItems.ITEM_TYPE, PersistentDataType.STRING);
        if (!SurvivorClassItems.TYPE_SURVIVOR_HEALTH_SPLASH_POTION_1
                .equals(type))
            return;

        // Always cancel to prevent vanilla consumption
        event.setCancelled(true);

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long expiry = cooldownExpiry.getOrDefault(player.getUniqueId(), 0L);

        // Do nothing if already on cooldown; inform the player
        if (now < expiry) {
            long remaining = (expiry - now + 999) / 1000; // ceiling seconds
            player.sendActionBar(Component.text(
                    "Health Potion on cooldown: " + remaining + "s remaining",
                    NamedTextColor.RED));
            return;
        }

        // Launch the potion entity carrying the same item (effects intact)
        ThrownPotion thrown = player.launchProjectile(ThrownPotion.class);
        thrown.setItem(item);

        // Record expiry for this player's health-potion cooldown specifically
        cooldownExpiry.put(player.getUniqueId(), now + cooldownMillis);

        // Apply a custom "Health Potion Cooldown" indicator effect so the player sees
        // a dedicated icon in the HUD for the cooldown duration.
        // Falls back to Absorption if the data pack hasn't been loaded yet
        // (only possible on the very first server start before a /reload).
        PotionEffectType cooldownEffect = Registry.EFFECT
                .get(new NamespacedKey("shrouded", "health_potion_cooldown"));
        if (cooldownEffect == null)
            cooldownEffect = PotionEffectType.ABSORPTION;
        player.addPotionEffect(
                new PotionEffect(cooldownEffect, cooldownTicks, 0, // amplifier
                                                                   // 0
                        false, // not ambient
                        false, // no particles
                        true // show HUD icon
                ));
    }
}
