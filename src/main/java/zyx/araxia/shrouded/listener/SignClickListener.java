package zyx.araxia.shrouded.listener;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import zyx.araxia.shrouded.lobby.Lobby;
import zyx.araxia.shrouded.lobby.LobbyManager;
import zyx.araxia.shrouded.lobby.LobbySession;
import zyx.araxia.shrouded.menu.ClassSelectMenu;

public class SignClickListener implements Listener {

    private final LobbyManager lobbyManager;

    public SignClickListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign)) return;

        LobbySession session = lobbyManager.getSessionBySign(
                block.getWorld().getName(), block.getX(), block.getY(), block.getZ());

        if (session == null) return; // Not a registered lobby sign

        event.setCancelled(true); // Prevent sign editing

        Player player = event.getPlayer();
        Lobby lobby = session.getLobby();

        if (session.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You are already in lobby '" + lobby.getName() + "'.");
            return;
        }

        if (session.isFull()) {
            player.sendMessage(ChatColor.RED + "Lobby '" + lobby.getName() + "' is full ("
                    + lobby.getMaxPlayers() + "/" + lobby.getMaxPlayers() + ").");
            return;
        }

        // Teleport to lobby spawn (center of the region)
        player.teleport(lobby.getSpawnLocation(block.getWorld()));

        session.add(player);
        player.sendMessage(ChatColor.GREEN + "You joined lobby '" + lobby.getName() + "' ("
                + session.getPlayerCount() + "/" + lobby.getMaxPlayers() + ").");

        // Open class selection menu
        ClassSelectMenu.open(player);
    }
}
