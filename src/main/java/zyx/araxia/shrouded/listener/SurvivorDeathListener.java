package zyx.araxia.shrouded.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import zyx.araxia.shrouded.lobby.LobbyManager;
import zyx.araxia.shrouded.lobby.LobbySession;

/**
 * Ends the current match immediately when every {@link
 * zyx.araxia.shrouded.game.PlayerClass#SURVIVOR Survivor} in the session has
 * been killed, rather than waiting for the round timer to expire.
 *
 * <p>Also suppresses item and XP drops for any player who dies during an
 * active match — kit items are session-managed and must not litter the arena.
 */
public class SurvivorDeathListener implements Listener {

    private final LobbyManager lobbyManager;

    public SurvivorDeathListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        LobbySession session = lobbyManager
                .getSessionForPlayer(event.getEntity().getUniqueId());
        if (session == null)
            return;

        // Suppress drops so session-managed kit items don’t scatter around the arena.
        if (session.isMatchActive()) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        session.onPlayerDied(event.getEntity().getUniqueId());
    }
}
