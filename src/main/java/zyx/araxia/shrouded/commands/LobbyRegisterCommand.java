package zyx.araxia.shrouded.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import zyx.araxia.shrouded.lobby.LobbyManager;

public class LobbyRegisterCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final LobbyManager lobbyManager;

    public LobbyRegisterCommand(JavaPlugin plugin, LobbyManager lobbyManager) {
        this.plugin = plugin;
        this.lobbyManager = lobbyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length < 7 || args.length > 9) {
            sender.sendMessage("Usage: /shrouded.register.lobby <lobby_name> <x1> <y1> <z1> <x2> <y2> <z2> [world_name] [max_players]");
            return true;
        }

        String lobbyName = args[0];
        int x1, y1, z1, x2, y2, z2;

        try {
            x1 = Integer.parseInt(args[1]);
            y1 = Integer.parseInt(args[2]);
            z1 = Integer.parseInt(args[3]);
            x2 = Integer.parseInt(args[4]);
            y2 = Integer.parseInt(args[5]);
            z2 = Integer.parseInt(args[6]);
        } catch (NumberFormatException e) {
            sender.sendMessage("All coordinates must be whole numbers.");
            return true;
        }

        // args[7] = optional world name, args[8] = optional max players
        String world = (args.length >= 8) ? args[7] : player.getWorld().getName();

        if (plugin.getServer().getWorld(world) == null) {
            sender.sendMessage("World '" + world + "' does not exist or is not loaded.");
            return true;
        }

        int maxPlayers = 8; // default
        if (args.length == 9) {
            try {
                maxPlayers = Integer.parseInt(args[8]);
                if (maxPlayers < 1) {
                    sender.sendMessage("Max players must be at least 1.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("Max players must be a whole number.");
                return true;
            }
        }

        if (lobbyManager.registerLobby(lobbyName, world, x1, y1, z1, x2, y2, z2, maxPlayers)) {
            sender.sendMessage("Lobby '" + lobbyName + "' registered in world '" + world + "'.");
            sender.sendMessage("Region: (" + x1 + ", " + y1 + ", " + z1 + ") to (" + x2 + ", " + y2 + ", " + z2 + ")" +
                    " | Max players: " + maxPlayers);
        } else {
            sender.sendMessage("Failed to save lobby '" + lobbyName + "'. Check the console for errors.");
        }

        return true;
    }
}
