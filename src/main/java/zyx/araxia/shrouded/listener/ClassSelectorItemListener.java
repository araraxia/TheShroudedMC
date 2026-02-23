package zyx.araxia.shrouded.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import zyx.araxia.shrouded.item.ShroudedItems;
import zyx.araxia.shrouded.lobby.LobbyManager;
import zyx.araxia.shrouded.menu.ClassSelectMenu;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import zyx.araxia.shrouded.TheShrouded;

/**
 * Listens for right-click interactions with the class-selector item and opens
 * the {@link ClassSelectMenu} for the player.
 *
 * <p>Paper fires {@link PlayerInteractEvent} twice for each physical click
 * (once for {@link EquipmentSlot#HAND} and once for
 * {@link EquipmentSlot#OFF_HAND}). We filter to {@code HAND} only to avoid
 * opening the menu twice per click.
 */
public class ClassSelectorItemListener implements Listener {

    private static final Logger LOGGER = JavaPlugin.getPlugin(TheShrouded.class).getLogger();
    private final LobbyManager lobbyManager;

    public ClassSelectorItemListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only react to right-click (air or block), but not left-click
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Deduplicate — ignore the off-hand duplicate event Paper fires
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();

        if (!ShroudedItems.isClassSelector(event.getItem())) {
            return;
        }
        
        LOGGER.log(Level.FINE, "Player {0} ({1}) interacted with class selector item.",
                new Object[] { player.getName(), player.getUniqueId() });

        // Only let the player open the menu if they are inside a lobby session
        if (!lobbyManager.isPlayerInSession(player)) {
            LOGGER.log(Level.WARNING, "Player {0} ({1}) attempted to use class selector item outside of a lobby session.",
                    new Object[] { player.getName(), player.getUniqueId() });
            return;
            // TODO: Run player sweep to remove any items they shouldn't have if they're outside a session but still have the class selector item for some reason
        }

        event.setCancelled(true);
        ClassSelectMenu.open(player);
    }
}
