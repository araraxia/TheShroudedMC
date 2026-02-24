package zyx.araxia.shrouded.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import zyx.araxia.shrouded.lobby.Arena;
import zyx.araxia.shrouded.menu.ArenaVoteMenuHolder;

/**
 * Handles clicks inside the {@link zyx.araxia.shrouded.menu.ArenaVoteMenu}.
 */
public class ArenaVoteMenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ArenaVoteMenuHolder holder))
            return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player))
            return;

        Arena arena = holder.getArenaAt(event.getSlot());
        if (arena == null)
            return;

        holder.getSession().recordVote(player.getUniqueId(), arena);
        player.closeInventory();
        player.sendMessage(Component.text("You voted for arena '", NamedTextColor.GREEN)
                .append(Component.text(arena.getName(), NamedTextColor.AQUA))
                .append(Component.text("'.", NamedTextColor.GREEN)));
    }
}
