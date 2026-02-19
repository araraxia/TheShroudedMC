package zyx.araxia.shrouded.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import zyx.araxia.shrouded.lobby.LobbyManager;

/**
 * Removes a player from their lobby session when they disconnect.
 */
public class PlayerQuitListener implements Listener {

    private final LobbyManager lobbyManager;

    public PlayerQuitListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lobbyManager.removePlayerFromSession(event.getPlayer());
    }
}
