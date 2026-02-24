package zyx.araxia.shrouded.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import zyx.araxia.shrouded.item.ShroudedItems;
import zyx.araxia.shrouded.item.SurvivorClassItems;

/**
 * Handles right-click use of the
 * {@link SurvivorClassItems#TYPE_SURVIVOR_WIND_CHARGE Survivor Wind Charge}.
 * <p>
 * On right-click, a vanilla {@link WindCharge} is launched in the player's
 * look direction. The last item in the stack is never consumed — it is instead
 * put on a cooldown from config ({@code survivor.wind-charge-cooldown-seconds}).
 */
public class SurvivorWindChargeListener implements Listener {

    private final JavaPlugin plugin;

    /** Tracks when each player's wind-charge cooldown expires (epoch milliseconds). */
    private final Map<UUID, Long> cooldownExpiry = new HashMap<>();

    public SurvivorWindChargeListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK)
            return;

        // Only handle the main hand to avoid firing twice
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        ItemStack item = event.getItem();
        if (item == null || !ShroudedItems.isShroudedItem(item))
            return;

        String type = item.getItemMeta().getPersistentDataContainer()
                .get(ShroudedItems.ITEM_TYPE, PersistentDataType.STRING);
        if (!SurvivorClassItems.TYPE_SURVIVOR_WIND_CHARGE.equals(type))
            return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long expiry = cooldownExpiry.getOrDefault(player.getUniqueId(), 0L);

        if (now < expiry) {
            long remaining = (expiry - now + 999) / 1000;
            player.sendActionBar(Component.text(
                    "Survivor Wind Charge on cooldown: " + remaining + "s remaining",
                    NamedTextColor.RED));
            return;
        }

        // Read launch speed from config at call time
        double launchSpeed = plugin.getConfig()
                .getDouble("survivor.wind-charge-speed", 1.5);

        // Spawn a vanilla WindCharge in the player's look direction
        Vector velocity = player.getLocation().getDirection().multiply(launchSpeed);
        player.getWorld().spawn(player.getEyeLocation(), WindCharge.class, charge -> {
            charge.setShooter(player);
            charge.setVelocity(velocity);
        });

        // Read cooldown from config at call time so reloads take effect
        double cooldownSeconds = plugin.getConfig()
                .getDouble("survivor.wind-charge-cooldown-seconds", 30.0);
        int cooldownTicks = (int) (cooldownSeconds * 20);
        long cooldownMillis = (long) (cooldownSeconds * 1000);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            cooldownExpiry.put(player.getUniqueId(), now + cooldownMillis);
            player.setCooldown(Material.WIND_CHARGE, cooldownTicks);
        }
    }
}
