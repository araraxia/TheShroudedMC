package zyx.araxia.shrouded.listener;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import zyx.araxia.shrouded.lobby.LobbyManager;
import zyx.araxia.shrouded.lobby.LobbySession;

/**
 * Redirects players who died during an active match back to their lobby spawn
 * point, rather than letting them respawn at the world's default location.
 * <h3>Flow</h3>
 * <ol>
 * <li>{@link SurvivorDeathListener} handles
 * {@link org.bukkit.event.entity.PlayerDeathEvent}, suppresses item/XP drops,
 * and calls {@link LobbySession#onPlayerDied}.</li>
 * <li>{@code onPlayerDied} adds the dead player's UUID to the session's
 * {@code pendingLobbyRespawn} set.</li>
 * <li>When the player clicks Respawn, this listener fires, consumes the pending
 * flag, sets the respawn location to the lobby spawn, and applies the standard
 * lobby state (cleared effects, class selector, stopped equipment spoof).</li>
 * </ol>
 */
public class PlayerRespawnListener implements Listener {

	private final LobbyManager lobbyManager;

	public PlayerRespawnListener(LobbyManager lobbyManager) {
		this.lobbyManager = lobbyManager;
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		LobbySession session = lobbyManager
				.getSessionForPlayer(event.getPlayer().getUniqueId());
		if (session == null)
			return;

		if (!session.consumePendingRespawn(event.getPlayer().getUniqueId()))
			return;

		// Apply lobby state first (inventory, effects, spoof) so items are
		// correct when the player materialises at the lobby spawn.
		session.applyLobbyStateOnRespawn(event.getPlayer());

		Location lobbySpawn = session.getLobbySpawnLocation();
		if (lobbySpawn != null) {
			event.setRespawnLocation(lobbySpawn);
		}
	}
}
