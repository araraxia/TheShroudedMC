package zyx.araxia.shrouded.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import zyx.araxia.shrouded.lobby.ArenaManager;
import zyx.araxia.shrouded.lobby.LobbyManager;

/**
 * Handles linking and unlinking arenas from a lobby's valid arenas list.
 *
 * Usage: /shrouded.lobby.arena add <lobby_name> <arena_name>
 *   /shrouded.lobby.arena remove <lobby_name> <arena_name>
 */
public class ArenaLobbyCommand implements CommandExecutor {

    private final LobbyManager lobbyManager;
    private final ArenaManager arenaManager;

    public ArenaLobbyCommand(LobbyManager lobbyManager, ArenaManager arenaManager) {
        this.lobbyManager = lobbyManager;
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender == null) {
            return false;
        }

        if (args.length != 3) {
            sender.sendMessage("Usage: /shrouded.lobby.arena <add|remove> <lobby_name> <arena_name>");
            return true;
        }

        String action = args[0].toLowerCase();
        String lobbyName = args[1];
        String arenaName = args[2];

        if (!action.equals("add") && !action.equals("remove")) {
            sender.sendMessage("Action must be 'add' or 'remove'.");
            return true;
        }

        if (!lobbyManager.lobbyExists(lobbyName)) {
            sender.sendMessage("Lobby '" + lobbyName + "' does not exist.");
            return true;
        }

        if (action.equals("add")) {
            if (!arenaManager.arenaExists(arenaName)) {
                sender.sendMessage("Arena '" + arenaName + "' does not exist. Register it first with /shrouded.register.arena.");
                return true;
            }
            if (lobbyManager.registerArenaToLobby(lobbyName, arenaName)) {
                sender.sendMessage("Arena '" + arenaName + "' added to lobby '" + lobbyName + "'.");
            } else {
                sender.sendMessage("Arena '" + arenaName + "' is already linked to lobby '" + lobbyName + "'.");
            }
        } else {
            if (lobbyManager.removeArenaFromLobby(lobbyName, arenaName)) {
                sender.sendMessage("Arena '" + arenaName + "' removed from lobby '" + lobbyName + "'.");
            } else {
                sender.sendMessage("Arena '" + arenaName + "' is not linked to lobby '" + lobbyName + "'.");
            }
        }

        return true;
    }
}
