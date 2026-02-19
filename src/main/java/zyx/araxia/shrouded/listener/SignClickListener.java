package zyx.araxia.shrouded.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import zyx.araxia.shrouded.lobby.LobbyManager;
import zyx.araxia.shrouded.lobby.LobbyManager.JoinSessionResult;
import zyx.araxia.shrouded.lobby.LobbySession;

public class SignClickListener implements Listener {

    private final LobbyManager lobbyManager;

    public SignClickListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign))
            return;

        Player player = event.getPlayer();
        String world = block.getWorld().getName();
        int x = block.getX(), y = block.getY(), z = block.getZ();

        // Check leave signs first
        LobbySession leaveSession = lobbyManager.getSessionByLeaveSign(world, x, y, z);
        if (leaveSession != null) {
            event.setCancelled(true);
            if (!leaveSession.contains(player.getUniqueId())) {
                player.sendMessage(Component.text(
                        "You are not in lobby '" + leaveSession.getLobby().getName() + "'.",
                        NamedTextColor.YELLOW));
                return;
            }
            lobbyManager.removePlayerFromSession(player);
            player.sendMessage(Component.text(
                    "You left lobby '" + leaveSession.getLobby().getName() + "'.",
                    NamedTextColor.YELLOW));
            return;
        }

        // Check join signs
        LobbySession session = lobbyManager.getSessionBySign(world, x, y, z);
        if (session == null)
            return; // Not a registered lobby sign

        event.setCancelled(true);

        JoinSessionResult result = lobbyManager.addPlayerToSession(player, session);
        switch (result) {
            case SUCCESS -> player.sendMessage(Component.text(
                    "You joined lobby '" + session.getLobby().getName() + "' ("
                            + session.getPlayerCount() + "/" + session.getLobby().getMaxPlayers() + ").",
                    NamedTextColor.GREEN));
            case ALREADY_IN_LOBBY -> player.sendMessage(Component.text(
                    "You are already in lobby '" + session.getLobby().getName() + "'.",
                    NamedTextColor.YELLOW));
            case LOBBY_FULL -> player.sendMessage(Component.text(
                    "Lobby '" + session.getLobby().getName() + "' is full ("
                            + session.getLobby().getMaxPlayers() + "/" + session.getLobby().getMaxPlayers() + ").",
                    NamedTextColor.RED));
            case WORLD_NOT_FOUND -> player.sendMessage(Component.text(
                    "Could not join lobby '" + session.getLobby().getName()
                            + "' — its world is not loaded.",
                    NamedTextColor.RED));
        }
    }
}
