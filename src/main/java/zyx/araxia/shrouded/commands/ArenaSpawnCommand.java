package zyx.araxia.shrouded.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import zyx.araxia.shrouded.lobby.ArenaManager;

/**
 * Adds a spawn point to a registered arena for either the regular player group
 * or the Shrouded role.
 *
 * <p>Usage: {@code /shrouded.arena.spawn <arena_name> <player|shrouded>}
 *
 * <p>The spawn is recorded at the executing player's current position and
 * look-direction. Multiple spawns may be registered for each role; they are
 * assigned round-robin when a match begins.
 */
public class ArenaSpawnCommand implements CommandExecutor {

    private final ArenaManager arenaManager;

    public ArenaSpawnCommand(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage("Usage: /shrouded.arena.spawn <arena_name> <player|shrouded>");
            return true;
        }

        String arenaName = args[0];
        String roleArg = args[1].toLowerCase();

        if (!roleArg.equals("player") && !roleArg.equals("shrouded")) {
            sender.sendMessage("Role must be 'player' or 'shrouded'.");
            return true;
        }

        double x = player.getLocation().getX();
        double y = player.getLocation().getY();
        double z = player.getLocation().getZ();
        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();

        boolean saved;
        if (roleArg.equals("player")) {
            saved = arenaManager.addPlayerSpawn(arenaName, x, y, z, yaw, pitch);
        } else {
            saved = arenaManager.addShroudedSpawn(arenaName, x, y, z, yaw, pitch);
        }

        if (!saved) {
            sender.sendMessage("Arena '" + arenaName + "' not found or could not be saved.");
            return true;
        }

        sender.sendMessage("Added " + roleArg + " spawn to arena '" + arenaName + "' at ("
                + Math.round(x * 10) / 10.0 + ", "
                + Math.round(y * 10) / 10.0 + ", "
                + Math.round(z * 10) / 10.0 + ").");
        return true;
    }
}
