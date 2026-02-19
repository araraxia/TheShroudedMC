package zyx.araxia.shrouded.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import zyx.araxia.shrouded.lobby.LobbyManager;

/**
 * Sets the countdown timer (in seconds) for a registered lobby.
 *
 * Usage: /shrouded.lobby.countdown <lobby_name> <seconds>
 */
public class LobbyCountdownCommand implements CommandExecutor {

    private final LobbyManager lobbyManager;

    public LobbyCountdownCommand(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("Usage: /shrouded.lobby.countdown <lobby_name> <seconds>");
            return true;
        }

        String lobbyName = args[0];
        int seconds;
        try {
            seconds = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("'" + args[1] + "' is not a valid number.");
            return true;
        }

        if (seconds <= 0) {
            sender.sendMessage("Countdown must be greater than 0.");
            return true;
        }

        if (!lobbyManager.lobbyExists(lobbyName)) {
            sender.sendMessage("Lobby '" + lobbyName + "' does not exist.");
            return true;
        }

        if (lobbyManager.setCountdown(lobbyName, seconds)) {
            sender.sendMessage("Countdown for lobby '" + lobbyName + "' set to " + seconds + " second(s).");
        } else {
            sender.sendMessage("Failed to save countdown for lobby '" + lobbyName + "'.");
        }

        return true;
    }
}
