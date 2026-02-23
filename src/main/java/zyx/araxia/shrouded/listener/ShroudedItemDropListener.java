package zyx.araxia.shrouded.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

import zyx.araxia.shrouded.item.ShroudedItems;

/**
 * Prevents players from dropping any item that carries the
 * {@link ShroudedItems#IS_SHROUDED_ITEM} marker tag.
 */
public class ShroudedItemDropListener implements Listener {

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (ShroudedItems.isShroudedItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }
}
