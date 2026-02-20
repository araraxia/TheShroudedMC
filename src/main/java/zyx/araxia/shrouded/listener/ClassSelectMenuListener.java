package zyx.araxia.shrouded.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import zyx.araxia.shrouded.game.PlayerClass;
import zyx.araxia.shrouded.lobby.LobbyManager;
import zyx.araxia.shrouded.menu.ClassMenuHolder;

public class ClassSelectMenuListener implements Listener {

    private final LobbyManager lobbyManager;

    public ClassSelectMenuListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Only handle our class-select menu
        if (!(event.getInventory().getHolder() instanceof ClassMenuHolder)) return;

        event.setCancelled(true); // Prevent taking items from the menu

        if (!(event.getWhoClicked() instanceof Player player))
            return;

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null) return;
        
        // Match the clicked item material to a PlayerClass
        for (PlayerClass cls : PlayerClass.values()) {
            if (cls.getIcon() == currentItem.getType()) {
                lobbyManager.setPlayerClass(player, cls);
                player.closeInventory();
                player.sendMessage(
                        Component.text("You selected the ", NamedTextColor.GREEN)
                                .append(Component.text(cls.getDisplayName(), NamedTextColor.GOLD))
                                .append(Component.text(" class!", NamedTextColor.GREEN)));
                return;
            }
        }
    }
}
