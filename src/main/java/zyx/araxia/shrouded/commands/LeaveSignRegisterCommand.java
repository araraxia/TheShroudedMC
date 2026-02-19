package zyx.araxia.shrouded.commands;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import zyx.araxia.shrouded.lobby.LobbyManager;

/**
 * Registers the sign the admin is looking at as a leave sign for a lobby.
 * Players who right-click this sign will be removed from the lobby.
 *
 * Usage: /shrouded.register.leavesign <lobby_name>
 */
public class LeaveSignRegisterCommand implements CommandExecutor {

    private final LobbyManager lobbyManager;

    public LeaveSignRegisterCommand(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("Usage: /shrouded.register.leavesign <lobby_name>");
            return true;
        }

        String lobbyName = args[0];

        if (!lobbyManager.lobbyExists(lobbyName)) {
            sender.sendMessage("Lobby '" + lobbyName + "' does not exist. Register it first with /shrouded.register.lobby.");
            return true;
        }

        Block target = player.getTargetBlockExact(5);

        if (target == null || !(target.getState() instanceof Sign)) {
            sender.sendMessage("You must be looking directly at a sign (within 5 blocks).");
            return true;
        }

        String world = target.getWorld().getName();
        int x = target.getX();
        int y = target.getY();
        int z = target.getZ();

        if (lobbyManager.registerLeaveSign(lobbyName, world, x, y, z)) {
            sender.sendMessage("Sign at (" + x + ", " + y + ", " + z + ") registered as a leave sign for lobby '" + lobbyName + "'.");
        } else {
            sender.sendMessage("Failed to register leave sign. Check the console for errors.");
        }

        return true;
    }
}
