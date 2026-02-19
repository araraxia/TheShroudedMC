package zyx.araxia.shrouded.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import zyx.araxia.shrouded.lobby.LobbyManager;

/**
 * Cancels any pending countdown and immediately starts the session for a lobby.
 *
 * Usage: /shrouded.lobby.forcestart <lobby_name>
 */
public class LobbyForceStartCommand implements CommandExecutor {

    private final LobbyManager lobbyManager;

    public LobbyForceStartCommand(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("Usage: /shrouded.lobby.forcestart <lobby_name>");
            return true;
        }

        String lobbyName = args[0];

        if (!lobbyManager.lobbyExists(lobbyName)) {
            sender.sendMessage("Lobby '" + lobbyName + "' does not exist.");
            return true;
        }

        if (lobbyManager.forceStartSession(lobbyName)) {
            sender.sendMessage("Force-started lobby '" + lobbyName + "'.");
        } else {
            sender.sendMessage("Could not force-start lobby '" + lobbyName + "' — no active session found.");
        }

        return true;
    }
}
