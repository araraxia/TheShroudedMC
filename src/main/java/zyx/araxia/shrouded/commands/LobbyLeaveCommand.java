package zyx.araxia.shrouded.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import zyx.araxia.shrouded.lobby.LobbyManager;

/**
 * Removes the executing player from whatever lobby they are currently in.
 *
 * Usage: /shrouded.lobby.leave
 */
public class LobbyLeaveCommand implements CommandExecutor {

    private final LobbyManager lobbyManager;

    public LobbyLeaveCommand(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (lobbyManager.removePlayerFromSession(player)) {
            player.sendMessage(Component.text("You left the lobby.", NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("You are not in a lobby.", NamedTextColor.RED));
        }

        return true;
    }
}
