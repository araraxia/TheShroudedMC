package zyx.araxia.shrouded.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import zyx.araxia.shrouded.lobby.LobbyManager;

/**
 * /shrouded.lobby.spawn <lobby_name> [x y z [yaw pitch]]
 *
 * <p>
 * Registers a spawn point for the named lobby. If no coordinates are provided
 * the player's current location (and facing direction) are used. If only x/y/z
 * are provided, the player's current yaw and pitch are used. Fails if the
 * resolved coordinates are not within the lobby's bounding box.
 */
public class LobbySpawnCommand implements CommandExecutor {

    private final LobbyManager lobbyManager;

    public LobbySpawnCommand(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender == null) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        // Valid arg counts: 1 (name only), 4 (name + xyz), 6 (name + xyz + yaw/pitch)
        if (args.length != 1 && args.length != 4 && args.length != 6) {
            sender.sendMessage("Usage: /shrouded.lobby.spawn <lobby_name> [x y z [yaw pitch]]");
            return true;
        }

        String lobbyName = args[0];

        if (!lobbyManager.lobbyExists(lobbyName)) {
            sender.sendMessage("§cLobby '" + lobbyName + "' does not exist.");
            return true;
        }

        double x, y, z;
        float yaw, pitch;

        if (args.length == 1) {
            // Use player's current location
            Location loc = player.getLocation();

            if (loc == null) {
                sender.sendMessage("§cCould not determine your current location.");
                return true;
            }

            x = loc.getX();
            y = loc.getY();
            z = loc.getZ();
            yaw = loc.getYaw();
            pitch = loc.getPitch();
        } else {
            // Parse x y z
            try {
                x = Double.parseDouble(args[1]);
                y = Double.parseDouble(args[2]);
                z = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cCoordinates must be numbers (e.g. 12.5 64 -30).");
                return true;
            }

            if (args.length == 6) {
                // Parse yaw and pitch
                try {
                    yaw = Float.parseFloat(args[4]);
                    pitch = Float.parseFloat(args[5]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cYaw and pitch must be numbers (e.g. -90.0 0.0).");
                    return true;
                }
            } else {
                // Yaw/pitch from player
                Location loc = player.getLocation();
                if (loc == null) {
                    sender.sendMessage("§cCould not determine your current location.");
                    return true;
                }
                yaw = loc.getYaw();
                pitch = loc.getPitch();
            }
        }

        if (!lobbyManager.setLobbySpawn(lobbyName, x, y, z, yaw, pitch)) {
            sender.sendMessage("§cThat location is outside the bounds of lobby '" + lobbyName + "'.");
            return true;
        }

        sender.sendMessage("§aSpawn point for lobby '" + lobbyName + "' set to ("
                + String.format("%.2f, %.2f, %.2f", x, y, z)
                + ") facing yaw=" + String.format("%.1f", yaw)
                + " pitch=" + String.format("%.1f", pitch) + ".");
        return true;
    }
}
