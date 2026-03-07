package zyx.araxia.shrouded.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import zyx.araxia.shrouded.lobby.LobbyManager;

/**
 * Restores orphaned player snapshots on login.
 *
 * <p>
 * When the server shuts down or the plugin is reloaded mid-session, snapshot
 * files written by {@link LobbyManager#addPlayerToSession} may be left on disk
 * without a corresponding live session to clean them up. The next time one of
 * those players joins the server, this listener detects the leftover file and
 * calls {@link LobbyManager#tryRestoreOrphanedSnapshot(org.bukkit.entity.Player)}
 * to teleport them back to their pre-lobby location and restore their
 * inventory, XP, and potion effects.
 *
 * <p>
 * The restore is deferred by one tick so the player is fully present in the
 * world (chunk loaded, inventory accessible) before any teleport or inventory
 * writes occur.
 */
public class PlayerJoinListener implements Listener {

    private final LobbyManager lobbyManager;
    private final JavaPlugin plugin;

    public PlayerJoinListener(LobbyManager lobbyManager, JavaPlugin plugin) {
        this.lobbyManager = lobbyManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Defer by one tick to ensure the player is fully loaded into the world
        // before any teleport or inventory manipulation is attempted.
        new BukkitRunnable() {
            @Override
            public void run() {
                if (event.getPlayer().isOnline()) {
                    lobbyManager.tryRestoreOrphanedSnapshot(event.getPlayer());
                }
            }
        }.runTaskLater(plugin, 1L);
    }
}
